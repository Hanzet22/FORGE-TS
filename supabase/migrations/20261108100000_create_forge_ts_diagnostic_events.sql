create table if not exists public.forge_ts_diagnostic_events (
  id uuid primary key default gen_random_uuid(),
  event_id uuid not null unique,
  install_hash text not null,
  occurred_at timestamptz not null,
  app_version varchar(32) not null,
  android_sdk smallint not null check (android_sdk between 1 and 100),
  android_release varchar(32) not null,
  level varchar(8) not null check (level in ('DEBUG','INFO','WARNING','ERROR')),
  tag varchar(48) not null,
  event_name varchar(120) not null,
  exception_type varchar(200) not null default '',
  stack_trace text not null default '',
  created_at timestamptz not null default now()
);

alter table public.forge_ts_diagnostic_events enable row level security;

create index if not exists forge_ts_diagnostic_events_occurred_at_idx
  on public.forge_ts_diagnostic_events (occurred_at desc);
create index if not exists forge_ts_diagnostic_events_install_hash_idx
  on public.forge_ts_diagnostic_events (install_hash, occurred_at desc);
