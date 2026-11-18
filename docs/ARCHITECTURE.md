# FORGE-TS Current Architecture

**Status snapshot:** 2026-11-18. For the detailed per-feature implementation and platform-gap matrix, use the [development handoff](DEVELOPMENT-HANDOFF.md). This is a compact architecture map, not a complete feature inventory.

## Product and platform

- Native Android application written in Kotlin with Jetpack Compose and Material 3.
- Application ID and namespace: `com.HG.Forge.Ts`.
- `minSdk = 29`, `compileSdk = 36`, `targetSdk = 36`, Java/Kotlin target 17, ARM64 native ABI.
- Six top-level screens: Home, Learn, Network, Simulate, Tools and Settings. Simulate and Tools use the shared Supabase Auth gate.
- UI supports EN/ID for the maintained fixed-string catalog; some dynamic text and lesson explanations remain English.

## Operating model

Local-first, online and offline. Local utilities and reference content remain usable without connectivity. Network-backed features disclose their provider and use bounded requests. Source code and workflow definitions establish behavior. [Phase documents](INDEX.md#phase-history) preserve historical plans and acceptance criteria; check their status against the [development handoff](DEVELOPMENT-HANDOFF.md).

## Layers

- **Presentation:** Compose screens and components in `ui/`; `ForgeApp.kt` owns navigation, intro overlay, shared auth/session lifecycle and language state.
- **State:** screen ViewModels and state holders; selected tab UI state is retained across navigation.
- **Data:** `data/network/` has local calculations and bounded Ktor repositories; `data/preferences/` persists user preferences.
- **Runtime:** `data/simulation/` contains PRoot provisioning/execution, native PTY, mirror profiles, display-session contracts and simulation foundations.
- **Remote boundary:** `data/remote/` contains Supabase and Redis clients. Server secrets must never enter the Android app.
- **Background work:** `data/sync/` queues and retries bounded diagnostic uploads through WorkManager.
- **Backend:** `supabase/functions/` contains the Redis gateway and diagnostics ingestion; migrations define RLS and telemetry schemas.

## Implemented behavior and invariants

### Core Networking

CIDR calculation is local-only. Public IP lookup uses HTTPS and identifies its provider. Host Checker performs one DNS resolution and one user-selected TCP port check; optional TLS checks use platform certificate and hostname verification. It is not a port scanner.

### Knowledge Center

MikroTik and OS command content is educational reference material. Search and filtering run locally. Commands are never executed. Learning labs are simulated and send no packets.

### DNS and Protocols

DNS lookup uses Google Public DNS over HTTPS for supported record types. Results are transient UI state and are not persisted. Protocol references are bundled and work offline. The PublicDNS.xyz catalog is an offline directory snapshot, not a live availability test; see [`data/dns-catalog/README.md`](../data/dns-catalog/README.md).

### Repository Explorer and OSINT

Public GitHub repository search uses the unauthenticated REST API. Search text is sent to GitHub, and its unauthenticated rate limit applies. OSINT Web performs bounded public metadata lookups; it is not an exploit or broad port-scanning tool. Results are evidence indicators, not proof of service health, ownership or physical location.

### Auth and remote services

The account gate protects Simulate and Tools. Supabase stores owner-scoped lab rows and bounded diagnostic telemetry. Redis gateway calls require a user JWT and use per-user key namespaces. The former TOTP/AAL2 app gate was removed; migration history remains, while owner-scoped RLS stays active. Read [Supabase setup and security](../supabase/README.md) before backend changes.

### Local data and diagnostics

DataStore persists preferences. Technical diagnostics use bounded event labels and trace IDs; remote uploads require consent. Never log secrets, passwords, terminal input/output, target names, resolver addresses, files or network payloads.

### PRoot and workstation

ARM64 PRoot execution supports Debian, Ubuntu and Alpine rootfs lifecycles, an interactive PTY shell, package mirror configuration and a package-based XFCE4/TigerVNC/noVNC route. PRoot shares the Android/Linux host kernel and is not a VM or kernel isolation boundary.

Native Linux, an embedded X11 server and VMStation do not have registered runnable engines. Capability, workspace and display-session scaffolding does not establish that those runtimes execute. `/dev/kvm` visibility alone is not proof that the app can use KVM. See [WorkStation architecture](WORKSTATION-ARCHITECTURE.md) and the [development handoff](DEVELOPMENT-HANDOFF.md).

## Security invariants

- Never bundle Supabase secret/service-role keys, Redis REST tokens, GitHub personal access tokens or provider secrets.
- Keep owner-scoped RLS enabled on user data. The public Supabase key is not caller authentication.
- Host checks are limited to systems the user owns or is authorized to inspect.
- Apply strict target validation, redirect limits and response bounds to network-facing metadata collection.
- Respect Android Private DNS and disclose when explicit DNS lookups send queried names to a public resolver.
- Keep diagnostic records privacy-filtered and avoid raw terminal content.

## Build and verification

There is no checked-in Gradle wrapper. CI installs JDK 17 and Gradle 8.13, then runs `gradle --no-daemon :app:assembleDebug`. The Android APK workflow and CodeQL workflow are linked in the [development handoff](DEVELOPMENT-HANDOFF.md). A successful build does not verify behavior on physical devices.
