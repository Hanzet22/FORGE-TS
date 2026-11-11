# Phase 43 — WorkStation Display Session Host

## Scope
Adds a backend-neutral host-side coordinator for the existing `DisplaySessionAdapter` contract. The controller coordinates capability inspection, start readiness, Android Surface attachment, bounded input forwarding, resize, and stop under a serialized session lifecycle.

## Delivered
- `DisplaySessionController` owns one active display session at a time.
- A backend must be explicitly registered; the default registry remains empty.
- Start is accepted as RUNNING only when the adapter reports both `accepted=true` and `ready=true`.
- An accepted-but-not-ready start triggers a best-effort backend stop and is reported as FAILED, never RUNNING.
- Surface attachment, input and resize are rejected unless a session is confirmed RUNNING.
- Surface dimensions and density are bounded before forwarding.
- Stop clears the active session only when the adapter confirms completion.
- No X11 server, native binary, companion app, backend dependency, database, Supabase change or Redis change is introduced.

## Not claimed
- No embedded X11 backend is registered.
- No XFCE desktop can be launched from this milestone.
- No device compatibility is claimed from Android version or ABI alone.
- Surface rendering, keyboard/touch delivery and full desktop behavior still require a concrete backend and device validation.

## Next gate
Pin the selected embedded backend and its minimum Android API only after source, ABI, package assumptions, integration path and GPL/source obligations are reviewed. Then implement the Android display host and validate on a supported physical device.
