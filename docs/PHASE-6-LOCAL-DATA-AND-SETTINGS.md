# Phase 6 — Local Data & Settings

> **Status context:** This document records phase scope, design assumptions or acceptance criteria and may describe work that was later changed. The live implementation state is summarized in the [development handoff](DEVELOPMENT-HANDOFF.md); verify current source and CI before claiming a feature is available.

## Delivered scope
- Persist the preferred DNS record type using Android DataStore.
- Restore the saved selection when DNS tools are opened.
- Keep the preference local to the device.

## Boundaries
- No DNS query history, host history, credentials or private keys are stored.
- DataStore contains only a non-sensitive UI preference.
- Cloud backup/export is not enabled by this milestone.
