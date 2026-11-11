# FORGE-TS CodSpeed Benchmarks

## Purpose

FORGE-TS keeps two host-side benchmark suites. Only the native C++ suite is
currently uploaded to CodSpeed in GitHub Actions. Neither suite adds a
dependency to the Android APK.

### Native PTY helpers (C++, CPU simulation)

Location: `benchmarks/` (Google Benchmark through the CodSpeed C++ integration).
The benchmarks exercise the portable helpers in
`app/src/main/cpp/pty_helpers.hpp`, which are the same functions called by the
JNI entry points in `forge_pty.cpp`:

- `BM_PtyDimensionsValidation`, `BM_PtyResizeSweepValidation`: terminal
  row/column validation used by `nativeStart` and `nativeResize`.
- `BM_PtyArgvEnvPointerTable[n]`: string-to-`char**` pointer tables built
  before `execve`, with `n` extra environment entries.
- `BM_PtyStringArgumentValidation[n]`: per-entry argv/env validation
  (non-empty, bounded, no embedded NUL).
- `BM_PtyLaunchPreparation[n]`: the full pre-fork preparation of `nativeStart`
  (dimension, cwd, argv and env validation plus pointer tables).
- `BM_PtySignalFilter`: the signal allow-list used by `nativeSignal`.
- `BM_PtyWaitStatusEncoding`: the `waitpid` status encoding returned by
  `nativeWait`.

These workloads are pure CPU and memory operations, so they run under the
CodSpeed CPU simulation instrument. Simulation results are useful for
comparisons of instrumented helper logic, but they do not measure kernel or
system-call costs and must not be presented as end-to-end PTY performance.

### Network planning (Kotlin, JMH, exploratory)

Location: `benchmarks/jvm/` (standalone Gradle build, separate from the Android
build). It compiles
`app/src/main/java/com/HG/Forge/Ts/data/network/CidrCalculator.kt` directly,
so the benchmarks exercise the production source:

- `CidrCalculatorBenchmark.calculate`: IPv4 `/24` and `/31`, IPv6 `/64`, and
  IPv4-mapped IPv6 inputs.
- `AddressBinaryBenchmark.toBinary`: IPv4 and IPv6 binary conversion.
- `VlsmCalculatorBenchmark.allocate`: 4-subnet and 64-subnet VLSM plans.

The JMH suite and CodSpeed JMH fork submodule are retained for deliberate
manual experiments. It is not part of the CodSpeed GitHub Actions workflow.
The repository currently uses standard GitHub-hosted runners; walltime
measurements there are too noisy to establish a dependable performance
baseline. Re-enable CodSpeed walltime reporting only after a suitable runner
and repeatable measurement environment are available. See
[walltime on personal accounts](https://codspeed.io/docs/instruments/walltime#usage-on-personal-github-accounts).

## Run in GitHub Actions

The CodSpeed workflow runs the native C++ simulation suite on pushes to
`main`, on pull requests targeting `main` that touch native benchmark code,
and on manual dispatch. Default-branch runs establish the native baseline used
for pull-request comparisons. The job authenticates with GitHub OIDC
(`id-token: write`); no CodSpeed token is stored in the repository.

The Kotlin JMH suite is intentionally not run as a CodSpeed CI job. This avoids
publishing noisy walltime data as if it were a stable regression signal.

## Build locally

C++ suite, on a Linux host with CMake, a C++17 compiler and Git:

```sh
cmake -S benchmarks -B build/benchmarks \
  -DCODSPEED_MODE=simulation \
  -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build build/benchmarks --parallel 4
codspeed run --mode simulation -- ./build/benchmarks/forge_pty_bench
```

Kotlin suite, with JDK 21 and Gradle 8.13:

```sh
git submodule update --init third_party/codspeed-jvm
git -C third_party/codspeed-jvm submodule update --init \
  jmh-fork/jmh-core/native-instrument-hooks/instrument-hooks
gradle -p benchmarks/jvm jmh
```

The Kotlin command above runs JMH locally; it does not upload a CodSpeed
baseline. The CodSpeed C++ dependency is pinned to the `v2.4.0` release commit
and the JMH fork submodule to `v0.2.1`.

## Measurement boundaries

These are host-side measurements. They do not represent Android device latency
and do not cover the Kotlin/JNI/Compose path, process creation, PTY I/O
(`poll`, `read`, `write`, `ioctl`), Android scheduling or terminal rendering.
Kernel and I/O-bound PTY lifecycle benchmarks should be added as a separate
walltime suite only when a suitable dedicated runner is available.

## References

- [CodSpeed: GitHub Actions integration](https://codspeed.io/docs/integrations/ci/github-actions)
- [CodSpeed: C++ Google Benchmark integration](https://codspeed.io/docs/benchmarks/cpp)
- [CodSpeed: Kotlin JMH integration](https://codspeed.io/docs/benchmarks/kotlin)
- [CodSpeed: Walltime instrument](https://codspeed.io/docs/instruments/walltime)
