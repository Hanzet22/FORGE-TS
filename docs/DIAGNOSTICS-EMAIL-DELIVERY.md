# FORGE-TS — Automatic Diagnostics & Admin Email

## App startup behavior

- Local technical logging starts in every build, including release builds, when the Android Application process starts.
- The app records bounded lifecycle events locally. Runtime lifecycle events remain in the separate bounded runtime journal.
- On first launch, the user sees a diagnostics notice and chooses whether remote diagnostics may be uploaded.
- If allowed, startup events and WARNING/ERROR events are queued automatically and uploaded by WorkManager when a network is available.
- The user can revoke remote upload permission at any time in Settings. Revocation cancels pending unique upload work. Local logging remains enabled.

## Data boundaries

Remote diagnostic events may contain app version, Android version/SDK, a per-installation pseudonymous identifier, event severity/name, component label, exception class and technical stack trace. They must not contain passwords, terminal input/output, message contents, or network payloads.

Admin email alerts are deliberately narrower: only WARNING/ERROR summaries are sent. The email omits installation identifiers and stack traces. The complete accepted event records remain in the protected Supabase diagnostics store.

## Email delivery configuration

The Edge Function `forge-diagnostic-ingest` supports optional Resend delivery to:

- `papakjamsutnoruu@gmail.com`

The Supabase project owner must configure these Edge Function secrets before email delivery can work:

- `RESEND_API_KEY` — Resend API key, stored only as a Supabase secret.
- `DIAGNOSTICS_EMAIL_FROM` — sender address on a domain verified with Resend.

No mail-provider key is embedded in the Android app or Git repository. If either secret is absent, diagnostics still upload to Supabase and the function reports `emailAlert: not_configured`.

## Alert volume

- Email is sent only when an accepted batch contains WARNING or ERROR events.
- Email alerts are throttled to at most one per installation every 30 minutes.
- Ordinary app-open INFO events are stored remotely (when consented) but do not generate email. This prevents one email per student per app launch.

## Deployment state

- Android first-launch consent gate and Settings control are in the app source.
- Remote diagnostic upload remains opt-in per installation.
- `forge-diagnostic-ingest` deployed as version 4 with `verify_jwt=false` preserved.
- Email delivery is code-ready but remains inactive until the two Resend secrets above are configured and the sender domain is verified.
- A successful Edge Function deployment does not prove a real email was delivered. No synthetic event or test email was sent.
