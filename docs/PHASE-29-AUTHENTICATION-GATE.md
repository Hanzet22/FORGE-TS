# Phase 29 — Central Authentication Gate

## Access policy

- Home, Learn, Network and Settings remain available without authentication.
- The Simulate and Tools navigation entries remain visible.
- Without an active Supabase Auth session, selecting either protected tab displays the shared authentication gate instead of mounting its feature screen.
- Feature panels and actions inside those two tabs are unreachable through normal application navigation until authentication succeeds.
- Home shortcuts that navigate directly to Tools are subject to the same root-level gate.

## Authentication flow

- Create Account and Sign-in Account use the existing Supabase Auth email/password REST endpoints.
- Reset Password requests a Supabase recovery email. The registered custom-scheme callback exchanges the recovery access/refresh tokens, opens the in-app new-password form, and updates the password through the authenticated Supabase user endpoint.
- Password creation and reset enforce the configured policy: at least 20 characters with uppercase, lowercase, a number and a symbol.
- Email confirmation and password-recovery callbacks return through the registered custom-scheme auth callback deep link.
- A process-wide session state is shared by the authentication gate and app shell.
- The session is considered active only until its Supabase expiry timestamp.
- Sign-out calls the Supabase logout endpoint when possible and always clears the local in-memory session.
- The app shell restores the encrypted session on startup and refreshes it before expiry. The former Remote Access panel is no longer part of the UI.

## Security boundaries

- The Supabase publishable key is public client configuration; no service-role or Upstash credential is added to the APK.
- The existing Redis gateway continues to require a Supabase JWT with JWT verification enabled.
- The existing lab-record table continues to rely on its owner-scoped RLS policies.
- The gate is client-side feature access control, not a replacement for server-side JWT verification or RLS.
- Session tokens are stored with Android Keystore-backed encryption and restored when the app reopens. Sign-out clears the encrypted local session.
- No new database table, migration, Edge Function, or Redis database is required for this phase.

## Redis scope

The existing Upstash database is named hyperai-security-redis and is not a dedicated FORGE-TS resource. This phase does not write to it, repurpose it, or expose its credentials. The already deployed FORGE-TS Redis gateway remains a separate server-side integration boundary.

## Verification

- Source review confirms the gate is applied at the root navigation switch for both Simulate and Tools, including Home-to-Tools navigation.
- Authenticated session state is owned by the app shell and shared with the root authentication gate; there is no separate Remote Access login state.
- GitHub Actions is the authoritative Android build check; no local Android build is claimed by this commit.
