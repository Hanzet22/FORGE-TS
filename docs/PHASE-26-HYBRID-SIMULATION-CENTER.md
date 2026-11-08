# Phase 26 — Hybrid Simulation Center

**Status:** Initial application implementation committed; cloud integrations are gated on dedicated infrastructure provisioning.

## Goal

Add a first-class Simulation tab with automatic connectivity awareness and three independent labs:

1. Redis key/value and TTL learning lab.
2. Supabase table/CRUD learning lab.
3. Loopback localhost test plus deterministic simulator.

## Implemented in this phase

- Added a dedicated **Simulate** bottom-navigation tab.
- Added a connectivity observer using Android `ConnectivityManager.NetworkCallback`.
- Treats internet as validated only when the active network reports both `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`.
- Added an in-memory Redis-like offline engine supporting bounded `SET`, `GET`, `DEL`, TTL and key listing.
- Added an in-memory Supabase-like sample table supporting `INSERT`, `SELECT`, `UPDATE`, and `DELETE` at the engine layer.
- Added a localhost HTTP health server bound specifically to `127.0.0.1`, using an ephemeral port, plus a client request and deterministic simulator response.
- Local lab records are temporary and are not uploaded.
- The loopback listener is stopped when the screen is disposed.

## Automatic mode policy

Network presence is not equivalent to service health. The application must keep two separate signals:

- **Internet status:** Android's validated default network.
- **Service status:** an actual, bounded health request to the specific configured backend.

When internet is unavailable, remote requests must not be attempted and local simulation remains available. When internet is available, a configured service can be checked; if that service is unconfigured or unhealthy, its module must report that fact and continue with its local engine. Do not display simulated output as a real server response.

## Cloud integration gates

### Supabase

A dedicated Supabase project is required. The existing FurFinder project must not be reused.

- Account discovery found organization `HyperGaruda Base` (`opemnrfclxgeaqepqnok`).
- The only currently visible project was `furfinder`, inactive.
- The Supabase project creation estimate for the discovered organization was **$0/month** at the time checked.
- Project creation has not been performed; organization selection still needs the user's explicit confirmation.
- Before enabling online table access, create a dedicated lab schema/table with RLS and least-privilege policies.
- Android clients may use a publishable key, but must never contain a Supabase secret/service-role key.
- User-owned rows require an authenticated identity and ownership policies; do not ship an unrestricted public CRUD table.

### Redis / Upstash

- The currently visible Upstash database is `hyperai-security-redis`; it belongs to another project and must not be reused for FORGE-TS.
- A dedicated free database creation attempt was rejected because the account already has its one free database. The API reported that another database requires a payment method and pay-as-you-go.
- No paid plan, billing change, or reuse of the unrelated database was performed.
- Redis must sit behind a server-side FORGE-TS API gateway. Redis is the data store, not itself the API gateway.
- The Android APK must never contain the Upstash REST token or Redis password.
- The online Redis module remains pending a dedicated database and protected gateway endpoint.

## Loopback boundaries

- Bind only to IPv4 loopback (`127.0.0.1`), never `0.0.0.0`.
- Use an ephemeral port and a narrow `GET /health` endpoint.
- Do not expose a remote listener, scan other ports, or probe other devices.
- The simulator path opens no socket.

## Validation still required

- GitHub Actions Android debug build and CodeQL.
- On-device test on Android 10+.
- Verify offline behavior with Wi-Fi and mobile data disabled.
- Verify validated-network transitions and service-specific failure states.
- Verify loopback server stop/restart and screen-disposal cleanup.
- Accessibility review with TalkBack and narrow-screen layout review.

## Primary references

- Android: [Read network state](https://developer.android.com/develop/connectivity/network-ops/reading-network-state)
- Android: [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager)
- Android: [Build an offline-first app](https://developer.android.com/topic/architecture/data-layer/offline-first)
- Supabase: [Securing your data](https://supabase.com/docs/guides/database/secure-data)
- Supabase: [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security)
- Upstash: [Redis REST API](https://upstash.com/docs/redis/features/restapi)
- Upstash: [Redis pricing](https://upstash.com/pricing/redis.md)

## Important distinction

The first committed version provides functional local learning engines and a real device-local loopback test. It does **not** claim that remote Redis or Supabase is connected. Those connectors must be enabled only after the dedicated cloud resources, RLS model, and server-side gateway are provisioned and verified.
