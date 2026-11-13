-- Optional MFA enforcement for authenticated FORGE-TS lab records.
-- Accounts without a verified factor may use AAL1; opted-in accounts require AAL2.
create policy "Require AAL2 for users with verified MFA"
  on public.forge_ts_lab_records
  as restrictive
  for all
  to authenticated
  using (
    array[(select auth.jwt()->>'aal')] <@ (
      select
        case
          when count(id) > 0 then array['aal2']
          else array['aal1', 'aal2']
        end
      from auth.mfa_factors
      where (select auth.uid()) = user_id
        and status = 'verified'
    )
  )
  with check (
    array[(select auth.jwt()->>'aal')] <@ (
      select
        case
          when count(id) > 0 then array['aal2']
          else array['aal1', 'aal2']
        end
      from auth.mfa_factors
      where (select auth.uid()) = user_id
        and status = 'verified'
    )
  );

comment on policy "Require AAL2 for users with verified MFA" on public.forge_ts_lab_records is
  'Restrictive optional-MFA policy: verified-factor accounts require AAL2; other authenticated users retain AAL1 access.';
