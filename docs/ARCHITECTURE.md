# FORGE-TS Architecture — Phase 0
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

## Security
No service-role keys or provider secrets in the APK. Do not store passwords or SSH private keys. Host checks are limited to systems the user is authorized to inspect.
