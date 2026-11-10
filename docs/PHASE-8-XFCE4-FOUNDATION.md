# Phase 8 — XFCE4 / Linux Userland Foundation

**Status:** Foundation increment implemented. No Linux distribution is installed or executed.

## Purpose

Prepare a private, auditable workspace for a future Linux userland and XFCE4 desktop without coupling it to RouterOS CHR licensing or guest boot.

## Implemented

- Adds `files/forge-runtime/linux-userland/` under Android app-private storage.
- Prepares separate `rootfs`, `home`, `tmp`, and `logs` directories.
- Checks canonical-path containment and read/write access before reporting readiness.
- Runs filesystem work on the IO dispatcher from the Simulation Center.
- Reports available app storage and explicit limitations.

## Deliberately not implemented

- No distribution/rootfs image is bundled, downloaded, unpacked, mounted, or executed.
- No package manager, shell, terminal, or arbitrary host process execution.
- No XFCE4 process, X11/VNC/display bridge, or background service.
- No QEMU process, RouterOS guest boot, virtual network, or device passthrough.
- No assumption that an ARM64 ABI provides KVM/AVF support.

## Next engineering gates

1. Select a userland distribution and review the licenses of the distribution and every shipped package.
2. Prefer an official source and independently verifiable checksum; keep distribution downloads separate from CHR assets.
3. Implement a bounded archive importer with compressed/uncompressed size caps, path traversal rejection, symlink/hardlink policy, atomic staging, and cleanup.
4. Define process ownership, cancellation, lifecycle recovery, and resource limits before starting any userland process.
5. Select and validate a display bridge separately; do not equate a rendered desktop with guest isolation.
6. Test on the target Android device before marking the desktop lifecycle ready.

## Security boundary

The Linux userland is a separate workspace, not a trusted Android host shell. Future commands must be executed only inside a deliberately constrained userland process boundary. User-controlled command text must never be concatenated into Android host shell commands.

## Completion criteria

Phase 8 is not complete until a supported userland can be provisioned, started, displayed, stopped, and recovered reliably on a supported device, with verified storage and process boundaries. This foundation increment only prepares the private workspace.
