const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });

const MAX_BODY = 64 * 1024;
const RATE_LIMIT_SCRIPT =
  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],86400) end; return n";
const EMAIL_RATE_LIMIT_SCRIPT =
  "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],1800) end; return n";

function cleanText(value: unknown, max: number): string {
  if (typeof value !== "string") return "";
  return value.replace(/[\u0000-\u001f\u007f]/g, " ").trim().slice(0, max);
}

function errorName(error: unknown): string {
  return error instanceof Error ? error.name : "UnknownError";
}

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

Deno.serve(async (req: Request) => {
  let stage = "request";
  try {
    if (req.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405);

    stage = "configuration";
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
    const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
    const missingConfig = [
      ["SUPABASE_URL", supabaseUrl],
      ["SUPABASE_SERVICE_ROLE_KEY", serviceRoleKey],
      ["UPSTASH_REDIS_REST_URL", redisUrl],
      ["UPSTASH_REDIS_REST_TOKEN", redisToken],
    ].filter(([, value]) => !value).map(([name]) => name);
    if (missingConfig.length > 0) {
      console.error("diagnostic_ingest_config_missing", { missing: missingConfig });
      return json({ ok: false, error: "server_not_configured" }, 503);
    }

    stage = "api_key";
    const apiKey = req.headers.get("apikey");
    const acceptedKeys = [
      Deno.env.get("SUPABASE_ANON_KEY"),
      Deno.env.get("SUPABASE_PUBLISHABLE_KEY"),
    ].filter((value): value is string => Boolean(value));
    if (!apiKey || !acceptedKeys.includes(apiKey)) {
      return json({ ok: false, error: "invalid_api_key" }, 401);
    }

    stage = "request_body";
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

    stage = "event_validation";
    const events = input.events;
    if (!Array.isArray(events) || events.length < 1 || events.length > 200) {
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
      const sessionId = cleanText(event.sessionId, 36);
      const traceId = cleanText(event.traceId, 36);
      const occurredAt = cleanText(event.occurredAt, 40);
      const appVersion = cleanText(event.appVersion, 32);
      const androidRelease = cleanText(event.androidRelease, 32);
      const level = cleanText(event.level, 8);
      const category = cleanText(event.category, 32) || "GENERAL";
      const tag = cleanText(event.tag, 48);
      const eventName = cleanText(event.eventName, 120);
      const exceptionType = cleanText(event.exceptionType, 200);
      const stackTrace = cleanText(event.stackTrace, 12000);
      const sdk = Number(event.androidSdk);
      const deviceBrand = cleanText(event.deviceBrand, 48);
      const deviceModel = cleanText(event.deviceModel, 80);
      const deviceRamMb = Number(event.deviceRamMb);
      const displayResolution = cleanText(event.displayResolution, 32);
      const cpuChipset = cleanText(event.cpuChipset, 80);
      const cpuArchitecture = cleanText(event.cpuArchitecture, 40);

      if (!/^[0-9a-fA-F-]{36}$/.test(eventId) ||
          !/^[0-9a-fA-F-]{36}$/.test(currentInstallationId) ||
          (sessionId !== "" && !/^[0-9a-fA-F-]{36}$/.test(sessionId)) ||
          (traceId !== "" && !/^[0-9a-fA-F-]{36}$/.test(traceId)) ||
          !Number.isFinite(Date.parse(occurredAt)) ||
          !appVersion || !androidRelease ||
          !["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL", "CRASH"].includes(level) ||
          !["GENERAL", "APP_LIFECYCLE", "ACTIVITY", "NAVIGATION", "FEATURE", "NETWORK", "DNS", "STORAGE", "DATABASE", "BACKGROUND", "PERMISSION", "RUNTIME", "MEDIA", "PERFORMANCE", "SECURITY", "CRASH", "TELEMETRY"].includes(category) ||
          !deviceBrand || !deviceModel ||
          !Number.isInteger(deviceRamMb) || deviceRamMb < 128 || deviceRamMb > 1048576 ||
          !/^\d{2,5}x\d{2,5}$/.test(displayResolution) ||
          !cpuChipset || !cpuArchitecture ||
          !tag || !eventName || !Number.isInteger(sdk) || sdk < 1 || sdk > 100) {
        return json({ ok: false, error: "invalid_event_payload" }, 400);
      }
      if (installationId && installationId !== currentInstallationId) {
        return json({ ok: false, error: "mixed_installations" }, 400);
      }
      installationId = currentInstallationId;
      normalized.push({
        event_id: eventId,
        session_id: sessionId || null,
        trace_id: traceId || null,
        occurred_at: new Date(occurredAt).toISOString(),
        app_version: appVersion,
        android_sdk: sdk,
        android_release: androidRelease,
        device_brand: deviceBrand,
        device_model: deviceModel,
        device_ram_mb: deviceRamMb,
        display_resolution: displayResolution,
        cpu_chipset: cpuChipset,
        cpu_architecture: cpuArchitecture,
        level,
        category,
        tag,
        event_name: eventName,
        exception_type: exceptionType,
        stack_trace: stackTrace,
      });
    }

    stage = "redis_configuration";
    let redisEndpoint: URL;
    try {
      redisEndpoint = new URL(redisUrl!);
    } catch {
      console.error("diagnostic_ingest_redis_endpoint_invalid");
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }
    if (redisEndpoint.protocol !== "https:" || !redisEndpoint.hostname.endsWith(".upstash.io")) {
      console.error("diagnostic_ingest_redis_endpoint_rejected", {
        protocol: redisEndpoint.protocol,
        host_suffix_valid: redisEndpoint.hostname.endsWith(".upstash.io"),
      });
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }

    stage = "redis_rate_limit";
    const installHash = await sha256(installationId.toLowerCase());
    const rateKey = "forge-ts:diagnostic-ingest:" + installHash + ":" +
      new Date().toISOString().slice(0, 10);
    let rateResponse: Response;
    try {
      rateResponse = await fetch(redisEndpoint, {
        method: "POST",
        headers: { Authorization: "Bearer " + redisToken, "Content-Type": "application/json" },
        body: JSON.stringify(["EVAL", RATE_LIMIT_SCRIPT, "1", rateKey]),
        signal: AbortSignal.timeout(4000),
      });
    } catch (error) {
      console.error("diagnostic_ingest_redis_request_failed", { error_name: errorName(error) });
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }
    if (!rateResponse.ok) {
      console.error("diagnostic_ingest_redis_rejected", { status: rateResponse.status });
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }

    let ratePayload: Record<string, unknown>;
    try {
      ratePayload = await rateResponse.json();
    } catch (error) {
      console.error("diagnostic_ingest_redis_invalid_json", { error_name: errorName(error) });
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }
    if (typeof ratePayload.error === "string") {
      console.error("diagnostic_ingest_redis_command_failed");
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }
    const count = Number(ratePayload.result);
    if (!Number.isFinite(count)) {
      console.error("diagnostic_ingest_redis_result_invalid");
      return json({ ok: false, error: "rate_limiter_unavailable" }, 503);
    }
    if (count > 300) return json({ ok: false, error: "daily_install_limit_reached" }, 429);

    stage = "database_insert";
    let insertUrl: URL;
    try {
      insertUrl = new URL(supabaseUrl! + "/rest/v1/forge_ts_diagnostic_events");
    } catch {
      console.error("diagnostic_ingest_supabase_endpoint_invalid");
      return json({ ok: false, error: "diagnostic_storage_unavailable" }, 502);
    }
    insertUrl.searchParams.set("on_conflict", "event_id");

    let insertResponse: Response;
    try {
      const rows = normalized.map((event) => ({ ...event, install_hash: installHash }));
      insertResponse = await fetch(insertUrl, {
        method: "POST",
        headers: {
          apikey: serviceRoleKey!,
          Authorization: "Bearer " + serviceRoleKey,
          "Content-Type": "application/json",
          Prefer: "resolution=ignore-duplicates,return=minimal",
        },
        body: JSON.stringify(rows),
        signal: AbortSignal.timeout(7000),
      });
    } catch (error) {
      console.error("diagnostic_ingest_database_request_failed", { error_name: errorName(error) });
      return json({ ok: false, error: "diagnostic_storage_unavailable" }, 502);
    }
    if (!insertResponse.ok) {
      console.error("diagnostic_ingest_database_rejected", { status: insertResponse.status });
      return json({ ok: false, error: "diagnostic_storage_unavailable" }, 502);
    }

    // Send one privacy-filtered summary for the accepted background-session envelope.
    // Raw stack traces, installation IDs, credentials, terminal data, and message contents
    // are never included in administrator email.
    let emailAlert = "not_configured";
    const resendKey = Deno.env.get("RESEND_API_KEY");
    const emailFrom = Deno.env.get("DIAGNOSTICS_EMAIL_FROM") ||
      Deno.env.get("DIAGNOSTIC_EMAIL_FROM");
    const importantEvents = normalized;
    if (resendKey && emailFrom && importantEvents.length > 0) {
      stage = "admin_email_rate_limit";
      const emailRateKey = "forge-ts:diagnostic-email:" + installHash;
      try {
        const emailLimitResponse = await fetch(redisEndpoint, {
          method: "POST",
          headers: { Authorization: "Bearer " + redisToken, "Content-Type": "application/json" },
          body: JSON.stringify(["EVAL", EMAIL_RATE_LIMIT_SCRIPT, "1", emailRateKey]),
          signal: AbortSignal.timeout(4000),
        });
        const emailLimitPayload = emailLimitResponse.ok
          ? await emailLimitResponse.json()
          : null;
        const emailCount = Number(emailLimitPayload?.result);
        if (Number.isFinite(emailCount) && emailCount === 1) {
          stage = "admin_email_send";
          const summary = importantEvents.slice(0, 10).map((event) => ({
            occurredAt: event.occurred_at,
            appVersion: event.app_version,
            androidVersion: event.android_release,
            androidSdk: event.android_sdk,
            deviceBrand: event.device_brand,
            deviceModel: event.device_model,
            deviceRamMb: event.device_ram_mb,
            displayResolution: event.display_resolution,
            cpuChipset: event.cpu_chipset,
            cpuArchitecture: event.cpu_architecture,
            level: event.level,
            category: event.category,
            component: event.tag,
            event: event.event_name,
            exceptionType: event.exception_type,
          }));
          const mailResponse = await fetch("https://api.resend.com/emails", {
            method: "POST",
            headers: {
              Authorization: "Bearer " + resendKey,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({
              from: emailFrom,
              to: ["papakjamsutnoruu@gmail.com"],
              subject: "[FORGE-TS] Session diagnostics summary",
              text: "FORGE-TS received a background-session diagnostics report.\n\n" +
                JSON.stringify(summary, null, 2) +
                "\n\nFull event records remain in the protected FORGE-TS diagnostics backend.",
            }),
            signal: AbortSignal.timeout(7000),
          });
          if (mailResponse.ok) emailAlert = "sent";
          else {
            console.error("diagnostic_admin_email_rejected", { status: mailResponse.status });
            emailAlert = "provider_rejected";
          }
        } else if (Number.isFinite(emailCount) && emailCount > 1) {
          emailAlert = "throttled";
        } else {
          console.error("diagnostic_admin_email_rate_limit_unavailable");
          emailAlert = "rate_limit_unavailable";
        }
      } catch (error) {
        console.error("diagnostic_admin_email_failed", { error_name: errorName(error) });
        emailAlert = "delivery_failed";
      }
    }

    return json({ ok: true, accepted: normalized.length, emailAlert });
  } catch (error) {
    console.error("diagnostic_ingest_unhandled_error", {
      stage,
      error_name: errorName(error),
    });
    return json({ ok: false, error: "diagnostic_ingest_internal_error" }, 500);
  }
});
