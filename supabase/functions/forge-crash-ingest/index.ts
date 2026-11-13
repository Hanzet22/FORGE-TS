const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

const MAX_BODY = 48 * 1024;
const RATE_LIMIT_SCRIPT =
  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],86400) end; return n";

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function cleanText(value: unknown, max: number, fallback = ""): string {
  if (typeof value !== "string") return fallback;
  return value.replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0, max);
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const publishableKey = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
  const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
  if (!supabaseUrl || !publishableKey || !serviceRoleKey || !redisUrl || !redisToken) {
    return json({ ok: false, error: "server_not_configured" }, 503);
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

  // The publishable key is public and is not caller authentication. This check
  // enforces the expected Supabase client request contract only.
  if (req.headers.get("apikey") !== publishableKey) {
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

  const installationId = cleanText(input.installationId, 64);
  const crashId = cleanText(input.crashId, 36);
  const occurredAt = cleanText(input.occurredAt, 40);
  const appVersion = cleanText(input.appVersion, 32);
  const androidRelease = cleanText(input.androidRelease, 32);
  const exceptionType = cleanText(input.exceptionType, 200);
  const stackTrace = cleanText(input.stackTrace, 32000);

  if (!/^[0-9a-fA-F-]{36}$/.test(installationId) ||
      !/^[0-9a-fA-F-]{36}$/.test(crashId) ||
      !Number.isFinite(Date.parse(occurredAt)) ||
      !appVersion || !androidRelease || !exceptionType || !stackTrace) {
    return json({ ok: false, error: "invalid_crash_payload" }, 400);
  }
  const sdk = Number(input.androidSdk);
  if (!Number.isInteger(sdk) || sdk < 1 || sdk > 100) {
    return json({ ok: false, error: "invalid_android_sdk" }, 400);
  }

  const installHash = await sha256(installationId.toLowerCase());

  // WorkManager may retry after the server stored a report but before the
  // response reached the device. Detect that duplicate before consuming quota.
  if (input.linkOnly !== true) {
    const existingUrl = new URL(`${supabaseUrl}/rest/v1/forge_ts_crash_events`);
    existingUrl.searchParams.set("select", "crash_id");
    existingUrl.searchParams.set("crash_id", `eq.${crashId}`);
    existingUrl.searchParams.set("install_hash", `eq.${installHash}`);
    try {
      const existingResponse = await fetch(existingUrl, {
        headers: {
          apikey: serviceRoleKey,
          Authorization: `Bearer ${serviceRoleKey}`,
        },
        signal: AbortSignal.timeout(5000),
      });
      if (!existingResponse.ok) {
        return json({ ok: false, error: "crash_storage_unavailable" }, 502);
      }
      const existingRows = await existingResponse.json();
      if (Array.isArray(existingRows) && existingRows.length > 0) {
        return json({ ok: true, duplicate: true, linkedToAccount: false });
      }
    } catch {
      return json({ ok: false, error: "crash_storage_unavailable" }, 502);
    }
  }

  const installHash = await sha256(installationId.toLowerCase());
  const rateKey = `forge-ts:crash-ingest:${installHash}:${new Date().toISOString().slice(0, 10)}`;
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
    if (count > 10) return json({ ok: false, error: "daily_install_limit_reached" }, 429);
  } catch {
    return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
  }

  let userId: string | null = null;
  const authorization = req.headers.get("authorization");
  if (authorization) {
    if (!/^Bearer\s+\S+$/i.test(authorization)) {
      return json({ ok: false, error: "invalid_authorization" }, 401);
    }
    try {
      const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
        headers: { apikey: publishableKey, Authorization: authorization },
        signal: AbortSignal.timeout(5000),
      });
      if (!userResponse.ok) return json({ ok: false, error: "invalid_user_session" }, 401);
      const user = await userResponse.json();
      if (typeof user?.id !== "string" || !/^[0-9a-fA-F-]{36}$/.test(user.id)) {
        return json({ ok: false, error: "invalid_user_session" }, 401);
      }
      userId = user.id;
    } catch {
      return json({ ok: false, error: "auth_verification_unavailable" }, 503);
    }
  }

  if (input.linkOnly === true) {
    if (!userId) return json({ ok: false, error: "authenticated_user_required_for_link" }, 401);
    const linkUrl = new URL(`${supabaseUrl}/rest/v1/forge_ts_crash_events`);
    linkUrl.searchParams.set("crash_id", `eq.${crashId}`);
    linkUrl.searchParams.set("install_hash", `eq.${installHash}`);
    linkUrl.searchParams.set("user_id", "is.null");
    const linkResponse = await fetch(linkUrl, {
      method: "PATCH",
      headers: {
        apikey: serviceRoleKey,
        Authorization: `Bearer ${serviceRoleKey}`,
        "Content-Type": "application/json",
        Prefer: "return=minimal",
      },
      body: JSON.stringify({ user_id: userId }),
      signal: AbortSignal.timeout(7000),
    });
    if (!linkResponse.ok) return json({ ok: false, error: "crash_link_unavailable" }, 502);
    return json({ ok: true, linkedToAccount: true });
  }

  const insertUrl = new URL(`${supabaseUrl}/rest/v1/forge_ts_crash_events`);
  insertUrl.searchParams.set("on_conflict", "crash_id");
  const insertResponse = await fetch(insertUrl, {
    method: "POST",
    headers: {
      apikey: serviceRoleKey,
      Authorization: `Bearer ${serviceRoleKey}`,
      "Content-Type": "application/json",
      Prefer: "resolution=ignore-duplicates,return=minimal",
    },
    body: JSON.stringify({
      crash_id: crashId,
      user_id: userId,
      install_hash: installHash,
      occurred_at: new Date(occurredAt).toISOString(),
      app_version: appVersion,
      android_sdk: sdk,
      android_release: androidRelease,
      exception_type: exceptionType,
      stack_trace: stackTrace,
    }),
    signal: AbortSignal.timeout(7000),
  });
  if (!insertResponse.ok) {
    return json({ ok: false, error: "crash_storage_unavailable" }, 502);
  }
  return json({ ok: true, linkedToAccount: userId !== null });
});
