# FORGE-TS WorkStation — Existing Foundation Audit

**Audit date:** 2026-11-16; **current runtime table updated:** 2026-11-18
**Repository:** Hanzet22/FORGE-TS  
**Branch:** main  
**Purpose:** Reconcile the approved WorkStation architecture with code already present before adding new runtime implementations.

## Existing components

> This audit predates the runtime work already present in `main`. The following status supersedes the implementation claims below; older architectural notes are retained as historical context.

### Current runtime status

| Area | Current behavior | Remaining platform gap |
|---|---|---|
| PRoot | ARM64 Debian 12, Ubuntu 24.04 and Alpine rootfs provisioning, pinned archive verification, process adapter, private DNS handling, XFCE4/TigerVNC bootstrap, and user-supplied HTTPS package mirror configuration are implemented. | Requires device PRoot capability. There is no built-in country mirror directory; users must supply a compatible mirror URL. |
| VNC | TigerVNC and noVNC can be installed in PRoot; the server and WebSocket bridge bind to loopback and the app opens noVNC in its own WebView. | Requires package installation and a running PRoot XFCE session. |
| Embedded X11 | Platform gate and capability reporting exist. | Native X11 library/host bridge is not packaged or implemented. |
| Linux native / VMStation | Registry workspaces and KVM capability probes exist. | No native Linux desktop or VM execution engine is registered in the Android adapter registry. |

Do not describe the gaps above as active runtimes in UI or release notes.

| Component | Current behavior | Not implemented |
|---|---|---|
| `HybridRuntimeManager` | DataStore-backed registry, app-private workspaces, resource snapshot, conservative recovery, state transitions and a PRoot adapter path | Native Linux and VM execution adapters |
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

## Remaining gaps and guardrails

1. Do not add a parallel runtime manager. Extend the current HybridRuntimeManager and keep the PRoot adapter path intact.
2. Keep workspace-prepared, runtime-installed and runtime-running as separate concepts.
3. Treat `/dev/kvm` visibility as preliminary inspection only; there is no registered VM engine.
4. Keep RouterOS guest execution marked Coming Soon; image staging is not boot or execution.
5. Keep Native Linux capability-gated and never silently fall back to PRoot.
6. Embedded X11 still needs a compatible native backend and host bridge. The VNC/noVNC route is distinct from X11.
7. Verify the current `app/build.gradle.kts` before updating SDK, ABI or NDK documentation.

## Conclusion

This audit records the earlier foundation review. Since that review, ARM64 PRoot provisioning/execution and the XFCE4/TigerVNC/noVNC path have been added. The current limit table at the top of this document and [the development handoff](DEVELOPMENT-HANDOFF.md) supersede this older conclusion: Native Linux, embedded X11, RouterOS guest execution and VM engines are still unavailable.
