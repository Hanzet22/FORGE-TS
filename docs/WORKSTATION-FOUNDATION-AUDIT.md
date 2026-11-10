# FORGE-TS WorkStation — Existing Foundation Audit

**Date:** 2026-11-10  
**Repository:** Hanzet22/FORGE-TS  
**Branch:** main  
**Purpose:** Reconcile the approved WorkStation architecture with code already present before adding new runtime implementations.

## Existing components

| Component | Current behavior | Not implemented |
|---|---|---|
| `HybridRuntimeManager` | DataStore-backed registry, app-private workspaces, resource snapshot, conservative recovery, state transitions | Native/PRoot/VM execution adapters; `requestStart` currently returns a refusal because no adapter exists |
| `RuntimeFoundationSupervisor` | Android API/ABI and RAM/storage inspection; checks `/dev/kvm` visibility/read/write flags; creates images/instances/logs folders | Actual KVM initialization or VM execution; node visibility is not proof of KVM usability |
| `LinuxUserlandFoundationSupervisor` | Prepares `forge-runtime/linux-userland/{rootfs,home,tmp,logs}` | Distro download, archive extraction, PRoot, XFCE, X11 |
| `RouterOsImageImporter` | Copies a user-selected CHR ARM64 RAW image to app-private storage and verifies its bytes against a supplied SHA-256 | Authenticating the hash source, booting or executing RouterOS |
| `RuntimeFoundationPanel` | Displays device/runtime foundation inspection | WorkStation runtime UI |
| `LinuxUserlandFoundationPanel` | Prepares a Linux userland workspace | Distro installation or desktop launch |
| `RouterOsImageImportPanel` | Exposes image staging inside Simulation Center | RouterOS guest runtime |
| `SimulationEnvironmentLauncher` | Displays persistent runtime registry and resource state; Start is guarded and does not launch a process | Actual runtime execution |

## Existing UI placement

- `SimulationScreen` currently includes `SimulationEnvironmentLauncher`, `RuntimeFoundationPanel`, `LinuxUserlandFoundationPanel` and `RouterOsImageImportPanel`.
- `ToolsScreen` currently contains DNS/Protocols, Repositories, Security, Diagnostics and OSINT Web; it does not contain a dedicated WorkStation Center.

## Storage reconciliation

Current app-private root is `filesDir/forge-runtime/`.

Existing paths include:
- `manager/registry`, `manager/state`, `manager/logs`
- `native-xfce/`
- `proot/`
- `linux-native/`
- `linux-userland/`
- `vm-workstation/`
- `routeros/`
- `images/`, `instances/`, `logs/`

The HybridRuntimeManager and LinuxUserlandFoundationSupervisor currently use separate Linux-userland/PRoot roots. Consolidation must be deliberate and preserve any existing data; do not perform a blind path migration.

## Persisted lifecycle vocabulary

The existing DataStore-backed enum is:
`NOT_INSTALLED`, `READY`, `STARTING`, `RUNNING`, `STOPPING`, `STOPPED`, `FAILED`, `UNAVAILABLE`, `RECOVERY_REQUIRED`.

This vocabulary is the compatibility baseline. Any additions or renames require a migration/compatibility plan.

## Required corrections before runtime implementation

1. Add adapter contracts to the existing HybridRuntimeManager; do not create a second runtime registry.
2. Keep workspace-prepared, runtime-installed and runtime-running as distinct concepts.
3. Treat `/dev/kvm` node inspection as a preliminary signal only.
4. Mark RouterOS image staging as Coming Soon or hide it until the RouterOS module is formally released.
5. Keep Native Linux capability-gated and never silently fall back to PRoot.
6. Decide the WorkStation Center navigation location; avoid duplicating the current Simulation panels.
7. Reconcile the app's `targetSdk = 58` in Gradle with the README's stated target SDK 36.

## Conclusion

The repository already contains a useful foundation layer. It is not an empty WorkStation project, but it also does not yet contain the actual Native Linux, PRoot, Embedded X11, RouterOS or VM execution runtimes.
