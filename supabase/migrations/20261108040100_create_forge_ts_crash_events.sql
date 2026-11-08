-- FORGE-TS crash telemetry: server-only ingestion, no client table access.
create table if not exists public.forge_ts_crash_events (
  id uuid primary key default gen_random_uuid(),
  crash_id uuid not null unique,
  user_id uuid references auth.users(id) on delete set null,
  install_hash text not null check (install_hash ~ '^[0-9a-f]{64}$'),
  occurred_at timestamptz not null,
  app_version varchar(32) not null,
  android_sdk smallint not null check (android_sdk between 1 and 100),
  android_release varchar(32) not null,
  exception_type varchar(200) not null,
  stack_trace text not null check (char_length(stack_trace) <= 40000),
  created_at timestamptz not null default now()
);
create index if not exists forge_ts_crash_events_created_idx
  on public.forge_ts_crash_events (created_at desc);
create index if not exists forge_ts_crash_events_user_created_idx
  on public.forge_ts_crash_events (user_id, created_at desc)
  where user_id is not null;
alter table public.forge_ts_crash_events enable row level security;
revoke all on table public.forge_ts_crash_events from public, anon, authenticated;
grant insert on table public.forge_ts_crash_events to service_role;
comment on table public.forge_ts_crash_events is
  'Sanitized FORGE-TS crash telemetry. Ingestion only through the rate-limited forge-crash-ingest Edge Function; no direct client access.';
