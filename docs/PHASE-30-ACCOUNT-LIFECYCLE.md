# Phase 30 — Account Lifecycle & Session Management

> **Status context:** This document records phase scope, design assumptions or acceptance criteria and may describe work that was later changed. The live implementation state is summarized in the [development handoff](DEVELOPMENT-HANDOFF.md); verify current source and CI before claiming a feature is available.

## Account overview
- Settings exposes the current Supabase account email and stable user ID.
- Email verification state is read from the authenticated Supabase user endpoint; unavailable network/profile responses are shown as unavailable, not treated as unverified.
- The active session expiry is displayed in local device time.

## Session controls
- The user can explicitly refresh the current Supabase refresh token.
- Successful refresh replaces and persists the session through the existing Android-Keystore-backed store.
- Failed refresh reports the returned error. If the saved session is already expired, local session state is cleared so protected features return to the authentication gate.
- Sign-out attempts server-side revocation and always clears the local session on this device.

## Password management
- An authenticated user can change their password from Settings.
- The existing policy remains: at least 20 characters with uppercase, lowercase, a number and a symbol.
- New-password and confirmation fields include independent visibility controls.
- The authentication gate's sign-in, account-creation, recovery and confirmation password fields also include show/hide controls.

## Security and infrastructure boundaries
- Supabase Auth remains the identity and session authority.
- Access and refresh tokens remain encrypted by Android Keystore-backed storage.
- No password, access token or refresh token is written to Redis.
- The existing single Upstash Redis database, logical namespaces, gateway, Supabase schema, RLS policies and Edge Functions are unchanged.
- No database migration, new database, new Edge Function, new credential or provider change is required.

## Verification boundary
- Changes are committed directly to main.
- No authentication emails were sent and no repeated live sign-in/reset tests were run.
- No Android build success is claimed here; GitHub Actions remains the authoritative build result.
