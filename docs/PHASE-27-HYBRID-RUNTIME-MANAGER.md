# Phase 2 — Hybrid Runtime Manager

**Status:** P0 adapter contract and per-distribution PRoot workspace registry integrated; actual execution remains gated until a concrete adapter and installed runtime are supplied.

## Implemented

- Central HybridRuntimeManager registry for five independent environments.
- Guarded lifecycle model: NOT_INSTALLED, READY, STARTING, RUNNING, STOPPING, STOPPED, FAILED, and UNAVAILABLE.
- App-private workspace preparation with canonical-path containment checks.
- Separate workspace roots with home, tmp, logs, and data directories for every environment.
- Independent Debian, Ubuntu, and Alpine PRoot instance workspaces under the existing `proot/` root, with separate persisted lifecycle and workspace metadata.
- Device-wide RAM, storage, and processor observations.
- Simulation Center integration for workspace status, lifecycle state, start gating, and resource observations.

## Readiness rules

- A prepared workspace is not an installed runtime.
- No environment is marked READY merely because its directories exist.
- Start requests resolve an injected environment-specific adapter, inspect its capability, and only persist RUNNING after the adapter confirms the session is active. The default registry is empty, so starts remain safely rejected until a concrete adapter is registered.
- MikroTik RouterOS remains UNAVAILABLE pending licensing, virtualization compatibility, and guest-isolation gates.
- Lifecycle state is currently in-memory and resets when the app process restarts.
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
- A concrete PRoot execution adapter or XFCE desktop startup.
- Native desktop/display bridge.
- QEMU or another VM backend.
- RouterOS CHR boot or redistribution.
- Supabase and Upstash integration; neither is required for this local manager foundation.

## Next increments

1. Add a verified rootfs import pipeline with archive limits, path-traversal rejection, and rollback.
2. Implement and register the PRoot execution adapter against the new contract.
3. Add lifecycle cleanup and Android process-death recovery before enabling long-running sessions.
