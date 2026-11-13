# Third-Party Attributions

FORGE-TS application source is licensed under **GNU AGPL-3.0** (see [LICENSE](LICENSE)). Third-party components retain their own licenses; this file does not relicense them.

**Inventory scope:** direct dependencies declared in `app/build.gradle.kts`, explicitly bundled native components, fonts, and identified embedded subcomponents. This is not yet a complete resolved transitive SBOM. Before a public release, resolve both debug and release runtime classpaths and reconcile every transitive artifact, embedded NOTICE, and license. Do not infer that an unlisted transitive artifact is absent.

## Android runtime dependencies

| Component / Maven coordinate | Version | License | Use / notes |
|---|---:|---|---|
| AndroidX Activity Compose, Core KTX, Compose UI/Foundation/Material 3/Material Icons, Lifecycle, Navigation, Room, DataStore, WorkManager | Compose BOM 2025.12.00; Activity 1.11.0; Core 1.17.0; Lifecycle 2.9.4; Navigation 2.9.5; Room 2.8.0; DataStore 1.1.7; WorkManager 2.10.5 | Apache-2.0 | Android platform and UI/runtime libraries |
| Kotlin / Kotlin Android and Compose compiler plugins | 2.2.21 | Apache-2.0 | Application language and compiler tooling |
| `org.connectbot:termlib` | 0.0.12 | Apache-2.0 | Terminal emulator library; license text: `third_party/licenses/APACHE-2.0.txt` |
| libvterm (bundled inside ConnectBot termlib source/artifact) | As included by termlib 0.0.12 | MIT | Native terminal parser; upstream notice: `third_party/licenses/LIBVTERM-MIT.txt` |
| `io.ktor:ktor-client-core`, `ktor-client-android`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` | 3.3.1 | Apache-2.0 | HTTP client and JSON serialization |
| `org.apache.commons:commons-compress` | 1.28.0 | Apache-2.0 | Archive handling |
| `org.tukaani:xz` | 1.10 | 0BSD | XZ/LZMA compression; notice text: `third_party/licenses/XZ-0BSD.txt` |

AndroidX, Kotlin, Ktor, Apache Commons Compress, and ConnectBot termlib use the Apache License 2.0. The full license text is retained at `third_party/licenses/APACHE-2.0.txt`. The exact transitive dependency closure still needs to be generated from Gradle resolution before release.

## Build tooling (not shipped as app runtime libraries)

| Tool | Version | License / terms | Notes |
|---|---:|---|---|
| Android Gradle Plugin | 8.13.2 | Apache-2.0 | Build plugin |
| Gradle | 8.13 (CI toolchain) | Apache-2.0 | Build system |
| Kotlin Gradle plugins | 2.2.21 | Apache-2.0 | Build plugins |
| Android NDK | 27.2.12479018 | Android SDK License Agreement | Used to compile ARM64 native runtime |
| CMake | 3.22.1 | BSD-3-Clause | Native build tooling |
| GitHub Actions and CodSpeed actions | Workflow-pinned action versions | Each action retains its own license | CI tooling; not packaged into the APK |

Build-tool licensing does not replace the obligation to identify the licenses of artifacts those tools produce or download.

## Bundled native runtime

### PRoot (Termux fork)

- Version: 5.1.107.102.
- Source: https://github.com/termux/proot/tree/v5.1.107.102
- Source archive SHA-256: `30d12a6e2cc4147123756ff58edea262d3083bbc238c12d0ab9b39e3d1c40852`.
- Termux package recipe metadata declares `GPL-2.0`: https://github.com/termux/termux-packages/blob/bb4dce3e28fa5fbcca7d91fee5910373e559ed41/packages/proot/build.sh
- The upstream PRoot project README identifies `GPL-2.0-or-later`: https://github.com/proot-me/proot
- The pinned Termux fork's `COPYING` contains the GPL version 2 license text, but the exact grant for the fork is not conclusively established by that file alone.
- Bundled license text: `app/src/main/assets/licenses/proot-runtime/PRoot-COPYING`.

**Distribution gate:** do not describe the pinned fork as definitively GPL-2.0-only or GPL-2.0-or-later until the tag's complete licensing grant is verified. See [Licensing Release Gate](docs/LICENSING-RELEASE-GATE.md).

### talloc (Samba)

- Version: 2.5.0.
- Source archive: https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz
- Source archive SHA-256: `912afa237510ae542a7733998eb18a12bcda35ab6729c8e2ddb43e8d0ebab007`.
- License: LGPL-3.0-or-later, as stated in the upstream talloc header.
- Linkage: statically linked into the PRoot executable by `scripts/native/build-proot-runtime.sh`.
- License text: `app/src/main/assets/licenses/proot-runtime/TALLOC-LICENSE`.

Static linking requires preserving the LGPL relinking and corresponding-source obligations. The build script and source hashes are recorded, but release packaging must also provide the materials needed to relink the executable with a modified talloc library. Resolve the PRoot license-grant question before distribution.

## Fonts and visual assets

| Asset | Copyright / source | License | Retained notice |
|---|---|---|---|
| DejaVu Sans family | Bitstream Vera; DejaVu modifications are public domain; imported Arev glyphs by Tavmjong Bah | Bitstream Vera / Arev permissive license | `third_party/licenses/DEJAVU-FONTS-BITSTREAM-VERA.txt`, `assets/fonts/LICENSE.txt` |
| Kaushan Script | Pablo Impallari and Igino Marini | SIL Open Font License 1.1; Reserved Font Name “Kaushan Script” | `third_party/licenses/KAUSHAN-SCRIPT-OFL.txt`, `assets/fonts/OFL.txt` |
| Anton | Vernon Adams; https://github.com/googlefonts/AntonFont | SIL Open Font License 1.1 | `assets/fonts/Anton/OFL.txt` |
| Archivo | Omnibus-Type; https://github.com/Omnibus-Type/Archivo | SIL Open Font License 1.1 | `assets/fonts/Archivo/OFL.txt` |
| Azeret Mono | Displaay, Martin Vácha and Daniel Quisek; https://github.com/displaay/Azeret/ | SIL Open Font License 1.1 | `assets/fonts/AzeretMono/OFL.txt` |
| Crimson Pro | Jacques Le Bailly; https://github.com/Fonthausen/CrimsonPro | SIL Open Font License 1.1 | `assets/fonts/CrimsonPro/OFL.txt` |
| Lora | Cyreal, Olga Karpushina and Alexei Vanyashin; https://github.com/cyrealtype/Lora-Cyrillic | SIL Open Font License 1.1 | `assets/fonts/Lora/OFL.txt` |

The font files are retained in their family directories. Keep the original OFL text and Reserved Font Name terms with redistributed font files.

## Reference material

- Web-Check — Copyright © 2023 Alicia Sykes; MIT License. Source: https://github.com/lissy93/web-check. FORGE-TS uses it as a feature/architecture reference; the Web-Check web application is not embedded. Retained notice: `third_party/licenses/WEB-CHECK-MIT.txt`.

## Release checklist

1. Generate a resolved debug and release dependency inventory/SBOM, including transitive Maven artifacts and embedded AAR/JAR notices.
2. Confirm each runtime artifact's license and required NOTICE/copyright text against its exact resolved version.
3. Resolve the PRoot fork license grant and talloc static-link compatibility before distributing APKs.
4. Include corresponding source and relinking materials for copyleft native binaries as required by their applicable licenses.
5. Verify the shipped APK contains the same notices and source-offer information as the source repository.
