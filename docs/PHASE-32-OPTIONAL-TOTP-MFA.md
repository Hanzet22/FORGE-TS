# Optional authenticator MFA — FORGE-TS

## Policy

- Password authentication remains available for accounts that have not opted into an authenticator factor.
- TOTP enrollment is user-initiated and is not required at account creation or sign-in.
- A factor is treated as active only when Supabase reports `status = verified`.
- If a verified factor exists, sign-in must be upgraded from AAL1 to AAL2 before protected app features are available.
- Unverified enrollment does not activate the MFA requirement.

## Supabase Auth flow

1. List factors with `GET /auth/v1/factors`.
2. Enroll a TOTP factor with `POST /auth/v1/factors` and `factor_type=totp`.
3. Show the returned QR SVG and temporary setup secret only in the in-memory setup screen.
4. Create a challenge with `POST /auth/v1/factors/{factor_id}/challenge`.
5. Verify the user code with `POST /auth/v1/factors/{factor_id}/verify`.
6. Accept the new session only when its signed JWT contains `aal=aal2`.
7. Permit factor removal through the Supabase unenroll API; refresh the session after removal so its assurance level is current.

The TOTP secret must never be written to DataStore, crash reports, diagnostics, logs, or analytics. Supabase remains the authority for factor status and JWT assurance claims.

## Enforcement

- The FORGE-TS lab-record table receives a restrictive RLS policy based on `auth.mfa_factors`: verified-factor users require AAL2; users without verified factors may use AAL1 or AAL2.
- The Redis gateway checks the verified factor list when the JWT is not AAL2. If a verified factor exists, it returns `mfa_required`; if factor status cannot be checked, it fails closed with `mfa_status_unavailable`.
- Frontend checks are for user experience only. RLS and the Edge Function enforce the authorization boundary.

## Recovery and operational limits

Supabase Auth TOTP does not provide built-in recovery codes in the current project integration. Users should enroll a second authenticator factor where practical and keep their authenticator setup material in a secure place. Losing every verified factor may require administrator-assisted account recovery. Do not claim that a recovery-code flow exists until it is implemented and tested.

Phone MFA is not enabled by this implementation. The initial supported second factor is TOTP through an authenticator app.
