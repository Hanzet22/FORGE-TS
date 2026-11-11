# FORGE-TS Native PTY Engine Architecture

Status: native PTY, ConnectBot VT terminal emulator, and event-driven output delivery are integrated; on-device behavioral acceptance remains pending.
Target: Android API 29+, ARM64 first, Kotlin + JNI + Android Bionic.

## 1. Objective

Replace the current pipe-backed PRoot interactive shell with a real pseudo-terminal (PTY). A PTY provides terminal semantics required by shells and interactive programs: isatty(), line discipline, foreground process groups, terminal-generated signals, and terminal window dimensions.

The interactive PRoot adapter now launches its long-lived guest shell through the native PTY bridge. One-shot commands and the disposable ptrace probe still use ProcessBuilder. The Compose terminal view forwards PTY output bytes into the emulator, keyboard bytes back to the PTY, and terminal size changes to the runtime manager. PTY output availability is signaled through a conflated Channel so the UI suspends while idle rather than waking on a fixed polling interval. On-device behavioral acceptance remains pending.

## 2. Evidence and platform baseline

- Android Bionic declares openpty() and forkpty() in <pty.h>; both are available from API level 23. FORGE-TS minSdk 29 is above that baseline.
- AOSP Terminal demonstrates PTY allocation, session creation, controlling-terminal setup, stdio attachment, and master-side I/O.
- Android Bionic implements forkpty() using openpty(), fork(), and login_tty().
- Existing libproot.so and libproot_loader.so files are executable ELF payloads, not JNI libraries. The PTY JNI library must have a distinct name such as libforge_pty.so.

## 3. Layer boundaries

1. Compose terminal UI — renders terminal cells, accepts key events, reports viewport rows/columns.
2. Terminal emulator — ConnectBot termlib 0.0.12 parses VT escape sequences and maintains the screen, cursor, colors, and scrollback.
3. Kotlin PTY session facade — owns lifecycle, coroutine dispatch, byte-array boundaries, and session identity.
4. JNI PTY engine — owns native handles, master FD, child PID, polling, resize, signals, and cleanup.
5. PRoot launcher — runs the existing packaged PRoot binary with the selected rootfs and guest shell.

The terminal emulator is not the PTY process manager. Keep these responsibilities separate. Native PTY integration is present; the emulator dependency remains deferred pending compatibility review.

## 4. Native session contract

- nativeStart(argv, envp, cwd, rows, cols): Long — allocate PTY, spawn child, return opaque session handle or a negative errno-style result.
- nativeRead(handle, buffer, timeoutMs): Int — poll-backed master read; positive byte count, zero timeout/no data/closed-slave indication, negative errno-style failure.
- nativeWrite(handle, bytes, offset, length): Int — write to master; Kotlin loops over partial writes and treats zero as a non-progress condition.
- nativeResize(handle, rows, cols): Boolean — apply TIOCSWINSZ to master; terminal driver delivers window-size change to the foreground process group.
- nativeWait(handle): Int — non-blocking waitpid(WNOHANG); zero means running, positive values encode an exited/signaled child, and negative values are errno-style failures.
- nativeSignal(handle, signal): Boolean — signal the child process group under a narrow allowlist.
- nativeClose(handle, graceful): Boolean — close master, request graceful shutdown, reap child, and release all native resources.

Validate array bounds, dimensions, environment entries, command length, and handle state at the JNI boundary. Never accept arbitrary native pointers from Kotlin.

## 5. Spawn and I/O lifecycle

- Prefer Bionic forkpty() with a winsize and explicit terminal settings, or openpty() + fork() + setsid() + TIOCSCTTY.
- Parent retains the PTY master; child receives the slave as stdin/stdout/stderr.
- In the child, perform only minimal native setup before execve(). Do not call Kotlin/Java, acquire application locks, or perform heap-heavy work after fork.
- Prepare argv/envp before spawning. Use an explicit executable path and execve(); avoid shell-string concatenation.
- Set the child working directory before exec. Report failures through a close-on-exec error pipe or equivalent reliable handshake.
- Keep PTY reads/writes off the main thread. Use poll()/ppoll() with bounded waits and handle EINTR, EAGAIN, partial writes, EIO-on-closed-slave, and EOF.
- Apply terminal resize through TIOCSWINSZ, not only a UI-side dimension update.
- Reap the child with waitpid(); avoid zombie processes and stale handles. Cleanup must be idempotent.

## 6. PRoot integration

ProotExecutionAdapter now passes the validated PRoot launcher command, loader environment, tmp paths, and selected rootfs to NativePtyEngine.nativeStart(). forkpty() attaches the PTY slave to the child process standard streams, so PRoot and the guest shell inherit terminal descriptors. The adapter retains ProcessBuilder for bounded one-shot commands and the ptrace probe.

Do not change rootfs provisioning, Supabase, Redis, account flows, or MikroTik/CHR as part of this PTY work.

## 7. Compatibility and rollout

- Keep the current pipe adapter as fallback until native PTY startup and lifecycle are verified.
- Prefer capability detection over assuming that a packaged native library exists.
- The UI must distinguish pipe-backed interactive shell from PTY terminal.
- Do not switch the default adapter until the native library is built for the packaged ABI and runtime checks confirm isatty(0) in the guest.

## 8. Acceptance criteria

1. Guest shell reports stdin as a TTY.
2. Interactive shell editing and prompt behavior work.
3. Ctrl+C reaches the foreground process group.
4. Ctrl+D reaches the terminal input discipline.
5. Resize updates guest terminal dimensions and triggers expected window-change behavior.
6. A TTY-dependent interactive program can start and exit normally.
7. Closing the app session closes descriptors and reaps the child.
8. A dead child is detected and cannot leave a stale RUNNING state.
9. Existing non-interactive PRoot command execution continues to work.

## 9. Primary references

- Android Bionic pty.h: https://android.googlesource.com/platform/bionic/+/main/libc/include/pty.h
- Android Bionic PTY implementation: https://android.googlesource.com/platform/bionic/+/main/libc/bionic/pty.cpp
- AOSP Terminal forkpty.cpp: https://android.googlesource.com/platform/packages/apps/Terminal/+/6a142b6/jni/forkpty.cpp
- Android Bionic API status: https://android.googlesource.com/platform/bionic/+/main/docs/status.md
- Linux PTY overview: https://man7.org/linux/man-pages/man7/pty.7.html
- Linux poll(2): https://man7.org/linux/man-pages/man2/poll.2.html
- Linux TIOCSWINSZ: https://man7.org/linux/man-pages/man2/TIOCSWINSZ.2const.html
- ConnectBot terminal emulator release 0.0.12: https://github.com/connectbot/termlib/tree/0.0.12
- Android JNI guidance: https://developer.android.com/ndk/guides/jni-tips

## 10. Terminal emulator dependency assessment

ConnectBot termlib is an emulator/display component, not a PTY or process supervisor. Its documented data flow is PTY bytes into TerminalEmulator and keyboard bytes back through a callback, which fits the layer separation above.

The repository declares Apache-2.0 and includes libvterm under MIT. The pinned org.connectbot:termlib:0.0.12 artifact declares Kotlin 2.2.21 and Compose BOM 2025.12.00. FORGE-TS uses Kotlin 2.2.21 and aligns its Compose BOM to 2025.12.00. The terminal view is connected to the native PTY using byte-preserving input/output callbacks and the PTY resize callback. The adapter reader signals new buffered output through a conflated Channel, and the UI awaits data instead of polling.

- termlib repository and license: https://github.com/connectbot/termlib
- termlib API documentation: https://termlib.connectbot.org/main
- Maven Central artifact metadata: https://central.sonatype.com/artifact/org.connectbot/termlib/0.0.40
