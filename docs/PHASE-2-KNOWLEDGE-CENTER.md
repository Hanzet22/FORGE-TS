# Phase 2 — Knowledge Center

## Objective
Deliver an offline-first learning area for MikroTik fundamentals and operating-system networking commands.

## Included in this milestone
- Native Jetpack Compose screen reachable from the Learn navigation item.
- Search across titles, summaries, explanations and examples.
- Category filters: All, MikroTik and OS Commands.
- Expandable reference cards with explanations and read-only examples.
- Static content works without a network connection.

## Safety boundary
- This module is a learning/reference library, not a terminal.
- It does not connect to routers, run commands, change device settings or store credentials.
- MikroTik examples are inspection-only.
- Network diagnostics must only be used on systems and networks the learner is authorized to inspect.

## Not included
- RouterOS remote management or command execution.
- Downloaded/synchronized lesson catalog.
- User accounts, cloud progress tracking or Supabase integration.
- Phase 1 Core Networking, Phase 3 DNS & Protocols, Phase 4 Repository Explorer, Phase 5 cloud/offline sync and Phase 6 release.

## Verification
The GitHub Actions Android Debug APK workflow is the build gate. A successful workflow confirms compilation and artifact creation; it does not replace on-device functional testing.
