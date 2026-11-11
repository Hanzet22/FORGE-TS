# FORGE-TS CodSpeed Benchmarks

## Purpose

FORGE-TS has a separate host C++ benchmark harness for CPU-side helpers shared
with the Android native PTY implementation. It currently measures:

- `BM_PtyDimensionsValidation`: the same terminal row/column validation used
  by the JNI PTY start and resize entry points.
- `BM_PtyArgvEnvPointerTable`: the same string-to-`char**` pointer-table
  preparation used before the native PTY launcher calls `execve`.

The shared implementation lives in
`app/src/main/cpp/pty_helpers.hpp`. The benchmark target is separate from the
Android app's CMake build and adds no benchmark dependency to the APK.

## Why simulation

The current workloads are CPU and memory operations. CodSpeed simulation is
appropriate for these deterministic helpers.

PTY process creation, `poll`, `read`, `write`, and `ioctl` are kernel/I/O
operations. They are deliberately not included in this simulation suite:
simulation does not measure elapsed syscall or I/O latency. A separate
walltime suite should be enabled only on a suitable CodSpeed Macro Runner.
An initial walltime attempt on a standard hosted runner built and executed the
benchmarks but did not produce valid profiler data, so it is not used as the
current CI measurement path.

## Run in GitHub Actions

The workflow is `.github/workflows/codspeed.yml`. It runs on pushes to
`main`, pull requests targeting `main`, and manual dispatch. Default-branch
runs establish the baseline used for pull-request comparisons.

The benchmark job uses GitHub OIDC with job-scoped `id-token: write`; no
long-lived CodSpeed token is committed to the repository.

## Build locally

On a Linux host with CMake, a C++17 compiler, and Git:

```sh
cmake -S benchmarks -B build/benchmarks \
  -DCODSPEED_MODE=simulation \
  -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build build/benchmarks --parallel 2
./build/benchmarks/forge_pty_bench
```

The CodSpeed C++ dependency is pinned to the upstream `v1.0.0` release commit
to avoid silently changing the benchmark toolchain.

## Measurement boundaries

These are host-side measurements of two portable native helper functions.
They do not represent Android device latency and do not measure the complete
Kotlin/JNI/Compose path, process creation, PTY I/O, Android scheduling, or
terminal rendering.

When a suitable device-side or CodSpeed Macro Runner becomes available, add
separate suites for PTY lifecycle and I/O. Keep those walltime results separate
from the CPU-simulation baseline.

## References

- [CodSpeed: GitHub Actions integration](https://codspeed.io/docs/integrations/ci/github-actions)
- [CodSpeed: GitHub Actions configuration and OIDC](https://codspeed.io/docs/integrations/ci/github-actions/configuration)
- [CodSpeed: C++ Google Benchmark integration](https://codspeed.io/docs/benchmarks/cpp)
- [CodSpeed: Walltime instrument](https://codspeed.io/docs/instruments/walltime)
- [CodSpeed C++ integration source](https://github.com/CodSpeedHQ/codspeed-cpp/tree/v1.0.0)
