# Phase 31 — PRoot RootFS Provisioning

Status: Debian, Ubuntu, and Alpine ARM64 gzip rootfs sources are pinned in the provisioner. Runtime installation and on-device PRoot execution still require device validation.

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
- SHA-256 is checked before any extraction. Alpine uses the exact pinned digest above; Ubuntu obtains the ARM64 digest from Canonical's official SHA256SUMS endpoint and compares the selected archive.
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
