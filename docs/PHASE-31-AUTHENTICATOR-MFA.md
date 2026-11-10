# Phase 31 — Authenticator MFA (TOTP)

## Scope
- Optional time-based one-time password (TOTP) MFA using authenticator applications.
- Supabase Auth remains the authority for factor enrollment, challenge, verification and session assurance.
- QR enrollment and manual setup-key fallback are both supported.
- Sign-in requires a TOTP challenge after password authentication whenever the account has a verified TOTP factor.

## Enrollment
1. An authenticated user opens Settings → Authenticator MFA.
2. FORGE-TS calls the Supabase Auth factor enrollment endpoint for a TOTP factor.
3. The app displays the Supabase-generated QR SVG and the same enrollment secret as a manual setup key.
4. If QR scanning is unavailable, the user copies the setup key into a compatible authenticator application.
5. FORGE-TS creates a TOTP challenge and accepts a six-digit code.
6. Supabase verifies the code and promotes the current session to AAL2.
7. The app refreshes the factor list and clears the transient enrollment secret from UI state.

## Sign-in enforcement
- Password sign-in is completed first, yielding a temporary AAL1 session.
- FORGE-TS lists the account's TOTP factors and acts only on factors with status `verified`.
- If a verified factor exists, the app creates a challenge and displays a six-digit authenticator prompt.
- The temporary session is not persisted in ForgeAuthSessionStore and protected tabs are not unlocked until verification succeeds.
- After successful verification, the returned or refreshed session is saved through the existing Android-Keystore-backed session store.
- If no verified TOTP factor exists, the existing password-only sign-in path remains available.

## Factor management
- Settings shows whether a verified TOTP factor is enabled.
- A verified factor can be unenrolled through Supabase Auth. The current session is refreshed after removal so the assurance claim reflects the new factor state.
- Incomplete, unverified enrollment factors can be removed and enrollment restarted.
- Supabase controls factor limits and verification policy.

## Secret handling
- The QR SVG, TOTP secret and otpauth URI are received from Supabase Auth.
- The manual setup secret is shown only during the enrollment flow and can be copied to the device clipboard.
- The secret is not written to Redis, DataStore, crash reports, diagnostic events, README files or application logs.
- Users must keep the setup key private. Anyone who obtains it can generate valid TOTP codes.
- The app does not generate or validate TOTP secrets locally.

## Infrastructure boundaries
- No database schema or migration changes.
- No new Supabase project, table, Edge Function, provider or credential.
- No Redis storage for TOTP secrets, challenges, passwords or sessions.
- Existing Supabase Auth, publishable key, Android Keystore session store and Redis gateway remain in place.
- This phase enforces the second-factor step in the native app sign-in gate; it does not change the Redis gateway to require an AAL2 claim on every request. Backend-wide AAL2 enforcement is a separate policy decision and must preserve access for accounts that have not enabled optional MFA.
- Existing phone authentication remains disabled; this phase is TOTP-only.

## Verification boundary
- Implementation was committed directly to `main`.
- No signup, reset-password, MFA enrollment or live authenticator test was run.
- No Android build success is claimed here. GitHub Actions build status remains the build authority.
- Runtime verification still needs a real account with a compatible authenticator app. Do not use production account enrollment as a throwaway test.
