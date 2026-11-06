# FORGE-TS
**Network & Systems Toolkit** — a native Android app for network utilities, operating-system references and systems administration learning.

## Phase 0 foundation
- Native Android APK, Kotlin and Jetpack Compose + Material 3
- Light professional design direction: white, grey, navy and blue
- MVVM-oriented presentation and data boundaries
- Room, DataStore, Ktor and WorkManager dependencies included
- Supabase remote boundary; no privileged credentials in the client
- GitHub Actions cloud debug APK workflow

## Core modules
1. IPv4/IPv6 CIDR calculator
2. MikroTik learning references
3. OS command reference library
4. Host checker for authorized hosts
5. Public IP information
6. DNS resolver lists
7. SSH and TLS references
8. Repository explorer

The command area is a reference guide, not an unrestricted remote shell. Host checks must only target systems the user is authorized to inspect.

## Build
Run the Android Debug APK workflow from the GitHub Actions tab. It uses JDK 17 and Gradle 8.13.

## Package
Selected namespace and application ID: com.HG.Forge.Ts. Keep both aligned unless a deliberate migration is approved.

## Status
Initial Phase 0 scaffold. Full module behavior, Room schema, Supabase integration and release signing are subsequent work.
