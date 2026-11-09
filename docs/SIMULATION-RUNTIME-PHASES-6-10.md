# FORGE-TS Simulation Runtime Roadmap (Phases 6–10)

This roadmap is the **Simulation Runtime** track. It is separate from the older product milestones documented in files such as `PHASE-6-LOCAL-DATA-AND-SETTINGS.md`.

## Phase 6 — Runtime Foundation
- Inspect Android API level, supported ABIs, CPU count, available/total RAM and app-filesystem capacity.
- Prepare app-private `files/forge-runtime/{images,instances,logs}` directories and verify canonical path containment.
- Report only whether `/dev/kvm` is visible/readable/writable. This is not a KVM capability test.
- No process execution, QEMU launch, image download, guest, virtual network, device passthrough, or cloud dependency.
- Exit: inspection is visible in Simulation Center; paths remain inside app-private storage; limitations are explicit.

## Phase 7 — RouterOS Integration
- Integrate only after confirming official ARM64 CHR image terms, checksum verification, machine/firmware compatibility and a supported virtualization backend.
- Keep RouterOS as a genuine guest, not a command simulator.
- RouterOS CLI access must terminate inside the guest; never route user commands to Android host shell.
- Exit: reproducible guest boot and isolation evidence on a supported device/backend.

## Phase 8 — XFCE4 Integration
- Provision Linux userland separately from the RouterOS guest.
- Integrate display lifecycle/fullscreen with clear process ownership and graceful shutdown.
- Exit: stable desktop lifecycle and verified separation from the RouterOS guest.

## Phase 9 — Networking Integration
- Create a private guest management network; expose no public inbound management by default.
- Keep QEMU monitor and host control interfaces inaccessible to guests.
- Make guest internet and physical LAN bridging explicit opt-ins.
- Exit: documented network boundaries and verified connectivity/isolation behavior.

## Phase 10 — Account & Persistence
- Persist local instance metadata and configuration using app-private storage.
- Protect secrets with Android Keystore; never sync RouterOS credentials or privileged runtime secrets to cloud metadata/telemetry.
- Keep local runtime startup independent of Supabase and Redis availability.
- Exit: offline relaunch, safe backup/restore behavior, and validated account-to-local-instance ownership.

## Global gates
- No assumption that ARM64 ABI implies KVM/AVF support.
- TCG must not be represented as a secure guest-isolation boundary.
- No use of unrelated Supabase projects or the HyperAI Redis database as a FORGE-TS runtime dependency.
- Validate each phase before starting the next; do not merge phase scope.
