# WorkStation Embedded Display — Feasibility Review

**Review date:** 2026-11-11  
**Scope:** Embedded X11 for the existing Android application  
**Decision:** Keep the display backend replaceable. Do not bundle a server until its Android API floor, protocol coverage, lifecycle, rendering and licensing are verified.

## Product constraints

- The display must render inside FORGE-TS; a separately installed X11 companion app is not an acceptable required dependency.
- The application minimum remains Android 10 (API 29).
- Runtime support is capability-gated. A backend may be unavailable on older devices, but it must never claim to be ready when it is not.
- PRoot and Native Linux remain separate execution modes. The display server is a shared service and does not change the guest execution boundary.
- No TCP X11 listener or unauthenticated display endpoint is permitted. Client admission and Xauthority/session ownership belong to the host integration.

## Candidates reviewed

### 1. Termux:X11

Upstream describes Termux:X11 as an Android X-server add-on consisting of an Android app plus a companion Termux package. That packaging model conflicts with the embedded, no-required-companion product requirement.

Reference: https://github.com/termux/termux-x11

**Decision:** Do not integrate the standalone app as the WorkStation display backend.

### 2. nwrkbiz/android-xserver

This MIT-licensed Java X11 server runs inside an Android View subclass and is explicitly designed to be embedded in other applications. Its README describes porting C++ FLTK applications and an embedded lightweight window manager. The repository's latest push is dated 2025-03-31.

The repository documentation does not establish compatibility with the complete XFCE desktop workload, the X11 extensions/compositing behavior expected by modern desktop components, or current Android 10–16 devices. An embedded View alone is not proof of XFCE compatibility.

References:
- https://github.com/nwrkbiz/android-xserver
- https://github.com/nwrkbiz/android-xserver/blob/master/LICENSE

**Decision:** Useful reference and possible small-client prototype, but not selected as the full XFCE display engine without a compatibility demonstration.

### 3. pelya/xserver-xsdl

This is an X.Org server port using an SDL 1.2 backend. Its own README lists no OpenGL hardware acceleration, no XRANDR support and hacky text input. Those limitations conflict with the planned resize/input experience and may constrain desktop usability.

Reference: https://github.com/pelya/xserver-xsdl

**Decision:** Not selected for the primary WorkStation display backend.

### 4. mekhontsev/magicdesk-x11 + MagicDesk x11-runtime

This is the closest reviewed candidate to the product architecture. The native fork exposes an embedding C API, independent X sessions, output surfaces, pointer/keyboard/text input, resize and renderer integration. The separate Android x11-runtime module supplies Java, Binder and JNI integration; the native fork alone is not a drop-in Android module.

Important constraints:
- The native engine declares Android API 34 as its floor.
- Its Android runtime module also declares minSdk 34.
- The project states that Android 14 device coverage remains pending; successful compilation is not device validation.
- The engine and module are GPL-3.0 and require preservation of notices and corresponding source obligations when redistributed.
- The host must still implement session ownership, authenticated client admission, process lifecycle, renderer integration and Android surface/input handling.

References:
- https://github.com/mekhontsev/magicdesk-x11
- https://github.com/mekhontsev/magicdesk-x11/blob/main/docs/embedding.md
- https://github.com/mekhontsev/magicdesk-x11/blob/main/lorie/src/main/cpp/lorie/embedded.h
- https://github.com/mekhontsev/magicdesk/blob/main/x11-runtime/README.md
- https://github.com/mekhontsev/magicdesk/blob/main/x11-runtime/build.gradle

**Decision:** Keep as the leading candidate for a separately capability-gated Android 14+ backend, subject to source pinning, full dependency/license inventory, host integration review and device validation. It cannot be represented as supporting Android 10–13.

## Current engineering decision

No reviewed backend has yet been demonstrated to satisfy all of these simultaneously:

1. Embedded in-app display without a required companion app.
2. Full XFCE desktop compatibility.
3. Android 10 (API 29) through current Android support.
4. Reliable touch, keyboard, text input and live resize.
5. Safe local client admission and clean session teardown.
6. Reproducible ARM64 build and complete license/source compliance.

Therefore:
- Do not add an unverified X11 binary or third-party module to the APK yet.
- Keep display capability separate from runtime capability: a PRoot shell may work while desktop display is unavailable.
- Keep UNAVAILABLE/UNKNOWN explicit on devices that do not meet the selected backend's requirements.
- The next P1 implementation slice is the host-side display contract and capability reporting; native engine integration follows only after the backend target and minimum Android version are explicitly pinned.
