# FORGE-TS CodSpeed Benchmarks

## Purpose

FORGE-TS now has a separate host-Linux benchmark harness for the native PTY
operations used by the Android runtime. It measures three concrete workloads:

- `BM_PtySpawnAndExit`: create a PTY, launch a short-lived child process, and
  reap it.
- `BM_PtyEchoRoundTrip`: send and receive a small payload through a raw PTY.
- `BM_PtyResize`: apply terminal window-size changes with `TIOCSWINSZ`.

The harness uses CodSpeed's Google Benchmark compatibility layer and is kept
outside the Android app's CMake build. It does not add benchmark dependencies
to the APK.

## Why walltime

These workloads include kernel operations and process/PTY I/O. CodSpeed CPU
simulation does not represent elapsed system-call and I/O latency, so this
workflow uses the `walltime` instrument.

Walltime measures actual elapsed time. Shared GitHub-hosted runners can be
noisy; CodSpeed's dedicated Macro Runners are recommended when available for
high-confidence regression thresholds. Treat early results from standard
hosted runners as trend data, not as Android-device latency.

## Run in GitHub Actions

The workflow is `.github/workflows/codspeed.yml`. It runs on pushes to
`main`, pull requests targeting `main`, and manual dispatch. Default-branch
runs establish the baseline used for pull-request comparisons.

The benchmark job uses GitHub OIDC with job-scoped `id-token: write`; no
long-lived CodSpeed token is committed to the repository. The repository is
public, but OIDC is retained as the preferred authentication path.

## Build locally

On a Linux host with CMake, a C++17 compiler, Git, and the system `libutil`
development library:

```sh
cmake -S benchmarks -B build/benchmarks \
  -DCODSPEED_MODE=walltime \
  -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build build/benchmarks --parallel 2
./build/benchmarks/forge_pty_bench
```

The CodSpeed C++ dependency is pinned to the upstream `v1.0.0` release commit
to avoid silently changing the benchmark toolchain.

## Measurement boundaries

This is a Linux host benchmark of PTY/kernel primitives. It is not a physical
Android benchmark and does not measure the complete Kotlin/JNI/Compose path,
Android scheduler behavior, device-specific kernel behavior, or terminal
rendering. The benchmark intentionally measures PTY process startup, PTY
round-trip I/O, and resize syscalls separately.

When an Android-device benchmark runner becomes available, add a separate
device-side suite rather than mixing its measurements with these host runs.

## References

- [CodSpeed: GitHub Actions integration](https://codspeed.io/docs/integrations/ci/github-actions)
- [CodSpeed: GitHub Actions configuration and OIDC](https://codspeed.io/docs/integrations/ci/github-actions/configuration)
- [CodSpeed: C++ Google Benchmark integration](https://codspeed.io/docs/benchmarks/cpp)
- [CodSpeed: Walltime instrument](https://codspeed.io/docs/instruments/walltime)
- [CodSpeed C++ integration source](https://github.com/CodSpeedHQ/codspeed-cpp/tree/v1.0.0)
