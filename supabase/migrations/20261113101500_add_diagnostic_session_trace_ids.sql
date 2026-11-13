alter table public.forge_ts_diagnostic_events
  add column if not exists session_id uuid,
  add column if not exists trace_id uuid;

create index if not exists forge_ts_diagnostic_events_session_id_idx
  on public.forge_ts_diagnostic_events (session_id)
  where session_id is not null;

create index if not exists forge_ts_diagnostic_events_trace_id_idx
  on public.forge_ts_diagnostic_events (trace_id)
  where trace_id is not null;
