# FORGE-TS Architecture — Phase 0, Phase 1 and Phase 2
## Operating model
Local-first, online and offline. Local utilities should work without connectivity. Shared catalog content is synchronized into local storage when a connection is available.

## Layers
- Presentation: Jetpack Compose and Material 3.
- State: MVVM / ViewModels.
- Data: repositories coordinating Room, DataStore and Ktor.
- Remote boundary: Supabase behind an interface.
- Background work: WorkManager.

## Data responsibilities
- Room: structured offline cache and local records.
- DataStore: lightweight preferences.
- Supabase: persistent remote source of truth for shared catalog data.
- Upstash Redis: optional future cache, never the source of truth; deferred beyond Phase 0.

## Knowledge Center (Phase 2)
- MikroTik Academy entries are educational references and use read-only inspection examples.
- OS command entries explain common diagnostic commands; they are displayed as text and never executed by the app.
- Search and category filtering run locally on the device.

## Security
No service-role keys or provider secrets in the APK. Do not store passwords or SSH private keys. Host checks are limited to systems the user is authorized to inspect.

## Core Networking (Phase 1)
- CIDR calculation is local-only and supports IPv4 and IPv6 address/prefix input.
- Public IP lookup uses HTTPS and identifies the external lookup provider in the UI. The request necessarily exposes the connection's public IP to that provider; FORGE-TS does not maintain IP history.
- Host Checker performs a single DNS resolution and one user-selected TCP port check. Optional TLS checks use the platform HTTPS stack so certificate-chain and hostname verification remain enabled.
- Network operations run away from the main thread and use bounded timeouts.
- Host checks are intended only for systems the user owns or is authorized to inspect. No port-range scanning, exploitation, credential collection, or shell execution is provided.
