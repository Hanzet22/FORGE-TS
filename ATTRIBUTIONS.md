# Attributions

Third-party libraries, fonts, datasets, documentation sources and their applicable licenses are listed here before distribution.

## Initial dependencies
- Kotlin — Apache License 2.0
- AndroidX, Jetpack Compose and Material 3 — Apache License 2.0
- Ktor — Apache License 2.0
- Room, DataStore and WorkManager — Apache License 2.0

## Fonts

### DejaVu Sans
- Copyright: Bitstream, Inc. (Bitstream Vera); DejaVu modifications are in the public domain; imported Arev glyphs are copyright Tavmjong Bah.
- Used in the Android app as the primary UI typography, including regular, bold, oblique and bold-oblique styles.
- License: Bitstream Vera / Arev permissive license notices, included at `third_party/licenses/DEJAVU-FONTS-BITSTREAM-VERA.txt` and the original `assets/fonts/LICENSE.txt`.
- Original font files and accompanying project documentation are retained in `assets/fonts/`.

### Kaushan Script
- Copyright (c) 2011 Pablo Impallari and Igino Marini.
- Used for the FORGE-TS wordmark on the Home screen.
- License: SIL Open Font License 1.1, including the Reserved Font Name “Kaushan Script”; included at `third_party/licenses/KAUSHAN-SCRIPT-OFL.txt` and the original `assets/fonts/OFL.txt`.
- The font is bundled unmodified.

## Additional font families

The following font families were added under `assets/fonts/`. Their original OFL 1.1 license notices are retained in each family directory.

### Anton
- Copyright: Vernon Adams.
- Source: https://github.com/googlefonts/AntonFont
- License: SIL Open Font License 1.1.
- License notice: `assets/fonts/Anton/OFL.txt`.

### Archivo
- Copyright: Omnibus-Type.
- Source: https://github.com/Omnibus-Type/Archivo
- License: SIL Open Font License 1.1.
- License notice: `assets/fonts/Archivo/OFL.txt`.

### Azeret Mono
- Copyright: Displaay, Martin Vácha and Daniel Quisek.
- Source: https://github.com/displaay/Azeret/
- License: SIL Open Font License 1.1.
- License notice: `assets/fonts/AzeretMono/OFL.txt`.

### Crimson Pro
- Copyright: Jacques Le Bailly.
- Source: https://github.com/Fonthausen/CrimsonPro
- License: SIL Open Font License 1.1.
- License notice: `assets/fonts/CrimsonPro/OFL.txt`.

### Lora
- Copyright: Cyreal, Olga Karpushina and Alexei Vanyashin.
- Source: https://github.com/cyrealtype/Lora-Cyrillic
- License: SIL Open Font License 1.1.
- License notice: `assets/fonts/Lora/OFL.txt`.

## OSINT Web reference
- Web-Check — Copyright (c) 2023 Alicia Sykes; MIT License.
- Source: https://github.com/lissy93/web-check
- The initial native OSINT Web module uses the project as a feature and architecture reference and reuses FORGE-TS networking infrastructure. It does not embed the Web-Check web application.
- License text: third_party/licenses/WEB-CHECK-MIT.txt

Recheck dependency versions and license notices before distribution.


## Native PRoot runtime

### PRoot
- Version: 5.1.107.102, Termux fork.
- Source archive: https://github.com/termux/proot/archive/v5.1.107.102.zip
- SHA-256: `30d12a6e2cc4147123756ff58edea262d3083bbc238c12d0ab9b39e3d1c40852`.
- License evidence: Termux package metadata declares `GPL-2.0`; the PRoot upstream project identifies its license as `GPL-2.0-or-later`. The bundled COPYING file contains the GPL version 2 text but does not itself resolve the exact later-version grant for this fork. Confirm the tag-level grant before public binary distribution.
- License text: `app/src/main/assets/licenses/proot-runtime/PRoot-COPYING`.

### talloc
- Version: 2.5.0; statically linked into the PRoot executable by the native build script.
- Source archive: https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz
- SHA-256: `912afa237510ae542a7733998eb18a12bcda35ab6729c8e2ddb43e8d0ebab007`.
- License: GNU Lesser General Public License 3.0 or later (LGPL-3.0-or-later).
- License text: `app/src/main/assets/licenses/proot-runtime/TALLOC-LICENSE`.
- Static-link distribution must preserve the LGPL relinking and corresponding-source requirements. The repository's native build script pins both source archives and records the build configuration; verify that the release source package and relinking materials accompany every distributed binary.

## Terminal emulator

### ConnectBot termlib
- Project: https://github.com/connectbot/termlib
- Version: 0.0.12 (`org.connectbot:termlib:0.0.12`)
- Purpose: Jetpack Compose VT terminal emulator integrated with the native PRoot PTY bridge.
- License: Apache License 2.0.
- The bundled libvterm terminal parser is MIT licensed.
