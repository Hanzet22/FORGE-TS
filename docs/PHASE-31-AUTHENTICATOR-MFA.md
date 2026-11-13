# Phase 31 — Authenticator MFA (TOTP)

## Status: Planned, not implemented

The current FORGE-TS source does not contain a TOTP enrollment, factor-listing, challenge, verification, unenrollment, or AAL2 sign-in enforcement flow. The existing sign-in implementation is password-based.

This document is a design boundary, not a statement of delivered functionality. Do not advertise or rely on TOTP protection until the implementation is present in the native client and verified against the configured Supabase Auth project.

## Intended scope

- Optional authenticator-app TOTP enrollment through Supabase Auth.
- QR code and manual setup-key presentation during enrollment.
- Six-digit challenge and server-side verification.
- Factor status and safe unenrollment.
- Sign-in enforcement for accounts with verified TOTP factors.
- Session persistence only after the required assurance level is reached.
- No TOTP secrets in Redis, DataStore, logs, crash reports, or diagnostic events.

## Required verification before marking implemented

1. Confirm the supported Supabase Auth TOTP API contract against current documentation.
2. Implement enrollment, challenge, verification, factor listing, and unenrollment in the native client.
3. Keep enrollment secrets transient and clear them after completion or cancellation.
4. Confirm that password-only accounts retain the intended sign-in behavior.
5. Verify that protected app features do not unlock before the required challenge succeeds.
6. Verify session assurance claims after enrollment, challenge, refresh, and unenrollment.
7. Test using a dedicated non-production account; do not use a throwaway production enrollment.
8. Update README and this document only after source and runtime verification are complete.

## Backend policy boundary

The Redis gateway currently requires an authenticated user, but it does not enforce AAL2 for every request. A future backend-wide AAL2 policy must account for users who have not opted into optional MFA and must not rely only on client-side navigation gates.

## References

- https://supabase.com/docs/guides/auth/auth-mfa
- https://supabase.com/docs/guides/auth/sessions
