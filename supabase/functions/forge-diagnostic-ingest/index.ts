const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

const MAX_BODY = 64 * 1024;
const RATE_LIMIT_SCRIPT =
  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],86400) end; return n";

function cleanText(value: unknown, max: number): string {
  if (typeof value !== "string") return "";
  return value.replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0, max);
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
  const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
  if (!supabaseUrl || !serviceRoleKey || !redisUrl || !redisToken) {
    return json({ ok: false, error: "server_not_configured" }, 503);
  }

  const apiKey = req.headers.get("apikey");
  const acceptedKeys = [
    Deno.env.get("SUPABASE_ANON_KEY"),
    Deno.env.get("SUPABASE_PUBLISHABLE_KEY"),
  ].filter((value): value is string => Boolean(value));
  if (!apiKey || !acceptedKeys.includes(apiKey)) {
    return json({ ok: false, error: "invalid_api_key" }, 401);
  }

  const raw = await req.text();
  if (raw.length > MAX_BODY) return json({ ok: false, error: "request_too_large" }, 413);
  let input: Record<string, unknown>;
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("invalid");
    input = parsed as Record<string, unknown>;
  } catch {
    return json({ ok: false, error: "invalid_json" }, 400);
  }

  const events = input.events;
  if (!Array.isArray(events) || events.length < 1 || events.length > 40) {
    return json({ ok: false, error: "invalid_event_batch" }, 400);
  }

  const normalized: Record<string, unknown>[] = [];
  let installationId = "";
  for (const rawEvent of events) {
    if (!rawEvent || typeof rawEvent !== "object" || Array.isArray(rawEvent)) {
      return json({ ok: false, error: "invalid_event" }, 400);
    }
    const event = rawEvent as Record<string, unknown>;
    const eventId = cleanText(event.eventId, 36);
    const currentInstallationId = cleanText(event.installationId, 36);
    const occurredAt = cleanText(event.occurredAt, 40);
    const appVersion = cleanText(event.appVersion, 32);
    const androidRelease = cleanText(event.androidRelease, 32);
    const level = cleanText(event.level, 8);
    const tag = cleanText(event.tag, 48);
    const eventName = cleanText(event.eventName, 120);
    const exceptionType = cleanText(event.exceptionType, 200);
    const stackTrace = cleanText(event.stackTrace, 12000);
    const sdk = Number(event.androidSdk);

    if (!/^[0-9a-fA-F-]{36}$/.test(eventId) ||
        !/^[0-9a-fA-F-]{36}$/.test(currentInstallationId) ||
        !Number.isFinite(Date.parse(occurredAt)) ||
        !appVersion || !androidRelease ||
        !["DEBUG", "INFO", "WARNING", "ERROR"].includes(level) ||
        !tag || !eventName || !Number.isInteger(sdk) || sdk < 1 || sdk > 100) {
      return json({ ok: false, error: "invalid_event_payload" }, 400);
    }
    if (installationId && installationId !== currentInstallationId) {
      return json({ ok: false, error: "mixed_installations" }, 400);
    }
    installationId = currentInstallationId;
    normalized.push({
      event_id: eventId,
      occurred_at: new Date(occurredAt).toISOString(),
      app_version: appVersion,
      android_sdk: sdk,
      android_release: androidRelease,
      level,
      tag,
      event_name: eventName,
      exception_type: exceptionType,
      stack_trace: stackTrace,
    });
  }

  let redisEndpoint: URL;
  try {
    redisEndpoint = new URL(redisUrl);
  } catch {
    return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
  }
  if (redisEndpoint.protocol !== "https:" || !redisEndpoint.hostname.endsWith(".upstash.io")) {
    return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
  }

  const installHash = await sha256(installationId.toLowerCase());
  const rateKey = `forge-ts:diagnostic-ingest:${installHash}:${new Date().toISOString().slice(0, 10)}`;
  try {
    const rateResponse = await fetch(redisEndpoint, {
      method: "POST",
      headers: { Authorization: `Bearer ${redisToken}`, "Content-Type": "application/json" },
      body: JSON.stringify(["EVAL", RATE_LIMIT_SCRIPT, "1", rateKey]),
      signal: AbortSignal.timeout(4000),
    });
    if (!rateResponse.ok) return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    const ratePayload = await rateResponse.json();
    const count = Number(ratePayload?.result);
    if (!Number.isFinite(count)) return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    if (count > 300) return json({ ok: false, error: "daily_install_limit_reached" }, 429);
  } catch {
    return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
  }

  const rows = normalized.map((event) => ({ ...event, install_hash: installHash }));
  const insertUrl = new URL(`${supabaseUrl}/rest/v1/forge_ts_diagnostic_events`);
  insertUrl.searchParams.set("on_conflict", "event_id");
  const insertResponse = await fetch(insertUrl, {
    method: "POST",
    headers: {
      apikey: serviceRoleKey,
      Authorization: `Bearer ${serviceRoleKey}`,
      "Content-Type": "application/json",
      Prefer: "resolution=ignore-duplicates,return=minimal",
    },
    body: JSON.stringify(rows),
    signal: AbortSignal.timeout(7000),
  });
  if (!insertResponse.ok) return json({ ok: false, error: "diagnostic_storage_unavailable" }, 502);
  return json({ ok: true, accepted: rows.length });
});
