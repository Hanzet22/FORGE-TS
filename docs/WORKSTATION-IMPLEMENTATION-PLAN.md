# FORGE-TS WorkStation — Implementation Plan

**Status:** Active; P0 lifecycle journaling delivered; P1 display adapter contract, serialized host controller and MagicDesk X11 API/ABI admission gate delivered; native engine and host bridge not integrated  
**Baseline:** [Final Architecture Specification](WORKSTATION-ARCHITECTURE.md)  
**Date:** 2026-11-10

## Delivery principles

- Follow the approved architecture baseline.
- Work directly on the repository's `main` branch unless a later instruction changes this.
- Avoid repeated exploratory tests. Use one focused validation per milestone and keep moving when a non-blocking CI issue appears; report it accurately.
- Do not add a backend, database, Redis instance or cloud dependency for local workstation runtime management.
- Do not bundle distro RootFS images or installer ISOs into the APK.
- Never label a PRoot session as Native Linux or a PRoot environment as a VM.

## P0 — Hybrid Manager Foundation (extend existing foundation)

Existing code to preserve and extend:
- `HybridRuntimeManager` already persists a registry with DataStore, manages app-private workspaces and exposes resource snapshots.
- `RuntimeFoundationSupervisor` already inspects Android API/ABI, resources and `/dev/kvm` node visibility/permissions.
- `LinuxUserlandFoundationSupervisor` already prepares a private Linux workspace.

Deliverables:
- Add a runtime execution-adapter contract and registry; do not create a parallel manager.
- Integrate adapter capability reports with the existing persistent manager.
- Preserve the existing `RuntimeLifecycleState` values and DataStore records; document any required state migration before changing them.
- Separate workspace-prepared state from runtime-installed and runtime-running state.
- Add resource policy and bounded local diagnostic events. A 64 KiB app-private lifecycle journal is now wired to Hybrid Manager and per-distro PRoot state changes; it records state metadata only, not terminal output or credentials.

Acceptance:
- UI can enumerate runtime adapters and display their real state.
- Unsupported capabilities are explicit.
- Adapter failures do not corrupt another runtime's state.

## P1 — Display Foundation and XFCE Native

Reuse the existing Linux-userland and Native runtime foundation code where it fits; do not interpret workspace readiness as an installed runtime.

Deliverables:
- Embedded X11 feasibility and licensing decision (review recorded).
- Backend-neutral DisplaySessionAdapter/Registry contract for readiness, Android Surface attachment, bounded pointer/touch/scroll/key/text input, resize and stop. No concrete backend is registered yet.
- Backend-neutral `DisplaySessionController` serializes inspect/start/Surface attach/input/resize/stop and permits only one active display session.
- Start is marked RUNNING only after the adapter confirms readiness; partial starts are stopped best-effort and never reported as running.
- Surface dimensions and density are bounded before forwarding; host retains Surface ownership.
- Android Compose display host and native input bridge remain pending until a backend is pinned.
- Native Linux capability detector.
- Native runtime adapter and session lifecycle.

Acceptance:
- No mandatory external X11 companion application.
- Device without required Native Linux privileges is marked Unsupported.
- Native mode never silently falls back to PRoot.

Implementation gate:
- Feasibility review: [Embedded Display Feasibility](WORKSTATION-DISPLAY-FEASIBILITY.md).
- The MagicDesk X11 fork is the leading API 34+ candidate, but it is not integrated; its device coverage is still pending. The MIT Java X server is not demonstrated to support the complete XFCE workload, and XSDL has material rendering/input limitations.
- Do not commit to a specific embedded X11 codebase until its Android integration, ABI, package assumptions and license are reviewed.
- Phase 43 records the host-side controller only; it does not add an X11 server, native binary, companion app or claim XFCE desktop readiness. See [Phase 43](PHASE-43-WORKSTATION-DISPLAY-SESSION-HOST.md).
- Phase 44 records the pinned MagicDesk X11 candidate revision and adds an API 34+/ARM64 admission gate. The gate deliberately blocks until the native library and JNI host bridge are implemented. See [Phase 44](PHASE-44-EMBEDDED-X11-ADMISSION-GATE.md).
- FORGE-TS currently uses NDK 27.2.12479018; the candidate documents NDK 27.3.13750724. Keep the current toolchain until a complete pinned native build is integrated.

## P2 — XFCE4 PRoot Multi-Distro

Deliverables:
- Online RootFS catalog for Debian, Ubuntu and Alpine.
- Per-distro acquisition adapters and pinned release metadata.
- Download staging, checksum/signature verification and safe extraction.
- PRoot initialization and XFCE4 package setup.
- Per-distro Start/Stop/Remove and workspace configuration.

Initial release:
- Download Online only.

Deferred:
- Bring the ISO's remains Coming Soon. It requires local format detection, ISO inspection and conversion to a PRoot-ready root filesystem.

Acceptance:
- Each distro installs independently.
- Incomplete or invalid downloads are not registered as usable environments.
- Official or reproducibly verified acquisition sources are used.
- Unsupported installer ISO formats are not imported as RootFS.

## P3 — VM WorkStation

Reuse `RuntimeFoundationSupervisor` for initial device/resource inspection, but replace node-visibility assumptions with a real backend initialization capability result before offering KVM.

Deliverables:
- Independent VM Supervisor.
- Engine Adapter interface.
- KVM capability detection.
- QEMU TCG fallback where a supported Android build exists.
- VM disk and memory configuration.
- Isolated default virtual networking.
- VM lifecycle and diagnostics.

Acceptance:
- KVM is selected only when present and accessible.
- TCG is clearly labeled software emulation.
- Unsupported devices are not offered a false Start path.
- VM WorkStation does not depend on PRoot.

## P4 — Shared Services

Reconcile existing storage paths under `filesDir/forge-runtime/` before adding new directories. In particular, review the existing `linux-userland/` and `proot/` roots and preserve existing DataStore records; do not perform a blind migration.

Deliverables:
- App-managed storage and free-space policy.
- Shared Workspace with explicit mounts.
- Backup/export and cleanup.
- Resource Monitor.
- Diagnostic Logger with secret redaction.

Acceptance:
- RootFS and VM disks remain separate.
- Uninstall/data-removal implications are disclosed.
- Guest network exposure is disabled by default.

## P5 — RouterOS MikroTik

Status: Coming Soon; not part of initial implementation. The UI is an informational version catalog listing RouterOS 7.25.1 (Stable), 7.24.6 (Long-term), 7.26beta5 (Development), 7.25rc3 (Testing) and 6.49.23 (Legacy), with every entry marked Coming Soon. Download, import, installation and runtime controls are not exposed. The existing `RouterOsImageImporter` remains deferred and is not reachable from the UI.

Entry criteria:
- Official distribution/deployment method confirmed.
- CHR licensing and redistribution requirements reviewed.
- Runtime adapter and network isolation design approved.

## P6 — WorkStation Center Integration

Existing runtime foundation panels currently appear inside the Simulate screen; the dedicated WorkStation Center placement/navigation must be decided and integrated without duplicating those panels.

Deliverables:
- Four module cards.
- Runtime status and compatibility display.
- Installation method selection for PRoot.
- Session controls.
- Storage/resource overview.
- Coming Soon states for ISO import and RouterOS.

Acceptance:
- UI reflects real adapter capability and lifecycle.
- No unsupported control appears as available.
- User-facing documentation matches the implementation.

## Focused validation policy

Per milestone:
1. Inspect the touched code and relevant configuration once.
2. Implement the complete milestone slice.
3. Run one focused build or integration validation.
4. Record the exact result and any remaining blocker.

No repeated test loops without a specific failure hypothesis.
