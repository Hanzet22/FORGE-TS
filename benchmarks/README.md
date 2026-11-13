# FORGE-TS CodSpeed benchmark scope

The CodSpeed suite currently measures only two portable C++ helper paths:

- `BM_PtyDimensionsValidation`: validates the PTY dimensions helper.
- `BM_PtyArgvEnvPointerTable`: builds the null-terminated argv/env pointer tables.

These are microbenchmarks for the native PTY helper layer. They do **not** measure Android app startup, Compose rendering, memory pressure, PRoot process startup, Supabase request latency, Redis throughput, or end-to-end device performance.

The CodSpeed workflow uses walltime mode because the previous simulation run reported system-call-dominated measurements and recommended walltime validation. Compare walltime runs only against other walltime runs. Treat this suite as a native-helper baseline, not as a full application performance score.
