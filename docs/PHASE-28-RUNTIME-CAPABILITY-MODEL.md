# Phase 28 — Runtime Capability Model

**Status:** Implemented as conservative reporting and manager/UI integration. No native runtime engine is claimed.

## Capability vocabulary

- AVAILABLE: the exact observation was established (for example, a node is visible or an adapter is registered).
- UNAVAILABLE: the app does not currently provide the required adapter.
- BLOCKED: an inspected operation/permission is denied in this app context.
- UNKNOWN: the capability was not probed or the available observation cannot establish it.

These states are not interchangeable. UNKNOWN is never promoted to AVAILABLE.

## Reported dimensions

- Supported Android ABIs and the process architecture reported by the runtime are separate values.
- /dev/kvm visibility is reported independently from advisory read/write permission flags.
- KVM API and VM-creation probes remain UNKNOWN; foundation inspection does not open the device, issue ioctls, or create a VM.
- PRoot ptrace remains UNKNOWN until a concrete adapter probes the actual app process and its disposable child using the same launch path it will ship.
- Adapter registration is an app-level observation and does not prove that the backend can start a workload.
- The capability report is attached to HybridRuntimeSnapshot and surfaced in Simulation Center.

## Readiness contract

- RuntimeFoundationStatus.READY means only that the declared Android minimum is met and app-private foundation directories are safely prepared.
- A workspace being prepared does not install a rootfs or mark a runtime READY.
- Runtime start is rejected unless foundation status is READY, the selected workspace is verified, the adapter is registered in the capability report, and adapter-specific inspection succeeds.
- Foundation READY means only that the declared Android minimum is met and app-private foundation directories are safely prepared; it does not imply KVM, PRoot, or any guest engine is usable.
- Only an adapter that independently confirms an active session can move the lifecycle to RUNNING.

## Deliberately not claimed

No KVM usability, PRoot ptrace availability, Native Linux privilege, QEMU support, guest isolation, or actual runtime execution is claimed by this phase. These require backend-specific probes and implementation. No DataStore keys, database, migration, or storage roots were changed.

## Primary technical references

- Android application sandbox: https://source.android.com/docs/security/app-sandbox
- Android SELinux: https://source.android.com/docs/security/features/selinux
- Android ABI guide: https://developer.android.com/ndk/guides/abis
- Linux KVM API: https://docs.kernel.org/virt/kvm/api.html
- Linux ptrace manual: https://man7.org/linux/man-pages/man2/ptrace.2.html
- PRoot manual: https://github.com/proot-me/PRoot/blob/master/doc/proot/manual.rst
