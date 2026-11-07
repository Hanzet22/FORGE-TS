# Phase 1 — Core Networking

## Included tools
- **CIDR Calculator:** local IPv4/IPv6 calculations, prefix, network, IPv4 subnet mask, broadcast (where applicable), usable IPv4 host range/count, total addresses and binary representation.
- **VLSM Planner:** local IPv4 subnet allocation from a base prefix using named host requirements, largest-first sorting, block alignment and capacity checks.
- **Network IP Allocation:** estimate IPv4 capacity for PAN, LAN, WLAN and VLAN segments from device count, reserved addresses and growth allowance; includes IPv6 /64 planning guidance.
- **What Is My IP:** HTTPS lookup of the public IP address through api.ipify.org. The provider is shown in the UI.
- **Host Checker:** one DNS lookup and one TCP connection attempt to a user-selected host and port. Optional TLS verification uses Android's default HTTPS certificate and hostname verification.

## UI
- Native Jetpack Compose and Material 3.
- Network tools are available from the bottom navigation.
- CIDR, VLSM, IP Plan, My IP, and Host are separate tabs within the Core Networking screen.
- Input validation and visible loading/error/result states are included.

## Privacy and safety
- CIDR calculations remain on-device.
- Public IP lookup sends the request to the named provider; FORGE-TS does not store an IP history.
- Host checks are limited to one selected port per request and should only be used on systems the user owns or is authorized to inspect.
- VLSM and IP allocation are local planning calculations only.
- IPv6 has no broadcast address; IPv4 /31 point-to-point and /32 host-route semantics are handled separately.
- No mass port scanning, exploitation, remote shell, or credential handling.
- Network requests run off the main thread and have bounded timeouts.

## Verification
GitHub Actions Android Debug APK workflow is triggered by the commits. A successful workflow is required before treating the implementation as build-verified.
