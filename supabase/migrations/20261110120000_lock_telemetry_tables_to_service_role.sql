-- Telemetry tables are server-only. Keep RLS enabled and explicitly
-- remove Supabase's broad public-schema default table grants from API roles.
-- Ingestion and crash linking are performed by trusted Edge Functions only.

revoke all privileges on table public.forge_ts_crash_events
  from public, anon, authenticated;
revoke all privileges on table public.forge_ts_diagnostic_events
  from public, anon, authenticated;

-- Crash ingestion inserts rows; authenticated crash linking updates user_id
-- through forge-crash-ingest. SELECT is needed for the filtered update path.
grant select, insert, update on table public.forge_ts_crash_events
  to service_role;

-- Diagnostic ingestion only inserts sanitized batches through its Edge Function.
grant insert on table public.forge_ts_diagnostic_events
  to service_role;

comment on table public.forge_ts_crash_events is
  'Server-only crash telemetry. No direct anon/authenticated table access; use forge-crash-ingest.';
comment on table public.forge_ts_diagnostic_events is
  'Server-only diagnostic telemetry. No direct anon/authenticated table access; use forge-diagnostic-ingest.';
