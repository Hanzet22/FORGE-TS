# Phase 30 — PRoot Native Engine Packaging

**Status:** Android packaging contract and runtime prerequisites documented; native engine binaries are not generated or bundled yet.

## Decision summary

- PRoot source of record: `termux/proot`, release tag `v5.1.107.102`.
- Source archive: `https://github.com/termux/proot/archive/v5.1.107.102.zip`.
- SHA-256 from the Termux package recipe: `30d12a6e2cc4147123756ff58edea262d3083bbc238c12d0ab9b39e3d1c40852`.
- License declared by the Termux package recipe: GPL-2.0.
- Official Termux package recipe declares `libandroid-shmem` and `libtalloc` dependencies and builds with `PROOT_WITH_LIBANDROID_SHMEM=true`.
- A static Android build recipe was inspected at `berilevi/proot-static`, commit `6f8ebfd8e24887dfba64c3f2d7d5fe9dc059b60e`. It demonstrates an AArch64 static build, bundled talloc 2.4.2, NDK toolchain use and 16 KB ELF page alignment. It is a community build reference, not the authoritative source pin and not an approved binary download source.
- No third-party prebuilt binary is copied into FORGE-TS.

## ABI and packaging contract

Initial native-engine target: `arm64-v8a`. Other ABIs remain unsupported by the PRoot engine until separately built and verified.

Expected files in `app/src/main/jniLibs/arm64-v8a/`:

| Packaged name | Build output role | Runtime use |
|---|---|---|
| `libproot.so` | PRoot executable, renamed from the build output `proot` | Direct `ProcessBuilder` executable |
| `libproot_loader.so` | Native guest ELF loader, renamed from `loader` | `PROOT_LOADER` |
| `libproot_loader32.so` | Optional 32-bit guest loader, renamed from `loader32` | `PROOT_LOADER_32` if 32-bit guest support is enabled |

These are executable ELF payloads packaged in Android's native-library location. Their `.so` suffix is a packaging convention; they are not JNI libraries to be loaded with `System.loadLibrary`.

The Android Gradle packaging option `jniLibs.useLegacyPackaging = true` is enabled so native payloads are extracted to the installed native-library directory. Runtime paths must always be resolved from `ApplicationInfo.nativeLibraryDir`, never hard-coded.

## Runtime environment

- `PROOT_LOADER` must point to the packaged loader for the device ABI.
- `PROOT_TMP_DIR` must point to a writable, app-private temporary directory. The adapter now uses the selected instance workspace's `tmp/` directory.
- `TMPDIR` is set to the same app-private temporary directory for child tools.
- `LD_LIBRARY_PATH` points to `nativeLibraryDir` for any host-side shared dependencies.
- The launcher must use the selected distribution's exact app-managed rootfs and keep the existing `--kill-on-exit` lifecycle behavior.

## Dependency and compatibility gates

The static reference build vendors talloc and links it statically, but the official Termux recipe also enables Android shared-memory support. Before a distributable engine is approved, the build must explicitly decide whether `libandroid-shmem` is required for the intended FORGE-TS feature set. Do not silently omit it or claim full dependency closure from the static example.

The build must record:
- Exact PRoot source tag, archive digest and any local patch set.
- Exact talloc source/version/digest and license notice.
- Whether Android shared-memory support is compiled in and how it is supplied.
- NDK version, Android API target, ABI, linker flags and 16 KB ELF alignment.
- ELF machine/class, executable type, dependency table and loader architecture.
- Corresponding source and license materials for every redistributed native component.

The inspected community static script currently builds AArch64 only. It is not evidence that x86_64 or 32-bit artifacts are available.

## Work completed in this phase

- Corrected Android packaging to extract native executable payloads into `nativeLibraryDir`.
- Added explicit app-private `PROOT_TMP_DIR` and `TMPDIR` routing in the PRoot adapter.
- Locked the initial source and packaging decision in this document.
- Kept the current adapter's `libproot.so` / `libproot_loader.so` filename contract; upstream build outputs must be renamed during packaging.
- Preserved the existing DataStore, workspace layout, Supabase and Redis configuration.

## Not completed / still gated

- No native binaries were built, downloaded or committed.
- No `jniLibs/arm64-v8a` payload is claimed to exist.
- No APK build, device execution, ptrace test, rootfs install or interactive PTY session was run.
- The PRoot adapter remains unavailable until the actual engine, matching loader, dependency closure and verified distro rootfs are installed and the disposable guest-process probe succeeds.
- A successful source audit or native file-existence check alone must never mark the runtime READY.

## References

- Termux PRoot: https://github.com/termux/proot
- Termux package recipe: https://github.com/termux/termux-packages/blob/master/packages/proot/build.sh
- PRoot source archive: https://github.com/termux/proot/archive/v5.1.107.102.zip
- Static Android build reference: https://github.com/berilevi/proot-static/tree/6f8ebfd8e24887dfba64c3f2d7d5fe9dc059b60e
- Android NDK ABI guidance: https://developer.android.com/ndk/guides/abis
- Android native library packaging: https://developer.android.com/guide/topics/manifest/application-element#extractNativeLibs


## P0.5.6 — Reproducible ARM64 build pipeline

**Status:** Build script and GitHub Actions workflow committed. Native binaries are generated as a CI artifact only after the workflow succeeds; they are not committed to the repository and are not yet available to the Android adapter.

### Dependency decision

- Build PRoot from the pinned Termux source archive above.
- Build talloc 2.5.0 from its pinned upstream source archive (SHA-256: `912afa237510ae542a7733998eb18a12bcda35ab6729c8e2ddb43e8d0ebab007`) and link it statically.
- Disable `PROOT_WITH_LIBANDROID_SHMEM` for the first standalone FORGE-TS runtime. The optional library is used by Termux's Android shared-memory integration; the initial FORGE-TS runtime does not promise Termux interoperability or cross-process Android shared-memory features. This avoids adding an unneeded native dependency to the first deliverable.
- Preserve PRoot's own source and loader logic without local patches in this first build.
- Use Android NDK r27c, Android API 29, `arm64-v8a`, and 16 KB maximum ELF page alignment. No other ABI is claimed.

### Build implementation

- Builder: `scripts/native/build-proot-runtime.sh`.
- Workflow: `.github/workflows/proot-native.yml`.
- Trigger: manual dispatch or relevant changes on `main`.
- The script verifies both source archives against pinned SHA-256 digests before extraction.
- It cross-compiles the talloc static archive using the cross-answer profile used by the Termux package recipe, then builds PRoot and its separate 64-bit loader from the pinned source.
- The output names match the Android adapter contract:
  - `arm64-v8a/libproot.so`
  - `arm64-v8a/libproot_loader.so`
- The build checks ELF architecture, 16 KB maximum page alignment for both payloads, and absence of a dynamic `libtalloc.so` dependency. It emits ELF reports, source/license materials, a build manifest and SHA-256 sums.
- GitHub Actions uploads `forge-ts-proot-native-arm64` as a 14-day artifact. The workflow copies the two ARM64 ELF payloads into `app/src/main/jniLibs/arm64-v8a/` and commits them to `main` together with the build manifest, payload SHA-256 list and complete upstream license directory. The generated payload commit does not modify the workflow's own path filters, so it does not start another native build.

### Validation boundary

- The pipeline source and workflow are committed. A successful CI execution and artifact review are not yet confirmed.
- The adapter must continue to report unavailable until the native build job successfully commits both payloads and the Android packaging workflow completes.
- A successful CI build is not a device runtime test. The disposable guest-child probe, ptrace behavior, rootfs verification, and Android device execution remain mandatory before enabling PRoot.
- The first artifact is ARM64 only; no 32-bit guest loader is generated or advertised by this phase.


### P0.5.7 — CI compile correction
The first CI compile reached PRoot but failed because ashmem_memfd.c used strcmp and memset without including string.h. The builder now inserts string.h at the top of that source file before compilation. This is a compile-only compatibility adjustment; runtime behavior is unchanged. The CI run must still pass the complete build and ELF validation before the artifact can be packaged.

### P0.5.8 — Include talloc portability implementation in static archive
The first link attempt with talloc 2.5.0 exposed an undefined `rep_memset_explicit` symbol. The upstream 2.5.0 release uses the libreplace portability layer for this function when the target libc does not provide `memset_explicit` (Android API 29 does not provide that API). The builder now includes the generated `lib/replace/*.o` objects alongside `talloc*.o` when assembling the static archive, so the replacement implementation is linked instead of leaving an unresolved reference. This is a dependency-closure correction; it does not change PRoot runtime behavior.


### P0.5.9 — Native payload to APK CI handoff

- Removed the generated PRoot payload commit's `[skip ci]` marker. The native workflow's generated commit now triggers the Android Debug APK workflow, so the packaged ELF payloads are included in an actual APK build rather than silently skipped by CI.
- Preserve all upstream license files from the build artifact under `app/src/main/assets/licenses/proot-runtime/`; do not ship only the PRoot notice while omitting the talloc notice.
- Added a dedicated `PAYLOAD-SHA256SUMS` containing only the two executable ARM64 payload hashes. The broader `SHA256SUMS` remains an artifact-level manifest.
- A successful APK build does not prove ptrace or PRoot execution on a physical device.
