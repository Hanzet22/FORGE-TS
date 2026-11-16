-- Remove the optional AAL2 gate while keeping the table's ownership RLS rules.
-- Password authentication remains protected by the existing authenticated role
-- and owner-scoped policies.
drop policy if exists "Require AAL2 for users with verified MFA"
  on public.forge_ts_lab_records;
