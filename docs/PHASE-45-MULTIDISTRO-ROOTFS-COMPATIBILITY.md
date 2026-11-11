# Phase 45 — Multi-Distro Rootfs Compatibility Recovery

Status: source changes committed; Android build and physical-device distro launches are not yet verified.

## Problem addressed

Alpine, Debian, and Ubuntu rootfs installs could be rejected at the shell validation/startup boundary even when the archive contained a valid guest shell. Android's `File.canonicalFile`, `exists()`, and `isFile` follow symlinks using the host filesystem view. Linux rootfs symlinks, especially absolute guest paths and usr-merge paths such as `/bin -> /usr/bin`, must instead be interpreted relative to the guest root.

## Delivered

- Added `RootfsGuestPath`, a guest-root-aware symlink resolver.
- Absolute symlink targets are interpreted from the rootfs root, not Android's `/`.
- Relative symlink targets are resolved from the guest link's parent.
- Symlink traversal is bounded and rejects paths that escape the selected guest root.
- Rootfs provisioning now requires a readable executable shell resolved at `/bin/sh` or `/usr/bin/sh`.
- PRoot adapter inspection uses the same resolver before probing or launching a guest session.
- Existing per-distro workspace layout, download sources, SHA-256 pins, staging, extraction limits, and promotion behavior remain unchanged.

## Important boundary

This change fixes guest-path interpretation; it does not claim that the archives have been installed or that PRoot can execute them on a physical device. A successful install still requires archive verification, extraction, guest-shell validation, native PRoot/loader availability, and the disposable-child ptrace probe.

## Deferred

- Native X11 JNI host bridge work is sequenced after restoring the multi-distro PRoot baseline.
- No native X11 engine, Binder service, display adapter, or XFCE desktop session was added in this phase.

## Acceptance

One focused Android build should be run after the code batch. Physical-device acceptance still requires installing and starting Alpine, Debian, and Ubuntu separately, then running `id`, `uname -a`, and `cat /etc/os-release` in each guest.
