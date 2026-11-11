# FORGE-TS CodSpeed Benchmarks

## Purpose

FORGE-TS has two host-side benchmark suites, both reported to CodSpeed from
`.github/workflows/codspeed.yml`. Neither adds any dependency to the APK.

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
CodSpeed CPU simulation instrument, which gives deterministic results on
standard GitHub runners.

### Network planning (Kotlin, JMH, walltime)

Location: `benchmarks/jvm/` (standalone Gradle build, separate from the Android
build). It compiles `app/src/main/java/com/HG/Forge/Ts/data/network/CidrCalculator.kt`
directly, so the benchmarks run the production code:

- `CidrCalculatorBenchmark.calculate`: IPv4 `/24` and `/31`, IPv6 `/64`, and
  IPv4-mapped IPv6 inputs.
- `AddressBinaryBenchmark.toBinary`: IPv4 and IPv6 binary conversion.
- `VlsmCalculatorBenchmark.allocate`: 4-subnet and 64-subnet VLSM plans.

The JVM integration uses the CodSpeed JMH fork, vendored as the git submodule
`third_party/codspeed-jvm`. CodSpeed only supports the walltime instrument on
the JVM.

Note: CodSpeed macro runners are only available to organization repositories,
so the walltime job runs on a standard `ubuntu-latest` runner. Expect
noticeably higher run-to-run variance there than for the simulation suite. See
[walltime on personal accounts](https://codspeed.io/docs/instruments/walltime#usage-on-personal-github-accounts).

## Run in GitHub Actions

The workflow runs on pushes to `main`, on pull requests targeting `main` that
touch the benchmarked code, and on manual dispatch. Default-branch runs set the
baseline used for pull-request comparisons. Both jobs authenticate with GitHub
OIDC (`id-token: write`); no CodSpeed token is stored in the repository.

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
# or, reporting to CodSpeed:
codspeed run --mode walltime -- gradle -p benchmarks/jvm jmh
```

The CodSpeed C++ dependency is pinned to the `v2.4.0` release commit and the
JMH fork submodule to `v0.2.1`, so the benchmark toolchain only changes on
purpose.

## Measurement boundaries

These are host-side measurements. They do not represent Android device latency
and do not cover the Kotlin/JNI/Compose path, process creation, PTY I/O
(`poll`, `read`, `write`, `ioctl`), Android scheduling or terminal rendering.
Kernel and I/O-bound PTY lifecycle benchmarks should be added as a separate
walltime suite once a suitable dedicated runner is available.

## References

- [CodSpeed: GitHub Actions integration](https://codspeed.io/docs/integrations/ci/github-actions)
- [CodSpeed: C++ Google Benchmark integration](https://codspeed.io/docs/benchmarks/cpp)
- [CodSpeed: Kotlin JMH integration](https://codspeed.io/docs/benchmarks/kotlin)
- [CodSpeed: Walltime instrument](https://codspeed.io/docs/instruments/walltime)
