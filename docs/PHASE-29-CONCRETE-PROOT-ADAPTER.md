# Phase 29 — Concrete Android PRoot Execution Adapter

**Status:** Adapter, guarded lifecycle integration, native PTY path, and ARM64 PRoot source-build/packaging workflow are implemented. Successful artifact packaging and on-device distro startup are not yet verified.

## Implemented

- A default ProotExecutionAdapter is registered with HybridRuntimeManager for XFCE_PROOT only.
- Debian, Ubuntu and Alpine instances remain isolated under their existing filesDir/forge-runtime/proot/<distro>/ workspaces.
- Adapter path resolution requires the exact managed instance directory, a rootfs/ child, a readable guest /bin/sh, and engine files in Android's installed nativeLibraryDir.
- Expected engine entrypoints are libproot.so and libproot_loader.so; the loader is supplied through PROOT_LOADER, and LD_LIBRARY_PATH points to the installed native library directory.
- Adapter inspection runs a bounded disposable guest command through the same PRoot launch path used by the session. A successful exit is required before Start can proceed.
- Start owns the launched process and only confirms active state when the process remains alive after startup.
- Stop is routed through the adapter, requests graceful termination, verifies launcher exit after forced termination if needed, and persists STOPPING followed by STOPPED or FAILED in the existing DataStore registry.
- PROOT_TMP_DIR points to the selected instance's app-private tmp/ directory for PRoot internals; guest TMPDIR points to /tmp. Guest HOME, USER, LOGNAME, SHELL, PATH, TERM and LANG are set explicitly. The launcher conditionally binds readable host /dev, /proc and /sys only; it does not expose Android shared storage or broad host app paths.
- Android Gradle packaging extracts native runtime payloads into the installed native-library directory.
- No new DataStore keys, database, migration, Supabase project, Redis resource, rootfs download or archive extraction was added.

## Runtime requirements not yet satisfied

The ARM64 native build and APK packaging workflow is now present, including the separate PRoot loader and payload checksum/license handoff. However, no successful native workflow run, APK containing the payload, or physical-device shell session has been confirmed yet. Consequently:

- Adapter registration means only that the adapter implementation exists; it does not mean the engine is installed.
- Adapter inspection must return unavailable until all required artifacts are present and the disposable-child probe succeeds.
- PRoot ptrace is not reported as available by foundation inspection; the adapter probe is the gate used by Start.
- Rootfs provisioning code supports Debian, Ubuntu and Alpine, but each installed rootfs and its interactive shell still requires a successful runtime start before that distro can be marked usable.
- The native PTY implementation is present, but no successful Debian, Ubuntu or Alpine interactive shell has been verified. XFCE, X11, audio, background services and desktop sessions remain unclaimed.

## Android packaging and compatibility gate

A generic executable copied into app-writable files/ is not an acceptable PRoot deployment strategy on modern Android. The native engine and loader must be reviewed for provenance, Android target-SDK compatibility, ABI, SELinux/W^X behavior and syscall/seccomp handling. They must be installed in the app's executable native-library location, with their complete native dependency closure. A successful file-existence check alone is insufficient; the disposable guest-child probe is required.

The adapter's current expected filenames are a packaging contract, not evidence that suitable binaries are already present. Phase 30 records the pinned source decision, archive digest, ARM64-first packaging layout, loader mapping, temporary-directory contract and remaining dependency review.

## Validation boundary

- Source changes were committed to main.
- No APK build, compilation, device run, ptrace test, rootfs download or prolonged test sequence was performed.
- A green build/runtime result must not be inferred from this source commit.
