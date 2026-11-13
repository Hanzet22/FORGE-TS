-- Canonical telemetry event category; default preserves compatibility with older app versions.
alter table public.forge_ts_diagnostic_events
  add column if not exists category varchar(32) not null default 'GENERAL';

alter table public.forge_ts_diagnostic_events
  drop constraint if exists forge_ts_diagnostic_events_category_check;

alter table public.forge_ts_diagnostic_events
  add constraint forge_ts_diagnostic_events_category_check
  check (category in (
    'GENERAL', 'APP_LIFECYCLE', 'ACTIVITY', 'NAVIGATION', 'FEATURE',
    'NETWORK', 'DNS', 'STORAGE', 'DATABASE', 'BACKGROUND', 'PERMISSION',
    'RUNTIME', 'MEDIA', 'PERFORMANCE', 'SECURITY', 'CRASH', 'TELEMETRY'
  ));
