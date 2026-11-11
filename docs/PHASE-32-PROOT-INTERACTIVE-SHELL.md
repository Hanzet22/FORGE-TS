# Phase 32 — PRoot Interactive Shell Transport

## Scope

This phase adds an app-facing interactive command channel for the existing Debian, Ubuntu, and Alpine PRoot sessions. It does not change the rootfs sources, Supabase, Redis, VM engine, or RouterOS/CHR workstream.

## Delivered

- Extended `RuntimeExecutionAdapter` with bounded interactive input and output operations.
- The PRoot adapter now keeps the guest shell's standard input open and continuously drains standard output/error.
- Output is buffered per workspace with a 256 KiB rolling ceiling and appended to the existing session log.
- Input is limited to 16 KiB per write and rejects NUL bytes.
- `HybridRuntimeManager` exposes distribution-scoped terminal input/output methods. Both require a prepared workspace and pass through the registered PRoot adapter.
- Added a Simulation Center shell panel with Debian/Ubuntu/Alpine selection, Start, Stop, Send, Read output, and lifecycle refresh.

## Important transport limitation

The current channel is pipe-backed interactive shell I/O, **not a pseudo-terminal (PTY)**. It supports sending shell input and receiving output, but does not provide TTY semantics such as job control, terminal resize signals, canonical line discipline, or reliable operation of TTY-dependent programs. The UI states this limitation explicitly.

A later native PTY bridge must allocate and manage a real PTY master/slave pair, propagate terminal dimensions, handle EOF and process-group cleanup, and connect the PTY master to the Compose terminal view. Do not describe this phase as a completed PTY implementation.

## Safety and lifecycle

- Shell start remains gated by the existing runtime lifecycle, workspace containment, native engine/loader checks, and disposable PRoot probe.
- Terminal input is accepted only for an adapter-owned live session.
- Stop continues to use the adapter-owned process handle and existing graceful/forced termination path.
- Output buffering is bounded; session logs remain in the distro workspace.
- No guest rootfs or user data is automatically uploaded.

## Validation status

The Android Debug APK and CodeQL workflows were triggered for the implementation commits. Device-level PRoot execution, terminal behavior, and actual rootfs interaction still require validation on a supported Android device. A successful APK build alone does not establish that PRoot can execute on every device.
