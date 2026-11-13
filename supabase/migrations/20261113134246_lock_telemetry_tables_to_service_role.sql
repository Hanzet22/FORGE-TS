-- Telemetry tables are server-only. Remove broad API-role grants and
-- grant only the operations required by the trusted Edge Functions.
revoke all privileges on table public.forge_ts_crash_events
  from public, anon, authenticated, service_role;
revoke all privileges on table public.forge_ts_diagnostic_events
  from public, anon, authenticated, service_role;

-- Crash ingestion inserts rows; authenticated crash linking updates user_id
-- through forge-crash-ingest. SELECT is needed for the filtered update path.
grant select, insert, update on table public.forge_ts_crash_events
  to service_role;

-- Diagnostic ingestion only inserts sanitized batches through its Edge Function.
grant insert on table public.forge_ts_diagnostic_events
  to service_role;

-- Lab rows remain owner-scoped by RLS; authenticated clients do not need
-- table-level TRUNCATE, REFERENCES, or TRIGGER privileges.
revoke truncate, references, trigger on table public.forge_ts_lab_records
  from authenticated;

comment on table public.forge_ts_crash_events is
  'Server-only crash telemetry. No direct anon/authenticated table access; use forge-crash-ingest.';
comment on table public.forge_ts_diagnostic_events is
  'Server-only diagnostic telemetry. No direct anon/authenticated table access; use forge-diagnostic-ingest.';
