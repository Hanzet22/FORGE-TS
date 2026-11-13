import { withSupabase } from "npm:@supabase/server@1.9.2";

type Operation = "set" | "get" | "del" | "ttl" | "ping";

const json = (body: Record<string, unknown>, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });

function validKey(value: unknown): value is string {
  return typeof value === "string" &&
    value.length > 0 &&
    value.length <= 128 &&
    /^[A-Za-z0-9:_-]+$/.test(value);
}

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    if (req.method !== "POST") {
      return json({ ok: false, error: "method_not_allowed" }, 405);
    }

    const userId = ctx.userClaims?.id;
    if (!userId) {
      return json({ ok: false, error: "authenticated_user_required" }, 401);
    }

    // Optional MFA: users with a verified factor must present an AAL2 JWT.
    // Accounts without a verified factor retain the normal AAL1 password flow.
    if (ctx.userClaims?.aal !== "aal2") {
      const supabaseUrl = Deno.env.get("SUPABASE_URL");
      const publishableKey = Deno.env.get("SUPABASE_ANON_KEY") ??
        Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
      const authorization = req.headers.get("authorization");
      if (!supabaseUrl || !publishableKey || !authorization) {
        return json({ ok: false, error: "mfa_status_unavailable" }, 503);
      }
      try {
        const factorResponse = await fetch(
          `${supabaseUrl.replace(/\\/$/, "")}/auth/v1/factors`,
          {
            headers: {
              "apikey": publishableKey,
              "Authorization": authorization,
            },
            signal: AbortSignal.timeout(4000),
          },
        );
        if (!factorResponse.ok) {
          return json({ ok: false, error: "mfa_status_unavailable" }, 503);
        }
        const factorData = await factorResponse.json();
        const factors = Array.isArray(factorData?.all)
          ? factorData.all
          : [
            ...(Array.isArray(factorData?.totp) ? factorData.totp : []),
            ...(Array.isArray(factorData?.phone) ? factorData.phone : []),
          ];
        if (factors.some((factor: { status?: string }) => factor?.status === "verified")) {
          return json({ ok: false, error: "mfa_required" }, 403);
        }
      } catch {
        return json({ ok: false, error: "mfa_status_unavailable" }, 503);
      }
    }

    const redisUrl = Deno.env.get("UPSTASH_REDIS_REST_URL");
    const redisToken = Deno.env.get("UPSTASH_REDIS_REST_TOKEN");
    if (!redisUrl || !redisToken) {
      return json({
        ok: false,
        error: "redis_not_configured",
        mode: "offline_fallback",
      }, 503);
    }

    let endpoint: URL;
    try {
      endpoint = new URL(redisUrl);
    } catch {
      return json({ ok: false, error: "redis_configuration_invalid" }, 503);
    }
    if (
      endpoint.protocol !== "https:" ||
      !endpoint.hostname.endsWith(".upstash.io") ||
      endpoint.username ||
      endpoint.password ||
      endpoint.hash
    ) {
      return json({ ok: false, error: "redis_configuration_invalid" }, 503);
    }

    let input: Record<string, unknown>;
    try {
      const raw = await req.text();
      if (raw.length > 8192) {
        return json({ ok: false, error: "request_too_large" }, 413);
      }
      input = JSON.parse(raw);
    } catch {
      return json({ ok: false, error: "invalid_json" }, 400);
    }

    const operation = input.operation as Operation;
    if (!["set", "get", "del", "ttl", "ping"].includes(operation)) {
      return json({ ok: false, error: "unsupported_operation" }, 400);
    }

    let command: string[];
    if (operation === "ping") {
      command = ["PING"];
    } else {
      if (!validKey(input.key)) {
        return json({ ok: false, error: "invalid_key" }, 400);
      }
      // Every account gets a separate keyspace in the FORGE-TS Redis database.
      const key = `forge-ts:${userId}:${input.key}`;

      switch (operation) {
        case "set": {
          if (typeof input.value !== "string" || input.value.length > 4096) {
            return json({ ok: false, error: "invalid_value" }, 400);
          }
          const ttl = input.ttlSeconds;
          if (ttl !== undefined &&
            (!Number.isInteger(ttl) || (ttl as number) < 1 || (ttl as number) > 86400)) {
            return json({ ok: false, error: "invalid_ttl" }, 400);
          }
          command = ttl === undefined
            ? ["SET", key, input.value]
            : ["SET", key, input.value, "EX", String(ttl)];
          break;
        }
        case "get":
          command = ["GET", key];
          break;
        case "del":
          command = ["DEL", key];
          break;
        case "ttl":
          command = ["TTL", key];
          break;
      }
    }

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${redisToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(command!),
        signal: AbortSignal.timeout(5000),
      });
      if (!response.ok) {
        return json({ ok: false, error: "redis_upstream_unavailable" }, 502);
      }
      const payload = await response.json();
      if (payload?.error) {
        return json({ ok: false, error: "redis_command_failed" }, 502);
      }
      return json({
        ok: true,
        operation,
        result: payload?.result ?? null,
        source: "redis-compatible",
      });
    } catch {
      return json({ ok: false, error: "redis_upstream_unavailable" }, 502);
    }
  }),
};
