drop policy if exists "Require AAL2 for users with verified MFA"
  on public.forge_ts_lab_records;

create policy "Require AAL2 for users with verified MFA"
  on public.forge_ts_lab_records
  as restrictive
  for all
  to authenticated
  using (
    array[((select auth.jwt()) ->> 'aal')] <@ (
      select
        case
          when exists (
            select 1
            from auth.mfa_factors
            where user_id = (select auth.uid())
              and status = 'verified'
          ) then array['aal2']
          else array['aal1', 'aal2']
        end
    )
  )
  with check (
    array[((select auth.jwt()) ->> 'aal')] <@ (
      select
        case
          when exists (
            select 1
            from auth.mfa_factors
            where user_id = (select auth.uid())
              and status = 'verified'
          ) then array['aal2']
          else array['aal1', 'aal2']
        end
    )
  );

comment on policy "Require AAL2 for users with verified MFA" on public.forge_ts_lab_records is
  'Restrictive optional-MFA policy: verified-factor accounts require AAL2; other authenticated users retain AAL1 access.';
