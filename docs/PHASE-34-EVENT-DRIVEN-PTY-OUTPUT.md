# Phase 34 — Event-Driven PTY Output and WallTime Benchmarking

## Scope

Remove the fixed-interval terminal output polling loop and measure the existing native PTY setup benchmarks in both CodSpeed Simulation and WallTime modes.

## Delivered

- Added a conflated coroutine Channel to each native PTY session as an output-availability signal.
- The PTY reader continues to own native reads and the bounded 256 KiB output buffer.
- The reader signals the Channel after appending each positive-length read.
- The terminal output reader suspends until a notification arrives, then drains the buffered bytes.
- The Channel closes when the PTY reader exits, allowing the UI collector to observe session termination without a timer loop.
- Removed the terminal UI's fixed 60 ms polling delay.
- Updated the CodSpeed GitHub Action to run both simulation and walltime in one action invocation.

## Design notes

The Channel carries notifications, not terminal bytes. This preserves the existing synchronized byte buffer and avoids creating an unbounded queue of output chunks. Conflation is intentional: multiple read notifications may collapse into one because the consumer drains all currently buffered bytes on wake-up.

The 256 KiB cap remains a bounded-output policy. If the UI falls behind by more than the cap, oldest buffered bytes are discarded; this is documented behavior and should be reconsidered only with real device measurements.

## Measurement scope

CodSpeed currently measures native helper benchmarks for PTY dimension validation and argv/env pointer-table preparation. Simulation provides CPU-oriented measurements; WallTime adds elapsed-time measurement for the system-call/runner overhead flagged by previous CodSpeed results. Neither mode measures Android Compose rendering, JNI device latency, or full PRoot session behavior.

## Acceptance still pending

Physical-device verification remains necessary for terminal input/output, shell job control, resize propagation, Unicode, foreground/background transitions, and session cleanup. The Android APK build is a compile/package check, not proof of these runtime behaviors.
