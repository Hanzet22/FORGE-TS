# FORGE-TS WorkStation — Final Architecture Specification

**Document status:** Approved architecture baseline  
**Specification:** v1.0  
**Implementation status:** Not started  
**Repository:** `Hanzet22/FORGE-TS`  
**Branch:** `main`  
**Date:** 2026-11-10

## 1. Purpose

Define the architecture for adding workstation and virtual-environment capabilities to the existing native Android application. This document records the approved product direction and evidence-based platform constraints. It does not claim that any workstation runtime is implemented or compatible with every Android device.

## 2. Existing application baseline

Repository inspection found:

- Kotlin, Jetpack Compose and Material 3 Android application.
- Application ID: `com.HG.Forge.Ts`.
- `minSdk = 29` (Android 10).
- `compileSdk = 36`.
- Current `targetSdk = 58` in `app/build.gradle.kts`; the README currently describes target SDK 36, so this discrepancy must be reconciled before release work.
- Java/Kotlin bytecode target 17.
- Existing dependencies include AndroidX, Compose, Room, DataStore, WorkManager and Ktor.
- No NDK/CMake, X11 server, PRoot binary or QEMU engine is currently declared in the inspected app Gradle file.

The implementation must preserve the existing app identity and Android 10 minimum unless a separately approved platform change is made. Runtime compatibility must be detected on-device; Android version alone is not sufficient.

## 3. Approved architecture decisions

| Area | Decision |
|---|---|
| XFCE Native | Native Linux Runtime, separate from PRoot |
| XFCE4 PRoot | PRoot runtime with Debian, Ubuntu and Alpine |
| PRoot initial acquisition | Download Online |
| PRoot local ISO import | Bring the ISO's — Coming Soon |
| Desktop display | Embedded X11 target, hosted/integrated in FORGE-TS |
| RouterOS MikroTik | Coming Soon |
| VM WorkStation | Independent VM engine, integrated through an adapter |
| VM backend strategy | KVM when genuinely available; QEMU TCG software emulation fallback where supported |
| Storage | App-managed storage |
| Orchestration | Central Hybrid Manager |
| Backend | Keep current Supabase and Redis infrastructure unchanged for this phase |

## 4. Component architecture

```text
FORGE-TS Android Application
|
+-- WorkStation Center UI
|
+-- Hybrid Manager
|   +-- Runtime Registry
|   +-- Capability Detector
|   +-- Lifecycle Controller
|   +-- Resource Policy
|   +-- Adapter Registry
|
+-- Display Layer
|   +-- Android Display Host
|   +-- Display Adapter
|   +-- Embedded X11-compatible Backend
|
+-- Runtime Layer
|   +-- XFCE Native Adapter
|   +-- XFCE4 PRoot Adapter
|   |   +-- Debian
|   |   +-- Ubuntu
|   |   +-- Alpine
|   +-- RouterOS Adapter (Coming Soon)
|   +-- VM WorkStation
|       +-- VM Supervisor
|       +-- KVM Adapter
|       +-- QEMU TCG Adapter
|
+-- Shared Services
    +-- Storage Manager
    +-- Workspace Manager
    +-- Network Manager
    +-- Resource Monitor
    +-- Diagnostic Logger
```

The Hybrid Manager coordinates adapters and reports their actual states. It does not embed hypervisor or distro-specific logic in the UI.

## 5. Display architecture — Embedded X11

The product target is an in-app display experience; users should not be required to install a separate X11 companion app as a mandatory product dependency.

The selected display contract is:

```text
Android Display Host
    -> Display Adapter
        -> Embedded X11-compatible Backend
            -> Runtime Display Session
                -> XFCE Desktop
```

Responsibilities:

- Create and stop display sessions.
- Render desktop windows inside the Android application.
- Route touch, pointer, keyboard and resize events.
- Expose a stable display-session interface to Native and PRoot adapters.
- Keep the display backend replaceable.

**Implementation constraint:** upstream Termux:X11 is a separate Android application plus supporting packages. It is useful technical reference, but it is not itself an embedded in-process backend. Any reuse or port must be checked for architecture fit, package/signing assumptions and license obligations. The embedded backend implementation remains a feasibility item until its source, license, Android integration and rendering path are confirmed. Do not silently substitute an external companion application.

## 6. XFCE Native

**Status:** Planned.

XFCE Native uses a Native Linux Runtime adapter and is not a PRoot alias.

Core responsibilities:

- Native runtime capability detection.
- Linux userspace/session preparation.
- Filesystem and process lifecycle.
- Display, input and audio adapters.
- Start/stop and error reporting.

Android normally isolates each application under its own UID and kernel-enforced sandbox; SELinux adds platform access controls. A conventional root/chroot-style Linux environment therefore cannot be assumed to work on an ordinary unrooted device. Native mode must check the required runtime privileges and kernel/platform capabilities before enabling installation or launch.

If requirements are absent, report `UNSUPPORTED` with a clear explanation. Never silently fall back to PRoot while continuing to label the session Native.

## 7. XFCE4 PRoot

**Status:** Planned.

Supported distributions:

- Debian
- Ubuntu
- Alpine Linux

Each distro receives a separate root filesystem, configuration, lifecycle and workspace mapping. The initial release supports only the online acquisition path.

### 7.1 Download Online — initial release

Flow:

1. Select Debian, Ubuntu or Alpine.
2. Select a supported release and CPU architecture.
3. Resolve the distro-specific RootFS acquisition source.
4. Download to a temporary staging directory.
5. Verify publisher-provided checksum/signature metadata where available.
6. Validate archive structure and expected architecture.
7. Extract with safe path handling and ownership/permission preservation where supported.
8. Initialize the PRoot environment.
9. Install XFCE4 and required display/session packages.
10. Register the environment and expose Start/Stop controls.

Source policy:

- Prefer official distribution sources and reproducible package metadata.
- Alpine publishes architecture-specific `alpine-minirootfs` archives and checksum/signature files.
- Debian cloud images are VM-oriented disk images; a `.tar.xz` cloud artifact may contain a disk image rather than a clean PRoot rootfs. Do not treat it as a PRoot archive without inspecting its actual contents.
- Ubuntu Base/WSL/cloud artifacts have different purposes and may include kernel or WSL-specific assumptions. Do not select one as the PRoot default until its archive layout and intended use are verified.
- Debian and Ubuntu acquisition adapters must use a verified RootFS construction/distribution path. Do not use random third-party prebuilt archives as trusted defaults.
- Pin release metadata and checksums; do not hard-code a floating `latest` URL without resolving and recording the exact artifact metadata.

### 7.2 Bring the ISO's — Coming Soon

This feature is explicitly deferred.

Future scope:

- Android Storage Access Framework file selection.
- ISO and archive format detection.
- ISO filesystem inspection and extraction.
- RootFS conversion only for supported installer layouts.
- Architecture and integrity checks.
- Clear incompatibility reporting.
- PRoot initialization.

A standard installer ISO is not automatically a PRoot-ready RootFS. This feature must not block the initial online installer.

### 7.3 PRoot execution boundary

PRoot is a user-space implementation of chroot-like filesystem views, bind mounts and binary-format handling, relying on ptrace/system-call translation. It does not boot a guest kernel. It therefore shares the Android/Linux host kernel and must not be described as VM-grade or kernel-level isolation. Cross-architecture execution through QEMU user-mode is a separate optional capability and is not required for the first ARM64-oriented implementation.

## 8. VM WorkStation

**Status:** Planned; independent engine.

The VM subsystem has its own supervisor and disk/network lifecycle. It is not implemented inside PRoot.

Components:

- VM Supervisor.
- Engine Adapter interface.
- Virtual CPU and memory configuration.
- Virtual disk manager.
- Virtual network adapter.
- Display integration.
- VM lifecycle and diagnostics.
- Snapshot/backup support when the selected backend supports it.

Backend selection:

1. Detect CPU architecture, kernel virtualization support, device nodes and application access.
2. Use KVM only when the device kernel exposes a compatible KVM interface and the app is permitted to use it.
3. Where a supported QEMU build exists but hardware acceleration is unavailable, offer QEMU TCG software emulation with a clear performance warning.
4. Otherwise report `UNSUPPORTED`.

ARM64 hardware alone does not prove that KVM is enabled or accessible to the app. QEMU documents TCG as software emulation and KVM as one of several optional accelerators. The Android-hosted QEMU build, native ABI packaging, device access and lifecycle integration remain implementation dependencies.

## 9. RouterOS MikroTik

**Status:** Coming Soon.

Future adapter scope:

- RouterOS/CHR environment integration, subject to official distribution and licensing requirements.
- Network configuration learning.
- Interface and IP configuration.
- Routing and firewall concepts.
- Network topology integration.

No RouterOS image is bundled, downloaded or executed in the initial WorkStation release. Licensing and supported deployment method must be reviewed before implementation.

## 10. App-managed storage

All workstation environment storage is managed by FORGE-TS.

Proposed layout:

```text
workstation/
+-- system/
|   +-- native/
|   +-- proot/
|   |   +-- debian/
|   |   +-- ubuntu/
|   |   +-- alpine/
|   +-- vm/
+-- workspace/
|   +-- shared/
|   +-- projects/
|   +-- exports/
+-- configs/
|   +-- native/
|   +-- proot/
|   +-- vm/
+-- backups/
+-- logs/
```

Policy:

- Internal app storage for metadata and sensitive configuration.
- App-specific external storage for large RootFS and VM disk data where available.
- Shared Workspace is a controlled file-exchange directory, not a shared root filesystem.
- Use Android's Storage Access Framework for explicit user-selected import/export operations.
- Explain that app-specific files can be removed when the app is uninstalled; provide backup/export before destructive removal.
- Enforce free-space checks, temporary download staging and cleanup of incomplete downloads.

## 11. Shared services

| Service | Responsibility |
|---|---|
| Storage Manager | Environment directories, quota and cleanup |
| Workspace Manager | Explicitly mounted/shared user files |
| Network Manager | Per-runtime network policy and status |
| Resource Monitor | CPU, memory and storage reporting |
| Session Manager | Runtime/display session lifecycle |
| Diagnostic Logger | Bounded local logs with secrets redacted |

Guest network forwarding is disabled by default. Do not expose guest management interfaces to the LAN by default. Credentials must not be embedded in the APK or copied into diagnostic logs.

## 12. Lifecycle contract

All adapters expose a common state vocabulary:

- `NOT_INSTALLED`
- `INSTALLING`
- `STOPPED`
- `STARTING`
- `RUNNING`
- `STOPPING`
- `ERROR`
- `UNSUPPORTED`

The Hybrid Manager may show `RUNNING` only after the adapter confirms the session is active. A submitted Start command is not proof of a successful start.

## 13. Dependency policy

No runtime dependency is added merely because it is available.

- Keep Native, PRoot and VM adapter dependencies independent.
- Evaluate Android ABI support, minimum OS, target SDK behavior, native build requirements and license obligations before selecting libraries.
- Keep X11 and hypervisor implementations behind replaceable interfaces.
- Record exact versions and source commits during implementation.
- The current app Gradle configuration does not yet declare PRoot, QEMU, NDK/CMake or an embedded X11 backend.

## 14. Backend policy

This WorkStation phase does not require a new Supabase project, database migration, Edge Function or Redis instance. Runtime metadata should be local unless a future, separately approved requirement demonstrates a need for cloud synchronization. Existing backend services remain unchanged.

## 15. Acceptance criteria

A runtime module is complete only when:

1. Compatibility detection accurately enables or disables it.
2. Installation and initialization complete from supported sources.
3. Lifecycle states reflect actual runtime conditions.
4. Storage boundaries remain independent.
5. Display and input work where the module requires a GUI.
6. Resource usage and storage requirements are visible.
7. Errors are diagnosable without leaking secrets.
8. Stop, cleanup, backup and recovery behavior are documented.
9. A focused build/integration validation succeeds for the milestone.

## 16. Research references

### Platform and upstream documentation

- [Android Application Sandbox](https://source.android.com/docs/security/app-sandbox)
- [Android SELinux](https://source.android.com/docs/security/features/selinux)
- [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific)
- [Android Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [PRoot upstream manual](https://raw.githubusercontent.com/proot-me/PRoot/master/doc/proot/manual.rst)
- [QEMU system emulator documentation](https://www.qemu.org/docs/master/system/qemu-manpage.html)
- [Termux:X11 upstream](https://github.com/termux/termux-x11)
- [Debian official cloud images](https://cloud.debian.org/images/cloud/)
- [Ubuntu cloud images](https://cloud-images.ubuntu.com/)
- [Alpine Linux downloads](https://alpinelinux.org/downloads/)

### Academic references

- Ahlgren et al., *Userland Containers for Mobile Systems* (2023), DOI: [10.1145/3572864.3581588](https://doi.org/10.1145/3572864.3581588).
- Ahlgren et al., *PARAVIRT: Userland Containers for Mobile Systems* (2025), DOI: [10.1109/MobileCloud66020.2025.00015](https://doi.org/10.1109/MobileCloud66020.2025.00015).
- Park et al., *Virtualizing Graphics Architecture of Android Mobile Platforms in KVM/ARM Environment* (2017), DOI: [10.1587/TRANSINF.2016EDP7435](https://doi.org/10.1587/TRANSINF.2016EDP7435).

## 17. Final status

**Architecture:** Approved baseline  
**Specification:** v1.0  
**Implementation:** Not started  
**Repository code changes:** None in this specification commit  
**Database changes:** None  
**New infrastructure:** None

Any change to these architecture decisions requires a documented revision before implementation.
