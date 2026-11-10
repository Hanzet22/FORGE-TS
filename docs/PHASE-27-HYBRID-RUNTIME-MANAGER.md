# Phase 2 — Hybrid Runtime Manager

**Status:** P0 adapter contract, per-distribution PRoot workspace registry, and conservative device-capability reporting integrated. Actual execution remains gated until a concrete adapter and installed runtime are supplied.

## Implemented

- Central HybridRuntimeManager registry for five independent environments.
- Guarded lifecycle model: NOT_INSTALLED, READY, STARTING, RUNNING, STOPPING, STOPPED, FAILED, and UNAVAILABLE.
- App-private workspace preparation with canonical-path containment checks.
- Separate workspace roots with home, tmp, logs, and data directories for every environment.
- Independent Debian, Ubuntu, and Alpine PRoot instance workspaces under the existing `proot/` root, with separate persisted lifecycle and workspace metadata.
- Device-wide RAM, storage, and processor observations.
- Explicit capability observations for process ABI, /dev/kvm visibility, advisory file permission flags, KVM API/VM-creation probe states, PRoot ptrace probe state, and per-environment adapter registration.
- Capability states distinguish AVAILABLE, UNAVAILABLE, BLOCKED, and UNKNOWN; unknown or metadata-only observations never imply an executable backend.
- HybridRuntimeManager includes the same foundation capability report in its snapshot; Simulation Center displays the backend gate alongside lifecycle state.
- Simulation Center integration for workspace status, lifecycle state, start gating, and resource observations.

## Readiness rules

- A prepared workspace is not an installed runtime.
- PRoot instance lifecycle transitions are independent per distribution; `READY` requires the instance workspace to be prepared, while the future installer must verify the rootfs before requesting that transition.
- No environment is marked READY merely because its directories exist.
- Start requests resolve an injected environment-specific adapter, inspect its capability, and only persist RUNNING after the adapter confirms the session is active. The default registry is empty, so starts remain safely rejected until a concrete adapter is registered.
- MikroTik RouterOS remains UNAVAILABLE pending licensing, virtualization compatibility, and guest-isolation gates.
- Environment and per-distribution lifecycle metadata are persisted in the existing DataStore registry. Startup converts previously active sessions to `RECOVERY_REQUIRED` and never auto-restarts them.
- Resource readings are device-wide observations, not reservations or guarantees for this app.

## Storage layout

~~~text
files/forge-runtime/
├── manager/
│   ├── registry/
│   ├── state/
│   └── logs/
├── native-xfce/
├── proot/
│   ├── debian/
│   ├── ubuntu/
│   └── alpine/
├── routeros/
├── linux-native/
└── vm-workstation/
    ├── home/
    ├── tmp/
    ├── logs/
    └── data/
~~~

Each environment receives its own private directories. No rootfs, image, or guest data is copied between environments.

## Not implemented

- Distribution downloads, archive extraction, or rootfs installation.
- A concrete Android PRoot adapter is now registered by default. It remains unavailable until the patched native engine, loader, dependency closure and a verified distro rootfs are packaged/installed.
- PRoot disposable-child launch probing, process ownership and adapter-level stop handling are implemented; no distro payload is bundled by this change.
- Native desktop/display bridge.
- QEMU or another VM backend.
- RouterOS CHR boot or redistribution.
- Supabase and Upstash integration; neither is required for this local manager foundation.

## Next increments

1. Add a verified rootfs acquisition and extraction pipeline with archive limits, path-traversal rejection, checksum/signature verification where available, and rollback.
2. Package a provenance-reviewed Android-patched PRoot engine, loader and all required native dependencies for each supported ABI; then validate the process-local ptrace probe on target devices.
3. Add a PTY/terminal bridge and foreground-session service before exposing interactive shell or desktop controls.
4. Harden descendant cleanup and Android process-death recovery before enabling long-running sessions.
4. Implement KVM API/VM-creation probes inside a dedicated VM adapter; do not infer KVM support from the foundation report.
