# FORGE-TS Development Handoff

**Snapshot date:** 2026-11-18  
**Snapshot commit:** Latest `main` revision; this handoff is refreshed with the current change.  
**Branch:** `main`  
**Audience:** the next developer or coding agent opening this repository without conversation history.

## Start here

Read this file first, then use [the docs index](INDEX.md) to find topic detail. Check out the current branch and source before relying on this dated snapshot. Source and workflow files are authoritative for behavior.

## Product at a glance

FORGE-TS is a native Android app (`com.HG.Forge.Ts`) written in Kotlin and Jetpack Compose. It targets API 36, supports API 29+, and builds ARM64 (`arm64-v8a`). The top-level tabs are Home, Learn, Network, Simulate, Tools and Settings. Simulate and Tools require a Supabase Auth session; other core screens remain available without sign-in.

The product combines local network utilities and educational references with bounded, disclosed remote lookups. It also has an ARM64 PRoot workstation path. Treat capability checks and “Coming Soon” labels as meaningful: not every workstation architecture target has an executable backend.

## Current implementation map

| Area | Current behavior | Main source locations | Limits to preserve |
|---|---|---|---|
| App shell and navigation | Six Compose tabs, animated transitions, saveable per-tab state, central auth gate for Simulate/Tools | `ui/ForgeApp.kt`, `ui/screens/ToolsScreen.kt`, `ui/screens/SimulationScreen.kt` | Runtime actions remain behind auth; do not bypass the shared gate. |
| EN/ID UI | Settings stores EN/ID preference locally; localization catalog contains 2,132 unique EN→ID source entries, including Protocol Center profiles and deep technical chapters | `ui/localization/ForgeLanguage.kt`, `ui/screens/SettingsScreen.kt` | Dynamic outputs and lesson explanations remain partly English. This is a Kotlin UI-string catalog, not Android resource-based localization. |
| Intro video | Full-screen `TextureView`; center-crop transform preserves source aspect ratio and re-centers after size changes; timeout fallback skips a stuck prepare/first frame | `ui/IntroVideoTextureView.kt`, `ui/ForgeApp.kt` | Center crop intentionally trims edges on mismatched screen aspect ratios. |
| Network | Local IPv4/IPv6/CIDR/VLSM tools, public IP lookup, one authorized host/TCP/TLS check, Android network snapshot | `data/network/`, `ui/screens/CoreNetworkingScreen.kt` | Host checker is a single selected endpoint check, not a port scanner. |
| DNS and protocol references | DoH lookups and offline protocol references | `data/network/DnsLookupRepository.kt`, `ui/screens/DnsProtocolsScreen.kt`, `ui/screens/ProtocolCenterScreen.kt` | DNS requests disclose queried names to their configured public resolver. |
| Public DNS directory catalog | Offline snapshot contains 132 country entries, 8,872 IPv4 observations and 54 IPv6 observations; 30 provider profiles | `app/src/main/assets/dns-catalog/`, `data/dns-catalog/README.md`, `ui/screens/DnsCatalogScreen.kt` | PublicDNS.xyz directory observations are not live-checked recursive resolvers. The snapshot has 195 country/region source pages; the derived asset currently has 132 countries with addresses. |
| OSINT Web | Passive DNS plus bounded public website/RDAP/TLS/header/mail and technology metadata checks | `ui/screens/OsintWebScreen.kt`, `ui/screens/OsintWebViewModel.kt`, `data/network/NativeWebCheckRepository.kt`, `data/network/Rdap*Repository.kt` | Not an exploit scanner; requests are bounded and private/local targets and unsafe redirects are rejected. Findings are indicators, not proof. |
| Learn | Searchable offline knowledge catalog, local labs, progress and 12 guided course tracks | `ui/screens/KnowledgeCenterScreen.kt` | The 12 tracks refer to existing lessons; lesson explanations are not yet fully translated. Labs are simulated and send no packets. |
| PRoot | ARM64 PRoot payload and Debian, Ubuntu and Alpine rootfs install/start lifecycle; interactive shell via native PTY | `data/simulation/Proot*`, `data/simulation/NativePtyEngine.kt`, `ui/screens/ProotTerminalPanel.kt`, `app/src/main/assets/proot-runtime/` | PRoot shares the Android host kernel and is not a VM or kernel isolation boundary. Device compatibility still needs on-device validation. |
| PRoot package mirrors | HTTPS mirror URL profiles can be saved by country; official Alpine mirror directory has 64 endpoints across 35 country/location labels in the checked asset | `data/simulation/ProotMirrorProfileStore.kt`, `data/simulation/ProotPackageMirrorManager.kt`, `app/src/main/assets/proot-runtime/alpine-mirrors.json` | Saved country profiles do not mean FORGE-TS hosts or verifies 132 country mirrors. Alpine status is a point-in-time upstream observation. |
| PRoot desktop | XFCE4 and TigerVNC bootstrap; loopback VNC/WebSocket path and in-app noVNC WebView | `data/simulation/ProotVncBootstrapSpec.kt`, `ui/screens/ProotVncViewerDialog.kt` | Requires a provisioned rootfs, installed packages and a running session. This is not embedded X11. |
| Native Linux / embedded X11 / VMStation | Capability, workspace and display-session scaffolding exist | `data/simulation/RuntimeFoundationSupervisor.kt`, `data/simulation/LinuxUserlandFoundationSupervisor.kt`, `data/simulation/DisplaySessionController.kt`, `data/simulation/MagicDeskX11PlatformGate.kt` | No native Linux desktop engine, embedded X11 renderer/host bridge, or VM/KVM/QEMU execution adapter is registered. Treat these modes as unavailable/planned. |
| Auth | Password-based Supabase Auth; encrypted Android Keystore session storage; lifecycle and recovery flows | `data/remote/SupabaseAuthClient.kt`, `data/remote/ForgeAuthSessionStore.kt`, `ui/screens/AuthGateScreen.kt` | MFA/TOTP/AAL2 app enforcement was removed. Historical MFA migrations/docs remain; do not infer it is active. |
| Diagnostics | Local technical logs, trace IDs, bounded queued uploads, optional remote consent, Supabase ingest functions; first-launch consent shares the root EN/ID language state with the app shell | `data/sync/`, `ui/DiagnosticConsentGate.kt`, `ui/localization/ForgeLanguage.kt`, `supabase/functions/forge-diagnostic-ingest/` | Remote upload remains opt-in; local logging remains enabled. Do not log secrets, passwords, terminal input/output, target domains, resolver addresses, files or network payloads. |

#### Startup consent and localization wiring

`MainActivity` owns one remembered `ForgeLanguageState` for the Compose activity tree and provides it through `LocalForgeLanguage` around both `ForgeApp` and `DiagnosticConsentGate`. This is required because the first-launch consent dialog calls `forgeText()`; placing the gate outside the provider previously caused `IllegalStateException: ForgeLanguageState was not provided` on a fresh install. Keep the fail-fast default in `LocalForgeLanguage`; do not mask missing providers with a fallback language. Language changes continue to update descendants through the same observable state instance.

## Important workstation distinctions

- **PRoot** is implemented for ARM64 distributions and can host a Linux userspace shell. It uses the Android/Linux host kernel.
- **XFCE4 through VNC** is a package/bootstrap and loopback display path that can run when the device/rootfs/packages cooperate. It is separate from native XFCE and X11.
- **Native Linux/XFCE** is a product target, not an executable backend in this app.
- **Embedded X11** has capability/admission scaffolding only; no in-process X server is integrated.
- **VMStation** has registry, resource and KVM-visibility groundwork; no VM engine is registered. `/dev/kvm` visibility alone would not prove usable KVM.
- RouterOS image staging/import is not a running RouterOS guest.

See [WorkStation architecture](WORKSTATION-ARCHITECTURE.md), [feasibility review](WORKSTATION-DISPLAY-FEASIBILITY.md), [foundation audit](WORKSTATION-FOUNDATION-AUDIT.md) and [PTY architecture](PTY-ENGINE-ARCHITECTURE.md) for detail. Some older sections in those files are historical; compare them with this table and the live source.

## Repository map

```text
app/src/main/java/com/HG/Forge/Ts/
  data/network/       local calculators and bounded network repositories
  data/preferences/   DataStore preferences
  data/remote/        Supabase Auth, records and Redis gateway clients
  data/simulation/    PRoot, PTY, runtime registry and simulation adapters
  data/sync/          diagnostics queue, storage and upload workers
  ui/                 app shell, localization, theme and Compose components
  ui/screens/         app feature screens and view models
app/src/main/assets/  DNS snapshots, PRoot payload metadata, licenses and other bundled data
app/src/main/cpp/     native PTY and packaged runtime build sources
data/dns-catalog/     immutable PublicDNS.xyz research snapshots and builder
supabase/             Edge Functions, config and SQL migrations
.github/workflows/    APK, CodeQL, native PRoot and Supabase CI workflows
docs/                 architecture, privacy, feature history and operational references
```

## Build and CI

- There is no checked-in Gradle wrapper. GitHub Actions installs JDK 17 and Gradle 8.13, then runs `gradle --no-daemon :app:assembleDebug`.
- Workflow: `.github/workflows/android-debug.yml`; output artifact is `FORGE-TS-debug-apk` with 14-day retention.
- CodeQL workflow: `.github/workflows/codeql.yml`; it builds Java/Kotlin and C/C++ manually before analysis.
- Snapshot CI for `411e67f`: [Android Debug APK run 44254561194](https://github.com/Hanzet22/FORGE-TS/actions/runs/44254561194) passed; [CodeQL run 44254561146](https://github.com/Hanzet22/FORGE-TS/actions/runs/44254561146) passed. No test suite was run for that change.
- Confirm current CI before claiming a newer revision is green. Build success does not establish on-device runtime compatibility.

## Supabase, Redis and secrets

Supabase functions are `forge-redis-gateway`, `forge-crash-ingest` and `forge-diagnostic-ingest`. Database migrations and function code live under `supabase/`. Read [Supabase setup/security notes](../supabase/README.md) before touching backend configuration.

Never place service-role keys, Redis REST tokens, Resend credentials or other secrets in Android code, `BuildConfig`, checked-in assets or logs. The Android Supabase publishable key is public configuration, not authentication. Maintain user JWT checks, owner-scoped RLS, and per-user Redis key prefixes. Do not reuse unrelated connected Supabase projects.

## Safety and data boundaries

- Keep OSINT checks passive/bounded and authorized; do not add exploitation, broad port scans, credential collection or stealth behavior.
- Keep diagnostic logs useful for lifecycle/error triage while redacting user data and secrets. Store raw terminal output only in the interactive terminal buffer, not diagnostics.
- Distinguish directory snapshots from verified service availability. Distinguish successful compilation from runtime/device verification.
- Respect Android Private DNS and explain third-party resolver disclosure for explicit DNS lookups.

## Next work / open questions

1. Finish ID translations for dynamically generated UI and lesson explanations, or move the UI into maintained Android string resources with a clear translation workflow.
2. Perform focused on-device acceptance for PRoot on ARM64 devices across Android API levels; CI cannot establish tracing, package DNS or VNC behavior on physical devices.
3. Keep native Linux, embedded X11 and VMStation unavailable until a concrete adapter is packaged, registered, and verified on supported devices.
4. Revisit downloaded rootfs provenance, mirror availability and integrity policy when changing acquisition behavior.
5. Keep the phase archive and operational docs aligned when a new capability changes status.

## Change checklist for future agents

1. Read this file and the relevant topic documents before changing runtime, auth, OSINT, diagnostics or data.
2. Inspect the current source and branch; do not assume this snapshot is still current.
3. Preserve privacy/safety boundaries and distinguish implemented, partial and planned behavior.
4. Update this handoff, `docs/INDEX.md`, README and topic docs when behavior or status changes.
5. Run only checks appropriate to the task and user request; report outcomes and unverified device behavior plainly.
