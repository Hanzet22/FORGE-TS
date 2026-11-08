-- FORGE-TS Phase 26: authenticated, per-user lab records.
-- No anonymous access; RLS binds every row to auth.uid().

create table if not exists public.forge_ts_lab_records (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  label varchar(80) not null check (char_length(btrim(label)) between 1 and 80),
  value varchar(500) not null check (char_length(value) <= 500),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists forge_ts_lab_records_owner_created_idx
  on public.forge_ts_lab_records (owner_id, created_at desc);

alter table public.forge_ts_lab_records enable row level security;

revoke all on table public.forge_ts_lab_records from anon;
grant select, insert, update, delete on table public.forge_ts_lab_records to authenticated;

drop policy if exists "Users can read their own FORGE-TS lab records" on public.forge_ts_lab_records;
create policy "Users can read their own FORGE-TS lab records"
  on public.forge_ts_lab_records for select to authenticated
  using ((select auth.uid()) = owner_id);

drop policy if exists "Users can insert their own FORGE-TS lab records" on public.forge_ts_lab_records;
create policy "Users can insert their own FORGE-TS lab records"
  on public.forge_ts_lab_records for insert to authenticated
  with check ((select auth.uid()) = owner_id);

drop policy if exists "Users can update their own FORGE-TS lab records" on public.forge_ts_lab_records;
create policy "Users can update their own FORGE-TS lab records"
  on public.forge_ts_lab_records for update to authenticated
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists "Users can delete their own FORGE-TS lab records" on public.forge_ts_lab_records;
create policy "Users can delete their own FORGE-TS lab records"
  on public.forge_ts_lab_records for delete to authenticated
  using ((select auth.uid()) = owner_id);

comment on table public.forge_ts_lab_records is
  'FORGE-TS per-user lab records. RLS isolates every user; no anonymous access.';
