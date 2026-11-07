# Phase 17 — Mail Security Intelligence

## Goal
Add a native, passive DNS review of public domain-level email security configuration. The checks use Google Public DNS-over-HTTPS and do not send email, authenticate to mailboxes, connect to mail servers, or attempt delivery.

## Checks
- MX: observes published mail-exchanger records.
- SPF: searches the apex TXT answers for `v=spf1` and flags multiple detected policies.
- DMARC: queries `_dmarc.<domain>`, identifies a `v=DMARC1` record and reports a parsed `p=` policy when present.
- DKIM: checks four common selector guesses (`selector1`, `selector2`, `google`, `default`) for TXT answers.
- MTA-STS: checks `_mta-sts.<domain>` for the `v=STSv1` marker.
- SMTP TLS Reporting: checks `_smtp._tls.<domain>` for the `v=TLSRPTv1` marker.

## Interpretation limits
- This is a DNS indicator review, not a complete mail-security audit or deliverability test.
- DKIM selector names are chosen by each mail provider/domain administrator. The four guesses are not exhaustive; no result does not prove DKIM is absent.
- SPF parsing is intentionally limited to record detection and duplicate-policy warning; it does not evaluate the complete SPF lookup/redirect/include budget.
- DMARC policy display is a basic TXT parse, not full RFC validation or aggregate-report analysis.
- MTA-STS policy-file retrieval and validation are not implemented.
- TLS-RPT reporting destinations and report delivery are not tested.
- DNS answers can be cached or temporarily inconsistent; resolver results are not proof of actual mail-server behavior.

## Native website inspection network diagnostics
- The primary bounded website request now uses a 12-second connect timeout and 12-second read timeout instead of seven seconds.
- The ViewModel classifies common connection timeout, DNS resolution and TLS negotiation failures into separate user-facing explanations.
- A TCP connect timeout after successful DNS resolution is not presented as proof that the target website is offline or that its certificate is invalid. The cause may be the destination service, network route, filtering, CDN/edge behavior or transient connectivity.
- This timeout adjustment and diagnostic classification improve resilience and clarity; they cannot force an unreachable destination to accept connections.
- The existing response-size caps, public-host validation, manual redirect validation, HTTPS downgrade block and no-credentials policy remain in effect.

## Privacy and safety
- DNS queries are sent to Google Public DNS; domain names and queried record types are visible to that resolver.
- No email messages are sent and no mailbox credentials or personal mailbox data are collected.
- Inspect only domains you own or are authorized to assess.

## Verification
Android Debug APK and CodeQL workflows must pass before this phase is marked build-verified. Runtime behavior should be checked on-device against a domain with known mail DNS records and against a network where the target HTTPS endpoint is reachable.
