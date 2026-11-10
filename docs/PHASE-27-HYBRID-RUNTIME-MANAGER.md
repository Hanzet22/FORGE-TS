# Phase 2 — Hybrid Runtime Manager

**Status:** Runtime-management foundation committed; actual environment execution remains gated.

## Implemented

- Central HybridRuntimeManager registry for five independent environments.
- Guarded lifecycle model: NOT_INSTALLED, READY, STARTING, RUNNING, STOPPING, STOPPED, FAILED, and UNAVAILABLE.
- App-private workspace preparation with canonical-path containment checks.
- Separate workspace roots with home, tmp, logs, and data directories for every environment.
- Device-wide RAM, storage, and processor observations.
- Simulation Center integration for workspace status, lifecycle state, start gating, and resource observations.

## Readiness rules

- A prepared workspace is not an installed runtime.
- No environment is marked READY merely because its directories exist.
- Start requests are rejected while no environment-specific execution adapter exists.
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
- PRoot execution or XFCE desktop startup.
- Native desktop/display bridge.
- QEMU or another VM backend.
- RouterOS CHR boot or redistribution.
- Supabase and Upstash integration; neither is required for this local manager foundation.

## Next increments

1. Persist registry and lifecycle metadata safely.
2. Add a verified rootfs import pipeline with archive limits, path-traversal rejection, and rollback.
3. Implement one execution adapter at a time.
4. Add lifecycle cleanup and Android process-death recovery before enabling long-running sessions.
