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

## OSINT Web reference
- Web-Check — Copyright (c) 2023 Alicia Sykes; MIT License.
- Source: https://github.com/lissy93/web-check
- The initial native OSINT Web module uses the project as a feature and architecture reference and reuses FORGE-TS networking infrastructure. It does not embed the Web-Check web application.
- License text: third_party/licenses/WEB-CHECK-MIT.txt

Recheck dependency versions and license notices before distribution.


## Terminal emulator

### ConnectBot termlib
- Project: https://github.com/connectbot/termlib
- Version: 0.0.12 (`org.connectbot:termlib:0.0.12`)
- Purpose: Jetpack Compose VT terminal emulator integrated with the native PRoot PTY bridge.
- License: Apache License 2.0.
- The bundled libvterm terminal parser is MIT licensed.
