# Phase 42 — Offline DNS Provider Catalog

## Delivered scope

- Adds a bundled, offline-readable provider directory derived from the saved PublicDNS.xyz provider snapshots.
- Normalizes 30 provider profiles into `app/src/main/assets/dns-catalog/providers.json`.
- Adds a searchable DNS Catalog tab under Tools, with provider-name/IP search and IPv4/IPv6 filters.
- Keeps each provider's PublicDNS.xyz profile URL alongside the listed addresses.
- Marks all entries as directory-listed and not live-tested. The catalog is reference data only; it does not select a resolver or modify Android, app, or PRoot DNS settings.
- A profile whose raw extraction was corrupted is retained from the earlier normalized inventory and explicitly attributed in its source metadata.

## Data handling

Raw scrape checkpoints remain under `data/dns-catalog/raw/` and are not overwritten by the normalized app asset. Country-directory observations are not promoted into this provider catalog because country listings may contain ISP/customer resolver addresses and do not establish public recursive-resolver availability.

## Validation boundary

The Android Debug APK workflow is the build gate. CodSpeed's existing PTY benchmarks are unrelated to this catalog UI; no DNS-catalog performance benchmark is claimed. Provider reachability, recursive behavior, privacy policy, and physical-device UI behavior require separate verification before an address is recommended for configuration.
