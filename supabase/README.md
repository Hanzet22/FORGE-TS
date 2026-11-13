# FORGE-TS Supabase

Project: `FORGE-TS-LAB`  
Region: `ap-southeast-1`

## Edge Function secrets

The `forge-redis-gateway` function uses the existing shared Upstash Redis database. Configure these two secrets in **Supabase Dashboard → Edge Functions → Secrets**:

- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`
- `RESEND_API_KEY` (optional; only required for email alerts)
- `DIAGNOSTICS_EMAIL_FROM` (optional; must be a verified sender when email alerts are enabled)
- `DIAGNOSTICS_EMAIL_TO` (optional; comma-separated recipient addresses; no recipient is hard-coded in source)

Do not put either value in the Android app, Git repository, build config, or issue tracker. Do not use the Redis password in place of the REST token.

The Redis-backed functions fail closed when their required Upstash secrets are missing. Diagnostic email alerts remain disabled unless the Resend key, verified sender, and recipient secret are configured. The Android client must retain its local simulator as a fallback.

## Security model

- The function requires a valid Supabase user JWT (`verify_jwt = true`).
- Redis keys are automatically namespaced as `forge-ts:<auth-user-id>:<client-key>`.
- Only SET, GET, DEL, TTL, and PING are allowed; clients cannot submit arbitrary Redis commands.
- Each user's keyspace is isolated by the authenticated subject.
- The database table `public.forge_ts_lab_records` has RLS enabled and grants no anonymous access.
- Never disable RLS or expose a Supabase secret/service-role key in the Android APK.

## Local simulation

The app's in-memory Redis and Supabase simulators remain available when connectivity or a remote service is unavailable. Simulated writes must never be replayed automatically against live services.


## Ingest abuse controls

- Crash ingestion has a Redis-backed daily ceiling of 500 unique reports globally and 10 per installation.
- Diagnostic ingestion has a Redis-backed daily ceiling of 3,000 unique event IDs globally and 300 per installation.
- Diagnostic event IDs are deduplicated in Redis before quota accounting so WorkManager retries of the same batch do not consume the daily event quota again.
- These are downstream workload and storage caps, not a complete network-layer DDoS defense. The publishable Supabase key is public and is not caller authentication.
- Email alerts are limited globally to one message per 30 minutes and only include ERROR, CRITICAL, or CRASH events. Configure recipients through `DIAGNOSTICS_EMAIL_TO`; do not commit recipient addresses.
