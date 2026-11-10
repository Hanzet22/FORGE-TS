# Phase 3 — Persistent Runtime Registry & Recovery

**Status:** Source implementation committed; build verification is tracked separately by GitHub Actions.

## Persistence design

- AndroidX Preferences DataStore stores small registry metadata atomically in app-private storage.
- Each of the five environment IDs has independent lifecycle, workspace-prepared, and updated-at fields.
- The manager's latest event is persisted in the same DataStore.
- Runtime payloads, root filesystems, disk images, and guest data remain in isolated app-private workspaces.
- DataStore is used instead of adding a Room compiler/KSP toolchain because this is a small fixed-key metadata registry, not a relational dataset.

## Startup recovery

- Startup loads the persistent registry before the Simulation Center reports initialization complete.
- Previous STARTING, RUNNING, or STOPPING states become RECOVERY_REQUIRED.
- Recovery never assumes a process survived Android process death and never auto-restarts an environment.
- MikroTik RouterOS remains UNAVAILABLE regardless of previously stored state.
- Workspace metadata is trusted only after the expected directory passes canonical-path containment and read/write directory checks.
- Missing or unsafe workspaces are marked unprepared in the persisted registry.

## Workspace and lifecycle behavior

- Workspace preparation runs on the IO dispatcher and persists the prepared flag.
- Future lifecycle transitions are validated and persisted.
- RECOVERY_REQUIRED may only transition to STOPPED or FAILED; it cannot jump directly to RUNNING.
- Start requests remain rejected because no environment-specific execution adapter exists.
- No rootfs download, archive extraction, PRoot process, XFCE desktop, QEMU guest, or RouterOS CHR boot is introduced.

## Storage

- Registry: app-private DataStore file named forge_runtime_registry.preferences_pb.
- Workspaces: files/forge-runtime/<environment>/.
- Manager directories: files/forge-runtime/manager/{registry,state,logs}/.

## Deliberate scope choice

The app already includes androidx.datastore:datastore-preferences. Preferences DataStore is appropriate for this small, fixed registry and provides transactional edits without introducing Room compiler/KSP configuration. A relational database can be reconsidered if the registry later gains query-heavy history or relationships.

## Next

Phase 4 should focus on user-selected rootfs import and validation, including archive traversal rejection, bounded extraction, symlink checks, checksum verification, and rollback. Importing a rootfs must not imply it is safe or ready to execute.
