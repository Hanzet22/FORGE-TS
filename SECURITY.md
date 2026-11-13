# Security Policy

## Supported versions

FORGE-TS has not published a stable release. Security fixes are currently targeted at the latest `main` branch.

| Version | Support |
| --- | --- |
| Latest `main` | Security fixes are reviewed and prioritized |
| Unreleased snapshots and older debug APKs | No security-update guarantee |

## Reporting a vulnerability

Please do not publish exploit details, credentials, personal data, or a reproducible attack in a public issue.

If GitHub's private vulnerability reporting is enabled for this repository, use the repository's **Security → Report a vulnerability** flow. If that option is unavailable, contact the maintainer through a private channel listed on the maintainer's GitHub profile and include only the minimum information needed to reproduce the issue.

A useful report includes:
- Affected commit, release or component.
- Impact and realistic attack conditions.
- Minimal reproduction steps or a proof of concept that does not access other people's data.
- Any relevant logs with secrets, tokens, personal data and device identifiers removed.

## Response expectations

The maintainer will aim to acknowledge a report within 7 calendar days and provide a status update within 30 calendar days. These are targets, not a guarantee. Coordinated disclosure timing should be agreed with the reporter.

## Scope

The policy covers the Android application, native components, GitHub Actions workflows, Supabase Edge Functions, database migrations, and the project's dependency and release-supply-chain configuration.
