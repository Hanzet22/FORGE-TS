# PublicDNS.xyz DNS Catalog Snapshot

This directory contains raw, checkpointed snapshots collected from PublicDNS.xyz for the FORGE-TS Custom DNS research catalog.

## Source inventory

- Directory: https://www.publicdns.xyz/
- Country index: https://www.publicdns.xyz/country/
- Provider directory: https://www.publicdns.xyz/
- Snapshot run date: 2026-11-11
- Directory lists 30 provider profile pages and 195 country/region pages.

## Saved data

- `raw/country-manifest.json`: 195 country/region page URLs.
- `raw/providers/batch-001.json` through `batch-003.json`: raw snapshots for all 30 provider profile pages.
- `raw/countries/batch-001.json` through `batch-020.json`: first-page country scrape checkpoints.
- `raw/countries/tavily-retry-001.json` through `tavily-retry-006.json`: fallback snapshots for country pages not returned in the first Exa pass.
- `raw/pagination/discovery-*.json`: pagination discovery snapshots from country first pages.
- `raw/pagination/pagination-manifest.json`: 619 pagination URLs discovered from 149 country pages.
- `raw/countries/pagination-batch-*.json`: pagination page snapshots, stored by batch. Some entries record missing extraction results; they are not silently discarded.

## Current scrape accounting

- Provider profiles: 30/30 raw page snapshots saved.
- Country/region first pages: 195/195 raw page snapshots saved across primary and fallback checkpoints.
- Pagination URLs discovered: 619.
- Pagination page bodies successfully saved: 543.
- Pagination page bodies not returned by the extraction providers at the time of this snapshot: 76. See each batch JSON's `missing` array; retry those URLs later.

## Checkpointing policy

Each scrape batch is committed as its own file on the `main` branch before moving to the next batch. A provider or extraction rate limit therefore does not erase previously committed snapshots. Failed and missing URLs are retained in the corresponding batch file so they can be resumed.

## Data quality and safety notes

- These are directory observations, not a guarantee that any resolver is active, recursive, publicly usable, or safe to configure.
- Country inventories may include ISP/customer resolvers, not only public recursive DNS providers.
- Reliability percentages are values displayed by the directory, not an independent uptime guarantee.
- A listed IP address does not imply DoH or DoT support. Encrypted DNS endpoints must be verified against the DNS operator's own documentation.
- Preserve the raw source values. Normalize and validate them in a separate derived dataset; do not overwrite the raw snapshots.
