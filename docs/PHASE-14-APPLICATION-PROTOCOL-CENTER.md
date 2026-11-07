# Phase 14 — Application Protocol Center

## Goal
Add a standalone Application Protocol Center tab to FORGE-TS. It complements the existing Protocol Center rather than replacing or duplicating its network-layer catalog.

## Dedicated tab
The DNS & Protocols screen now exposes four sections:
1. DNS Lookup
2. Protocol Library (legacy reference)
3. Protocol Center (network and infrastructure catalog)
4. Application Protocols (this phase)

The new section has an independent search field, horizontally scrollable category filters, searchable protocol cards and a dedicated detail view for each entry.

## Initial catalog
Fourteen entries are included:
- HTTP/1.1
- HTTP/2
- HTTP/3
- HTTPS
- TLS 1.3
- WebSocket
- Server-Sent Events (SSE)
- WebDAV
- REST
- GraphQL
- gRPC
- SOAP 1.2
- JSON-RPC 2.0
- Protocol Buffers

## Technical detail model
Each entry documents architecture and component roles; packet, frame and message model; operation lifecycle; security model and trust boundaries; performance characteristics; deployment and interoperability; troubleshooting methodology; advantages; limitations and operational hurdles; and primary specifications.

## Important classification distinctions
- HTTP/1.1, HTTP/2 and HTTP/3 are HTTP protocol versions with shared semantics but different framing and transport mappings.
- HTTP/2 runs over TCP; HTTP/3 maps HTTP semantics over QUIC, which uses UDP and integrates TLS 1.3.
- HTTPS means HTTP protected by TLS; it is not an independent HTTP message protocol.
- TLS 1.3 is a security protocol used by application protocols, not an application protocol.
- WebSocket is bidirectional; SSE is a browser-facing server-to-client event stream over HTTP.
- WebDAV extends HTTP with authoring and resource-management methods.
- REST is an architectural style, not a wire protocol.
- GraphQL is a query language and execution specification; its transport conventions are specified separately.
- gRPC is an RPC framework/protocol ecosystem commonly using HTTP/2 and Protocol Buffers.
- SOAP 1.2 is a messaging framework with bindings to underlying transports.
- JSON-RPC 2.0 is an RPC message protocol whose transport is not fixed by the core specification.
- Protocol Buffers is a serialization format and interface-definition language, not a network transport protocol.

## Primary references
- HTTP semantics RFC 9110 — https://www.rfc-editor.org/rfc/rfc9110
- HTTP caching RFC 9111 — https://www.rfc-editor.org/rfc/rfc9111
- HTTP/1.1 RFC 9112 — https://www.rfc-editor.org/rfc/rfc9112
- HTTP/2 RFC 9113 — https://www.rfc-editor.org/rfc/rfc9113
- HTTP/3 RFC 9114 — https://www.rfc-editor.org/rfc/rfc9114
- QUIC RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000
- QUIC-TLS RFC 9001 — https://www.rfc-editor.org/rfc/rfc9001
- TLS 1.3 RFC 8446 — https://www.rfc-editor.org/rfc/rfc8446
- WebSocket RFC 6455 — https://www.rfc-editor.org/rfc/rfc6455
- WebSocket over HTTP/2 RFC 8441 — https://www.rfc-editor.org/rfc/rfc8441
- WebSocket over HTTP/3 RFC 9220 — https://www.rfc-editor.org/rfc/rfc9220
- Server-Sent Events — WHATWG HTML Living Standard: https://html.spec.whatwg.org/multipage/server-sent-events.html
- WebDAV RFC 4918 — https://www.rfc-editor.org/rfc/rfc4918
- GraphQL specification releases and working drafts — https://spec.graphql.org/
- gRPC HTTP/2 protocol — https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
- SOAP 1.2 — W3C Recommendations: https://www.w3.org/TR/soap12-part1/ and https://www.w3.org/TR/soap12-part2/
- JSON-RPC 2.0 — https://www.jsonrpc.org/specification
- Protocol Buffers — https://protobuf.dev/

## Scope and safety
This phase adds offline educational reference content only. It does not implement an HTTP client, API tester, WebSocket client, RPC executor, credential store or remote service configurator. No provider secrets are embedded in the APK. Existing authorized-host-check boundaries remain unchanged.

## Verification
- Source catalog target: 14 entries.
- The detail screen is separate from the existing network Protocol Center.
- Build/CI status must be read from the GitHub Actions run associated with the resulting commit; source changes alone are not evidence of a successful build.
- No direct Android device test is claimed unless one is actually performed.
