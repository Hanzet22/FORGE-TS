# FORGE-TS WorkStation — Implementation Plan

**Status:** Planned; no implementation started  
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
- Add resource policy and bounded local diagnostic events.

Acceptance:
- UI can enumerate runtime adapters and display their real state.
- Unsupported capabilities are explicit.
- Adapter failures do not corrupt another runtime's state.

## P1 — Display Foundation and XFCE Native

Reuse the existing Linux-userland and Native runtime foundation code where it fits; do not interpret workspace readiness as an installed runtime.

Deliverables:
- Embedded X11 feasibility and licensing decision.
- In-app display host and backend interface.
- Input and resize bridge.
- Native Linux capability detector.
- Native runtime adapter and session lifecycle.

Acceptance:
- No mandatory external X11 companion application.
- Device without required Native Linux privileges is marked Unsupported.
- Native mode never silently falls back to PRoot.

Implementation gate:
- Do not commit to a specific embedded X11 codebase until its Android integration, ABI, package assumptions and license are reviewed.

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

Status: Coming Soon; not part of initial implementation. The existing `RouterOsImageImporter` only stages a user-selected CHR ARM64 RAW image after comparing a user-provided SHA-256; it does not authenticate the hash source or boot a guest. Keep its UI clearly marked Coming Soon or hidden until the RouterOS module is formally released.

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
