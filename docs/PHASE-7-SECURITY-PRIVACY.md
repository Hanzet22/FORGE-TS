# Phase 7 — Security & Privacy Center

> **Status context:** This document records phase scope, design assumptions or acceptance criteria and may describe work that was later changed. The live implementation state is summarized in the [development handoff](DEVELOPMENT-HANDOFF.md); verify current source and CI before claiming a feature is available.

## Delivered scope
- In-app disclosure of external providers and the data each feature sends.
- Clear distinction between local-only features and network-backed features.
- Safety boundaries for authorized host checks and read-only command references.
- Reminder that external provider retention and privacy practices are outside FORGE-TS control.

## Security boundaries
- No Supabase secret/service-role key, GitHub personal access token or provider secret in the APK.
- No shell execution, port-range scanning, exploitation or credential collection.
- The Android app requests internet and active-network-state permissions only.
