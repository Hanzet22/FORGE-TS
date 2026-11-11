# Phase 41 — PRoot Baseline Guest Integration

## Scope

This phase connects the existing PRoot launcher to the minimum Linux pseudo-filesystems and a predictable guest environment for the three managed ARM64 rootfs workspaces:

- Alpine
- Debian
- Ubuntu

## Implemented on main

- The launcher adds PRoot binds for host `/dev`, `/proc`, and `/sys` only when each source exists and is readable/traversable.
- The launcher does not bind Android shared storage, `/data`, `/system`, `/vendor`, app-private files, or Termux paths into the guest.
- Guest process environment now supplies `HOME=/root`, a standard Linux `PATH`, `TERM=xterm-256color`, and `LANG=C.UTF-8`.
- Interactive and one-shot execution continue to use the same command builder, so both paths receive identical baseline binds.

## Deliberately not included

- DNS/resolver mutation. Android Private DNS, VPNs, and per-network DNS require a separate resolver strategy; a hard-coded public resolver is not appropriate here.
- Distro package-manager bootstrap or package installation.
- Shared storage access.
- XFCE4, X11, audio, GPU acceleration, or VM integration.

## Acceptance status

Source integration is committed. This is not a claim that all three distributions boot successfully on a physical Android device. The remaining acceptance gate is a single focused Android build plus on-device PRoot shell verification for Alpine, Debian, and Ubuntu. The runtime must not be marked complete until each distro can start a shell and run basic commands.

## Upstream reference

The baseline `/dev`, `/proc`, and `/sys` bind pattern follows the minimal PRoot-Distro login model. FORGE-TS intentionally omits its broader Android/Termux host bindings to preserve app isolation.

- https://github.com/termux/proot-distro
