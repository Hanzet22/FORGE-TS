# FORGE-TS — Automatic Diagnostics & Admin Email

## App startup behavior

- Local technical logging starts in every build, including release builds, when the Android Application process starts.
- The app records bounded lifecycle events locally. Runtime lifecycle events remain in the separate bounded runtime journal.
- On first launch, the user sees a diagnostics notice and chooses whether remote diagnostics may be uploaded.
- If allowed, the complete bounded local diagnostic journal is sent as one JSON envelope when the app moves to the background. WorkManager persists the request, waits for connectivity, and retries transient failures. Pending events are recovered on the next app launch if Android terminates the process before the background callback can run.
- The user can revoke remote upload permission at any time in Settings. Revocation cancels pending unique upload work. Local logging remains enabled.

## Data boundaries

Remote diagnostic events may contain the agreed categories: Info, Warning, Error, Crash, Critical error, Device Specs (manufacturer/model, total RAM and display resolution), CPU Type (chipset model and ABI architecture), and Android Version (release and SDK). Event metadata also includes app version, timestamp, component/event labels, exception class, a per-installation pseudonymous identifier, and technical stack trace. This path does not collect IMEI, serial number, advertising ID, location, account activity, files, network payloads, terminal input/output, passwords, or message contents.

Admin email alerts are limited to ERROR, CRITICAL, and CRASH events. Alerts include device specifications, CPU type, Android version, app version, event name, component, and exception class; they omit installation identifiers and stack traces. INFO and WARNING events remain stored remotely when consented but do not generate email.

## Email delivery configuration

The Edge Function `forge-diagnostic-ingest` supports optional Resend delivery. Recipients are configured only in Supabase secrets; no personal address is hard-coded in the Android app or backend source. The Android manual-export flow opens the user's email app without preselecting a recipient.

The Supabase project owner must configure these Edge Function secrets before email delivery can work:

- `RESEND_API_KEY` — Resend API key, stored only as a Supabase secret.
- `DIAGNOSTICS_EMAIL_FROM` — sender address on a domain verified with Resend. The function also accepts the legacy singular spelling `DIAGNOSTIC_EMAIL_FROM`.
- `DIAGNOSTICS_EMAIL_TO` — one or more comma-separated recipient addresses.

No mail-provider key is embedded in the Android app or Git repository. If either secret is absent, diagnostics still upload to Supabase and the function reports `emailAlert: not_configured`.

## Alert volume

- When Resend is configured, only accepted ERROR, CRITICAL, and CRASH events can trigger a privacy-filtered summary.
- Email summaries are throttled globally to at most one message every 30 minutes. This is a shared project-wide limit, not a per-installation limit.
- The email omits raw stack traces and installation identifiers. Full event records remain in the protected diagnostics table.

## Deployment state

- Android first-launch consent gate and Settings control remain in place; local logging is always on and remote upload requires explicit permission per installation.
- ProcessLifecycleOwner queues upload when the app enters the background; WorkManager handles connectivity and retries.
- The Android worker sends the bounded local journal in one request, and the Edge Function accepts up to 200 events within its 64 KiB request limit.
- Supabase migration `20261111220653_expand_diagnostic_device_metadata` remains applied; no new table or database was added.
- The deployed Edge Function version is tracked in the live Supabase project and must be checked against GitHub before each release.
- The Edge Function accepts both the plural and legacy singular sender-secret names. Deployment does not prove a real email was delivered; no synthetic event or test email is sent during static validation.
