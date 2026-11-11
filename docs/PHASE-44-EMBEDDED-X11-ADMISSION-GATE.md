# Phase 44 — Embedded X11 Candidate Admission Gate

## Candidate baseline
- Project: `mekhontsev/magicdesk-x11`
- Reviewed source revision: `0676518439b125cda7b4dd19a58f6763afbf81b2`
- Android native floor: API 34
- ABI currently supported by the candidate build: `arm64-v8a`
- Native output library: `libXlorie.so`
- License: GNU GPL-3.0; preserve upstream history, notices, nested dependency licenses and corresponding source/build scripts when conveying an APK containing this engine.

## Delivered in FORGE-TS
`MagicDeskX11PlatformGate` provides a pure, deterministic admission decision for Android API, supported ABI, packaged native library and host-bridge readiness. It never starts or loads native code. It never reports an X server as available or running. Passing the gate means only that platform prerequisites are satisfied and native initialization may be attempted by a future adapter.

The gate currently returns `HOST_BRIDGE_NOT_IMPLEMENTED` even if API, ABI and library checks pass. This is intentional until JNI ownership, authenticated local client admission, server startup/readiness, Android Surface output binding, input routing and teardown are implemented.

## Build integration blockers
- FORGE-TS currently uses NDK `27.2.12479018`; the candidate's documented build uses NDK `27.3.13750724`. Do not silently change the app-wide NDK until a pinned native build is integrated.
- Candidate native build requires recursive Git submodules, CMake 3.22+, Ninja, Python 3, Bison, patch and a host C compiler.
- Candidate Android runtime is not a drop-in module: its Java/Binder/JNI module depends on MagicDesk's separate `hosted-runtime`. FORGE-TS must implement/adapt the host boundary rather than copying the Gradle module and leaving unresolved dependencies.
- Candidate source is GPL-3.0. Before distributing an APK with it, decide the product's whole-work licensing and publish the exact corresponding source, pinned nested dependencies, modifications, build scripts and notices. Do not ship only a binary or a link to a moving branch.

## Compatibility behavior
- FORGE-TS application minimum remains Android 10 / API 29.
- Embedded X11 candidate is separately gated to Android 14 / API 34+ and ARM64.
- Android 10–13 and non-ARM64 devices must report display backend unavailable, without blocking terminal or PRoot features.
- API 34+ and ARM64 alone do not prove device compatibility. Native initialization and physical-device rendering/input validation remain mandatory.

## Next implementation slice
Implement the JNI host bridge against the candidate's public `embedded.h` contract, first with a minimal native startup/shutdown path and a single Android Surface. Keep the bridge unregistered until the native library is packaged, local client admission is authenticated, and readiness/teardown are confirmed. Then add bounded input and live resize.

## Not delivered
No X11 engine source, native binary, third-party Gradle module, Binder service, X server, XFCE launcher or runtime adapter is included in this phase.
