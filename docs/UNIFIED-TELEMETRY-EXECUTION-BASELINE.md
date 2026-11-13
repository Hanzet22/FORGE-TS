# FORGE-TS Unified Telemetry — Execution Baseline

Status: implementation baseline adopted from repository audit  
Target: `Hanzet22/FORGE-TS` / `main`  
Package: `com.HG.Forge.Ts`

## 1. Repository facts verified

The current Android telemetry path is already composed of:

- `ForgeDiagnostics`: bounded local JSON journal (`forge-diagnostic-events.json`), capped at 200 events and 48 KiB.
- `ForgeDiagnosticsUploadWorker`: WorkManager delivery with network constraint, exponential backoff, unique work, and acknowledgement by event ID.
- `CrashReportStore` / `CrashReportUploadWorker`: separate crash report capture and upload path.
- Supabase Edge Functions: `forge-diagnostic-ingest` and `forge-crash-ingest`.
- Upstash Redis: per-installation request limits and email throttling.
- Resend: minimized administrative digest to the configured recipient.
- RLS and SQL grants: telemetry tables are server-only; ingestion is performed by trusted Edge Functions.

Room and WorkManager dependencies are present in Gradle, but the inspected telemetry implementation currently persists its bounded journal as an app-private JSON file. Room is not yet the active telemetry queue.

## 2. Architecture decision

Do not replace the live ingestion path with a public Apps Script endpoint in this execution.

Reason: a static shared token embedded in an Android APK is extractable. A public Apps Script Web App that executes as its owner and sends MailApp messages would expose owner-authorized quota and mail-sending capability to abuse unless an independently trustworthy authorization and abuse-control layer is established. Apps Script/MailApp may be considered later as an email relay, not as an unauthenticated replacement for the existing ingest gateway.

Keep the current runtime path until a migration has equivalent authentication, rate limiting, deduplication, privacy controls, and a tested rollback route:

`Android instrumentation -> local durable journal -> WorkManager -> Supabase Edge Function -> Upstash rate limit -> protected Supabase table -> minimized Resend digest`

No Supabase, Upstash, GitHub Actions, Auth, database policy, or deployment configuration is removed by this baseline.

## 3. Canonical event envelope

The next implementation increment should standardize event naming and metadata without breaking the current server contract.

Required fields:
- `schemaVersion`: integer, initially 1.
- `eventId`: UUID generated once at event creation and retained across retries.
- `occurredAt`: UTC ISO-8601 timestamp.
- `appVersion`, `androidSdk`, `androidRelease`.
- `level`: DEBUG, INFO, WARNING, ERROR, CRITICAL, or CRASH.
- `category`: canonical category from the registry below.
- `component`: stable module or subsystem identifier.
- `eventName`: lowercase dot-separated canonical event name.
- `attributes`: bounded, allowlisted scalar metadata only.

Correlation fields to add in a backward-compatible schema revision:
- `sessionId`: random per foreground session.
- `traceId`: optional identifier for a multi-step operation.
- `parentEventId`: optional causal event reference.

Do not generate a new event ID during retry. Server acknowledgement must continue to identify exactly which events can be removed from the local queue.

## 4. Canonical category registry

| Category | Scope | Example event names |
|---|---|---|
| APP_LIFECYCLE | Application/process | `application.created`, `session.foreground`, `session.background`, `session.interrupted_detected` |
| ACTIVITY | Android Activity | `activity.created`, `activity.resumed`, `activity.paused` |
| NAVIGATION | Screens and routes | `navigation.screen_entered`, `navigation.route_failed` |
| FEATURE | User-facing feature operations | `feature.started`, `feature.completed`, `feature.failed` |
| NETWORK | Connectivity and HTTP | `network.connectivity_changed`, `network.request_failed` |
| DNS | DNS lookup and records | `dns.lookup_started`, `dns.lookup_completed`, `dns.lookup_failed` |
| STORAGE | Local persistence/cache | `storage.read_failed`, `storage.write_failed` |
| DATABASE | Room or other local database | `database.query_failed`, `database.migration_failed` |
| BACKGROUND | WorkManager tasks | `background.work_started`, `background.work_retry`, `background.work_completed` |
| PERMISSION | Runtime permissions | `permission.requested`, `permission.result` |
| RUNTIME | PRoot, PTY, display/session runtime | `runtime.start_requested`, `runtime.ready`, `runtime.failed`, `runtime.stopped` |
| MEDIA | Intro/video/audio playback | `media.prepare_failed`, `media.first_frame`, `media.playback_error` |
| PERFORMANCE | Startup, memory, frame and responsiveness | `performance.startup_completed`, `performance.memory_pressure`, `performance.frame_slow` |
| SECURITY | Local security and validation boundaries | `security.validation_rejected`, `security.permission_denied` |
| CRASH | Fatal and recoverable failures | `crash.uncaught_exception`, `crash.recovery_shown` |
| TELEMETRY | Telemetry pipeline health | `telemetry.queue_full`, `telemetry.upload_started`, `telemetry.upload_accepted`, `telemetry.upload_rejected` |

## 5. Module coverage map

- Home: screen entry, startup readiness, intro media lifecycle, navigation actions.
- Learn: lesson open/complete, simulation start/complete/fail.
- Network: CIDR/VLSM calculations, DNS lookup, RDAP and HTTP operation outcomes.
- Tools: tool invocation, operation outcome, validation failures.
- Settings: preference changes, consent changes, permission state.
- Hybrid runtime: PRoot provisioning, PTY lifecycle, display-session lifecycle, runtime state transitions.
- Shared platform: application/session lifecycle, Activity lifecycle, background work, connectivity, storage, crash recovery.

Instrumentation must not log from a Compose body during recomposition. Use lifecycle-aware effects, navigation callbacks, repository/service boundaries, or explicit user-operation callbacks.

## 6. Privacy and data minimization

Never collect or transmit:
- passwords, access tokens, API keys, cookies, authorization headers, or secrets;
- email/message contents, clipboard contents, or user-entered command text;
- raw DNS/HTTP payloads or arbitrary request/response bodies;
- precise location, contacts, or unrelated device identifiers;
- full stack traces in administrator email.

Raw crash stack frames may remain in the existing protected crash-report path subject to explicit consent and the existing user review/share controls. Administrative email must contain only minimized event summaries and exception class names. All new attributes must be allowlisted, length-bounded, and scrubbed before persistence.

## 7. Delivery and failure semantics

- Local persistence precedes network upload.
- Upload only under the existing diagnostic consent setting.
- Use WorkManager with a connected-network constraint and exponential backoff.
- Preserve unique-work behavior to prevent concurrent duplicate uploaders.
- Treat delivery as at-least-once; server-side event-ID deduplication is required.
- Remove locally queued events only after a successful server acknowledgement.
- Never interpret process destruction as a guaranteed `APP_EXIT`; detect an interrupted prior session on the next launch instead.
- Telemetry failure must never block normal app startup or core features.

## 8. Implementation order

1. Introduce canonical category/event constants and a backward-compatible event API.
2. Add session/correlation identifiers and a versioned event envelope.
3. Instrument app lifecycle, navigation, feature/repository boundaries, network, background work, runtime, and media.
4. Extend the ingest schema and server validation compatibly; preserve RLS and server-only writes.
5. Add pipeline self-diagnostics and a minimized email digest.
6. Evaluate Room migration only if measured journal contention, crash-consistency, or queue requirements justify it; do not add a second persistence path merely because the dependency exists.
7. Consider Apps Script/MailApp only after a secure relay design and quota/abuse controls are independently established.

## 9. Execution boundary

This document records the architecture and schema baseline. It does not claim that runtime instrumentation, database migrations, endpoint deployment, or end-to-end delivery have been implemented. Those changes must be made in focused commits with one bounded validation pass, not repeated test loops.


## 10. Execution log — 2026-11-13

Completed:
- Added category inference to the Android diagnostic event writer.
- Added category validation and persistence to `forge-diagnostic-ingest`.
- Added category to the minimized administrative email summary.
- Applied the additive database migration to project `FORGE-TS-LAB`; database migration history reports version `20261113093925`.
- Deployed `forge-diagnostic-ingest` as Edge Function version 15. Existing `verify_jwt=false` setting was preserved; request API-key validation and Upstash rate limiting remain in the function.
- Aligned the repository migration filename with the version recorded by the database.

Validation boundary:
- The Android Debug APK GitHub Actions run was observed in progress at the `Assemble debug APK` step. No claim of build success is made until GitHub reports its conclusion.
- No synthetic telemetry event was sent and no repeated build/test cycle was started.
- Full module instrumentation, session/trace correlation fields, queue redesign, and runtime delivery confirmation remain pending.


## Execution log — 2026-11-13 (session and trace correlation)

- Added a process-scoped `sessionId` to every Android diagnostic event. It is generated locally and is not a device/account identifier.
- Added optional `traceId` support to `ForgeDiagnostics.log`; callers can generate one via `ForgeDiagnostics.newTraceId()` and pass the same ID to related log calls.
- Updated the diagnostic ingest function to validate optional UUID correlation fields and persist them as `session_id` and `trace_id`.
- Added nullable database columns and partial indexes. Older app versions remain compatible because the new fields are optional at the server/database boundary.
- Trace propagation is now available in the API; operation-specific instrumentation still needs to pass a shared trace ID at the relevant paired log callsites.
- No synthetic events were submitted. One CI validation pass is expected from the commit; no repeated test loop.

- Added primary navigation instrumentation for Home, Learn, Network, Simulate, Tools, and Settings. A tab-selection event and its corresponding tab-entry event share the same trace ID.
- Home-screen tool shortcuts now pass through the same navigation instrumentation when they switch tabs.

- Added operation-level network telemetry for DNS lookup, public-IP lookup, and authorized single-host TCP/TLS checks. Each operation emits started/succeeded/failed plus bounded duration events correlated by trace ID.
- Network telemetry intentionally excludes the supplied domain, hostname, IP address, port, DNS answer contents, and response body. No request payloads or traffic contents are captured.
- Local CIDR/VLSM planning remains local and is not uploaded as network telemetry.

## Runtime Telemetry

Runtime lifecycle instrumentation is emitted from the existing `HybridRuntimeManager` orchestration boundary. Start and stop operations receive a shared trace ID across their lifecycle transitions, outcome, and bounded duration events. Events contain only fixed runtime identifiers and lifecycle states; they never include terminal input, command arguments, command output, workspace paths, environment variables, or process output.

- Start: requested, STARTING/RUNNING/FAILED transition, accepted/rejected outcome, bounded duration.
- Stop: requested, STOPPING/STOPPED/FAILED transition, completed/incomplete outcome, bounded duration.
- Duration values are capped at ten minutes and encoded only in the event name.
- Per-keystroke PTY input/output is deliberately not instrumented.
- Existing app-private runtime journal remains a local state-transition journal; cloud telemetry uses the existing consent-gated diagnostics pipeline.
- Native PTY and PRoot execution internals are not modified by this increment.


### Runtime execution and PTY session increment

- Added operation-level telemetry around `executeProotCommand` at the `HybridRuntimeManager` boundary: requested, completed, non-zero exit, incomplete, failed, cancelled, and bounded duration outcomes.
- Added native PRoot PTY session lifecycle events: start requested, start confirmed, startup verification failure, early exit during startup, reader closure, and bounded successful-start duration.
- PRoot distribution is represented only by the existing fixed enum context where already available; no command text, argument values, stdin, stdout/stderr, terminal bytes, workspace paths, environment variables, hostnames, or process output are included in cloud events.
- PTY instrumentation is session-level only. No per-keystroke, per-read, per-write, or resize telemetry is emitted.
- No Supabase schema or Edge Function change was required; events continue through the existing consent-gated diagnostics pipeline.
- This increment is committed to `main`; Android Debug APK and CodeQL validation for the latest commit are pending. CI success is not inferred from an in-progress run.

### Media telemetry and adaptive intro layout

- Added one playback-scoped trace ID covering intro preparation, prepared state, playback start, first rendered frame, terminal outcome, and bounded playback duration.
- Added terminal outcomes for natural completion, playback error/start failure, surface destruction, and view detachment. Duplicate terminal events are suppressed.
- First-frame telemetry is latched once; no per-frame events are collected.
- Added layout-change events for the actual intro view bounds and a portrait/landscape classification. Repeated identical dimensions are deduplicated. This records layout dimensions only; no media URI, file path, content, audio, or frame data is collected.
- The TextureView center-crop transform is recalculated on view-size and surface-size changes. It fills the available intro bounds without distorting the source aspect ratio, including resizable split-screen windows.
- Playback duration is measured with monotonic elapsed time and capped at ten minutes.
- This is instrumentation and adaptive rendering logic only; it does not assert that playback has been verified on physical devices in every orientation or multi-window mode.


## Consent revocation and re-enrollment semantics — 2026-11-13

- Remote diagnostics remain opt-in; local technical event recording remains available.
- Turning remote diagnostics off persists the disabled state before cancelling queued work, clears the pending diagnostic upload journal, and marks queued crash reports as ineligible for later upload. Crash recovery records remain available locally.
- Turning remote diagnostics on after an opt-out starts a clean upload window: events accumulated while remote consent was off are not retroactively uploaded.
- A request already accepted by the network stack at the instant consent is revoked cannot be recalled; workers re-check consent immediately before starting each upload.
- Legacy crash reports without an explicit remote-upload eligibility flag are treated as local-only by default.


## Pseudonymous installation identity — 2026-11-13

- The random installation identifier is pseudonymous, not anonymous; it supports rate limiting and event correlation without using an account identifier.
- The identifier is rotated whenever remote diagnostics consent changes. Pending diagnostics are purged on opt-out and old crash reports are marked local-only, preventing cross-consent correlation and retroactive upload.
- Device metadata remains disclosed in Settings and is only uploaded while remote diagnostics consent is enabled.
