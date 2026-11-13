# Phase 31 — PRoot RootFS Provisioning

Status: Debian, Ubuntu, and Alpine ARM64 gzip rootfs sources are pinned and exposed through the PRoot terminal installation control. Runtime installation and on-device PRoot execution still require device validation.

## Verified upstream artifacts (2026-11-11)

| Distro | Exact artifact | SHA-256 | Format |
|---|---|---|---|
| Alpine 3.24.4 | `alpine-minirootfs-3.24.4-aarch64.tar.gz` | `4b14c18f7e90a063b61462ea1462a5732c154bf6adad594974fe6487cd98a8de` | gzip tar |
| Ubuntu Noble 24.04 | `ubuntu-noble-oci-arm64-root.tar.gz` | `c5a6798b238c14d4714ea0b2db40e83af7c86008e7e31c90ca44355dc36933db` | gzip tar |
| Debian Bookworm ARM64 | `rootfs.tar.gz` (OCI layer) | `f59e747f549eda321051637e213896378a19033924e1bfa7ae23a6572729c23c` | gzip tar; immutable artifact commit |

Sources:
- Alpine official release index: https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/
- Alpine archive checksum: https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.4-aarch64.tar.gz.sha256
- Ubuntu Canonical OCI index: https://partner-images.canonical.com/oci/noble/current/
- Ubuntu signed checksum list: https://partner-images.canonical.com/oci/noble/current/SHA256SUMS and `SHA256SUMS.gpg`
- Debian rootfs reproducibility and checksum catalog: https://docker.debian.net/
- Debian immutable OCI rootfs layer: https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/3b26c3995c7fae47d95ba678632a3520e97054f7/bookworm/oci/blobs/rootfs.tar.gz
- Debian pinned OCI manifest: https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/3b26c3995c7fae47d95ba678632a3520e97054f7/bookworm/oci/blobs/image-manifest.json
- Debian artifact commit and architecture metadata: https://github.com/debuerreotype/docker-debian-artifacts/tree/3b26c3995c7fae47d95ba678632a3520e97054f7/bookworm

## Implemented behavior

- Downloads remain outside the APK and use the existing app-private `filesDir/forge-runtime/proot/<distro>/` workspace.
- HTTPS-only downloads, same-origin HTTPS redirects, connection timeouts, and archive-size ceilings.
- SHA-256 is checked before any extraction. Alpine and Ubuntu use exact digests pinned in application source; the Ubuntu digest was cross-checked against Canonical's official `SHA256SUMS` list. Debian uses the ARM64 layer digest from its pinned OCI manifest.
- Tar extraction is staged; rejects absolute/traversal paths, duplicate paths, special device/socket entries, oversized entries, and writes through symlink parents.
- Hardlinks and symlinks are deferred until ordinary files have been extracted; link targets are constrained to the guest root.
- Validated staging rootfs is promoted while preserving an existing installation on failure.
- A local manifest is written only after rootfs validation. The registry becomes READY only after successful promotion.
- PRoot adapter still performs its own disposable guest process probe before a runtime can start.

## Explicit limitations

- SHA-256 is pinned in application source for all three archives. Canonical's detached `SHA256SUMS.gpg` signature is available, but the Android installer does not yet perform OpenPGP verification at runtime; this remains a supply-chain hardening item.
- Debian's OCI layer digest is anchored by a pinned upstream Git commit and manifest, but device download, extraction, and guest execution have not yet been verified on hardware.
- Archive digest verification does not make archive filesystem semantics safe; extraction checks remain mandatory.
- No scripts from the guest archive are executed. PRoot shares the Android host kernel and is not VM-grade isolation.
- CI green does not establish successful installation or runtime on a physical Android device.

## XFCE4 guest bootstrap

The optional Prepare / Repair XFCE4 action is user-triggered and requires the selected PRoot instance to be running. It does not run during rootfs download or installation.

| Guest distribution | Package index | XFCE4 packages |
| --- | --- | --- |
| Debian 12 Bookworm | apt-get update | xfce4 dbus-x11 via noninteractive APT with recommends disabled |
| Ubuntu 24.04 Noble | apt-get update | xfce4 dbus-x11 via noninteractive APT with recommends disabled |
| Alpine 3.24 | apk update | apk add --no-cache xfce4 dbus-x11 |

The bootstrap uses fixed argument lists, bounded command timeouts, and checks for both startxfce4 and dbus-launch before writing the guest marker .forge-xfce4.json. A failed update, install, or verification is reported to the UI and does not mark the desktop as prepared.

**Scope boundary:** package installation prepares the guest desktop environment only. It does not install or configure an Android X11 server, create a display session, or claim that XFCE4 is visibly launchable. Display integration remains a separate milestone.

## Local TigerVNC display bridge

The optional VNC server bootstrap is a separate user-triggered step after XFCE4 is command-verified.

| Guest | Server package | Password utility |
| --- | --- | --- |
| Debian 12 Bookworm ARM64 | `tigervnc-standalone-server` | `tigervnc-tools` (`tigervncpasswd`) |
| Ubuntu 24.04 Noble ARM64 | `tigervnc-standalone-server` | `tigervnc-tools` (`tigervncpasswd`) |
| Alpine 3.24 aarch64 | `tigervnc` | included in `tigervnc` (`vncpasswd`) |

Debian and Ubuntu expose the password utility as `tigervncpasswd`; Alpine exposes `vncpasswd`. The bootstrap detects either executable name. The session uses TigerVNC's virtual X server, not an Android X11 server. FORGE-TS generates a temporary eight-character VNC password, writes the VNC password file with mode 0600, and starts the guest X server with `-localhost`, `-SecurityTypes VncAuth`, and an explicit password file. The initial framebuffer is 960x540 at 16-bit depth and a 20 fps cap to limit load on mobile hardware. The endpoint is `127.0.0.1:5901`; no LAN listener is intended.

The in-app action opens a compatible external Android viewer through the standard `vnc://127.0.0.1:5901` URI. AVNC documents support for `vnc://` URIs, but is GPL-3.0; it is not bundled or linked as an app dependency. If no compatible viewer is installed, the user must install one separately. The temporary password is shown in the FORGE-TS screen and is not embedded in the URI.

**Lifecycle boundary:** the VNC and XFCE processes are started from the existing PRoot PTY shell so they remain children of the active PRoot session. Stopping the PRoot runtime ends that session. The desktop stop action requests termination of the saved process IDs and removes the guest password file. This implementation is CI/build-validated only; listener binding, VNC authentication, rendering, and process shutdown still require physical-device validation.
