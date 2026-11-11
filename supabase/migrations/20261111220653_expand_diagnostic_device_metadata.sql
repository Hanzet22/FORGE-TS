alter table public.forge_ts_diagnostic_events
  drop constraint if exists forge_ts_diagnostic_events_level_check;

alter table public.forge_ts_diagnostic_events
  add constraint forge_ts_diagnostic_events_level_check
  check (level in ('DEBUG','INFO','WARNING','ERROR','CRITICAL','CRASH'));

alter table public.forge_ts_diagnostic_events
  add column if not exists device_brand varchar(48) not null default 'unknown',
  add column if not exists device_model varchar(80) not null default 'unknown',
  add column if not exists device_ram_mb integer not null default 0,
  add column if not exists display_resolution varchar(32) not null default 'unknown',
  add column if not exists cpu_chipset varchar(80) not null default 'unknown',
  add column if not exists cpu_architecture varchar(40) not null default 'unknown';
