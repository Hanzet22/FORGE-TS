# FORGE-TS Supabase

Project: `FORGE-TS-LAB`  
Region: `ap-southeast-1`

## Edge Function secrets

The `forge-redis-gateway` function uses the existing shared Upstash Redis database. Configure these two secrets in **Supabase Dashboard → Edge Functions → Secrets**:

- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`

Do not put either value in the Android app, Git repository, build config, or issue tracker. Do not use the Redis password in place of the REST token.

The function is intentionally unavailable (HTTP 503 with `redis_not_configured`) until both secrets are set. The Android client must then retain its local simulator as a fallback.

## Security model

- The function requires a valid Supabase user JWT (`verify_jwt = true`).
- Redis keys are automatically namespaced as `forge-ts:<auth-user-id>:<client-key>`.
- Only SET, GET, DEL, TTL, and PING are allowed; clients cannot submit arbitrary Redis commands.
- Each user's keyspace is isolated by the authenticated subject.
- The database table `public.forge_ts_lab_records` has RLS enabled and grants no anonymous access.
- Never disable RLS or expose a Supabase secret/service-role key in the Android APK.

## Local simulation

The app's in-memory Redis and Supabase simulators remain available when connectivity or a remote service is unavailable. Simulated writes must never be replayed automatically against live services.
