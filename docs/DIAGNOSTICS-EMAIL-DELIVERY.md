# FORGE-TS — Automatic Diagnostics & Admin Email

## App startup behavior

- Local technical logging starts in every build, including release builds, when the Android Application process starts.
- The app records bounded lifecycle events locally. Runtime lifecycle events remain in the separate bounded runtime journal.
- On first launch, the user sees a diagnostics notice and chooses whether remote diagnostics may be uploaded.
- If allowed, startup events and WARNING/ERROR events are queued automatically and uploaded by WorkManager when a network is available.
- The user can revoke remote upload permission at any time in Settings. Revocation cancels pending unique upload work. Local logging remains enabled.

## Data boundaries

Remote diagnostic events may contain the agreed categories: Info, Warning, Error, Crash, Critical error, Device Specs (manufacturer/model, total RAM and display resolution), CPU Type (chipset model and ABI architecture), and Android Version (release and SDK). Event metadata also includes app version, timestamp, component/event labels, exception class, a per-installation pseudonymous identifier, and technical stack trace. This path does not collect IMEI, serial number, advertising ID, location, account activity, files, network payloads, terminal input/output, passwords, or message contents.

Admin email alerts are limited to WARNING, ERROR, CRITICAL, and CRASH events. Alerts include device specifications, CPU type, Android version, app version, event name, component, and exception class; they omit installation identifiers and stack traces. INFO events remain stored remotely when consented but do not generate email.

## Email delivery configuration

The Edge Function `forge-diagnostic-ingest` supports optional Resend delivery to:

- `papakjamsutnoruu@gmail.com`

The Supabase project owner must configure these Edge Function secrets before email delivery can work:

- `RESEND_API_KEY` — Resend API key, stored only as a Supabase secret.
- `DIAGNOSTICS_EMAIL_FROM` — sender address on a domain verified with Resend. The function also accepts the legacy singular spelling `DIAGNOSTIC_EMAIL_FROM`.

No mail-provider key is embedded in the Android app or Git repository. If either secret is absent, diagnostics still upload to Supabase and the function reports `emailAlert: not_configured`.

## Alert volume

- Email is sent only when an accepted batch contains WARNING, ERROR, CRITICAL, or CRASH events.
- Email alerts are throttled to at most one per installation every 30 minutes.
- Ordinary app-open INFO events are stored remotely (when consented) but do not generate email. This prevents one email per student per app launch.

## Deployment state

- Android first-launch consent gate and Settings control are in the app source.
- Remote diagnostic upload remains opt-in per installation.
- `forge-diagnostic-ingest` deployed as version 4 with `verify_jwt=false` preserved.
- Email delivery is code-ready but remains inactive until the two Resend secrets above are configured and the sender domain is verified.
- A successful Edge Function deployment does not prove a real email was delivered. No synthetic event or test email was sent.
