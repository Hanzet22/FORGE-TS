# IP & Network Registry Intelligence

FORGE-TS resolves the target's ordinary A/AAAA DNS answers and performs at most one read-only IP RDAP lookup for the first returned literal address.

## Displayed fields
- Resolved IP address and IP version
- Registry network name and handle, when returned
- Registered address range
- Country/region code supplied by the registry
- RDAP service that supplied the record

## Safety and privacy boundaries
- The lookup is limited to one IP address per inspection.
- Private, loopback, link-local, multicast, shared, and selected reserved/documentation ranges are rejected.
- HTTPS only; redirects are bounded and must remain HTTPS.
- Connect/read timeouts are bounded and the response body is capped at 128 KiB.
- Registrant contact/vCard data is not collected.
- The public IP address is sent to rdap.org and may be redirected to an authoritative regional registry. This is a third-party request.
- Registry allocation metadata does not prove physical server location, current hosting provider, or operational ownership.
- IP RDAP does not consistently return an ASN. FORGE-TS does not invent an ASN when the registry response omits one. Dedicated ASN enrichment is a separate future integration.

## Relationship to existing signals
This complements DNS A/AAAA answers and passive HTTP server-identification and CDN/WAF response-header hints. It does not perform port scanning, traceroute, reverse-IP enumeration, or active service fingerprinting.
