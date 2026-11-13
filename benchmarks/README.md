# FORGE-TS CodSpeed benchmark scope

The CodSpeed suite currently measures only two portable C++ helper paths:

- `BM_PtyDimensionsValidation`: validates the PTY dimensions helper.
- `BM_PtyArgvEnvPointerTable`: builds the null-terminated argv/env pointer tables.

These are microbenchmarks for the native PTY helper layer. They do **not** measure Android app startup, Compose rendering, memory pressure, PRoot process startup, Supabase request latency, Redis throughput, or end-to-end device performance.

The workflow currently uses simulation mode. The prior CodSpeed report flagged system-call-dominated measurement overhead and recommended walltime, but a walltime attempt on the current GitHub-hosted runner produced no compatible results. Do not interpret simulation values as walltime or end-to-end performance. A supported walltime runner is still needed for real wall-clock validation. Treat this suite as a native-helper baseline, not as a full application performance score.
