# Phase 33 — Native PTY Terminal Emulator Integration

## Scope

Connect the existing Android Bionic PTY transport to a real VT terminal emulator for Debian, Ubuntu, and Alpine PRoot sessions. Keep PRoot process ownership, terminal parsing, Compose rendering, and byte transport as separate layers.

## Delivered in this phase

- Pinned ConnectBot termlib 0.0.12, using libvterm for VT/ANSI parsing and Compose rendering.
- Replaced the old text-only shell output panel with the terminal emulator view.
- Forwarded PTY output as raw bytes into the emulator so split UTF-8 sequences are not decoded and corrupted between reads.
- Forwarded terminal keyboard bytes to the PTY without an intermediate lossy text conversion.
- Connected emulator dimension changes to the runtime manager and native TIOCSWINSZ path.
- Kept the existing PRoot lifecycle, workspace containment checks, disposable ptrace probe, and native session ownership.

## Runtime behavior

- The terminal panel polls the adapter's bounded output buffer while the selected PRoot session is RUNNING.
- PTY output is capped at 256 KiB between reads; session logs continue to be written under the selected workspace.
- Keyboard writes remain capped at 16 KiB per input operation and reject NUL bytes.
- The terminal emulator maintains VT screen state, cursor, ANSI colors, and scrollback; it does not launch or supervise processes.
- The native PTY engine remains responsible for process-group signals, window-size ioctl, child reaping, and descriptor cleanup.

## Dependency compatibility

- ConnectBot termlib 0.0.12: Kotlin 2.2.21, Compose BOM 2025.12.00, Apache-2.0.
- The app uses Kotlin 2.2.21 and aligns Compose BOM to 2025.12.00.
- The terminal library bundles libvterm, which is MIT licensed.

## Acceptance still pending

Device-level verification is still required for interactive shell editing, Ctrl+C/Ctrl+D, resize propagation, ANSI applications, Unicode input/output, child exit detection, and clean shutdown. A successful Android APK build does not establish those device behaviors.
