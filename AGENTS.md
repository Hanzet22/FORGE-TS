# FORGE-TS agent instructions

Before changing this repository, read [the development handoff](docs/DEVELOPMENT-HANDOFF.md) and [the documentation index](docs/INDEX.md). The handoff is the current compact map of product behavior, architecture, runtime limits, build/CI and privacy boundaries.

## Sources of truth

1. Current source code, checked-in assets, database migrations and workflow definitions establish what is implemented.
2. `docs/DEVELOPMENT-HANDOFF.md` summarizes the latest verified baseline and points to implementation files.
3. Phase documents preserve historical scope and acceptance criteria. Do not treat planned work in a phase document as implemented without checking the current source.
4. If code and a document disagree, verify the code and CI, then update the handoff, README and affected topic document in the same change.

## Working rules

- Keep the app's package/application ID and Android 10 minimum unless the user requests a platform change.
- Preserve explicit unavailable/coming-soon states. Do not report native Linux, embedded X11 or VMStation as runnable when no registered engine exists.
- Keep PRoot, native Linux and virtual machines as distinct execution models.
- Keep secrets out of Android source, assets, build config and Git. Keep credentials, terminal input/output, target names and payloads out of diagnostics.
- Match existing Kotlin, Compose, repository and adapter patterns. Avoid creating a parallel runtime registry or service when one already exists.
- Do not add or run test suites unless the user asks. Prefer a focused build/CI check when requested or needed to confirm compilation; report exactly what ran.
- Do not create a pull request or push to a remote unless the user explicitly asks. Direct-to-main authorization is per-task and must be confirmed by current instructions.
- Update the docs index and relevant operational docs whenever a change alters product status, setup, data sources, privacy, build steps or runtime compatibility.

## Handoff freshness

The implementation snapshot in `docs/DEVELOPMENT-HANDOFF.md` is dated and tied to a commit. Refresh its commit, date and CI references after material changes. Avoid copying its status table into another document; link to it instead.
