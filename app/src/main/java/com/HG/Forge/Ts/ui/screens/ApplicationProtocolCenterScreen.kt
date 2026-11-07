package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class ApplicationProtocolProfile(
    val name: String, val category: String, val layer: String, val classification: String,
    val overview: String, val architecture: String, val wireModel: String, val lifecycle: String,
    val security: String, val performance: String, val deployment: String, val troubleshooting: String,
    val advantages: String, val limitations: String, val references: String
)

private val applicationProtocolProfiles = listOf(
ApplicationProtocolProfile("HTTP/1.1","Web transfer","Application over TCP","Application protocol",
"Stateless request/response semantics for transferring representations and metadata between clients and servers.",
"A client selects an origin, resolves its name, establishes a TCP connection and optionally negotiates TLS. Intermediaries can act as proxies, gateways or caches.",
"Text-based start line, header fields, an empty line and an optional message body. Framing is determined by rules such as Content-Length and Transfer-Encoding.",
"The client sends a request method, target and fields. The server returns a status line, fields and optional content. Persistent connections can carry multiple exchanges.",
"Plain HTTP does not provide confidentiality or peer authentication. HTTPS deployments require TLS certificate validation, safe request parsing and careful proxy trust.",
"Connection reuse reduces setup overhead, but sequential request handling and connection limits can constrain concurrency. Compression and caching affect resource use.",
"TCP is the transport. Common service ports are 80 and 443 when protected by TLS. Reverse proxies and caches must agree on message framing and forwarding rules.",
"Check DNS, TCP reachability, TLS, request target, Host/:authority mapping, status code, framing headers, proxy behavior and server logs in that order.",
"Mature interoperability, simple debugging, broad tooling and compatibility with intermediaries.",
"Text framing has overhead; careless parsing of ambiguous message lengths can create request-smuggling risks. Plain HTTP is not suitable for sensitive traffic.",
"RFC 9110 (semantics), RFC 9111 (caching), RFC 9112 (HTTP/1.1 syntax). https://www.rfc-editor.org/rfc/rfc9112"),
ApplicationProtocolProfile("HTTP/2","Web transfer","Application over TCP","Application protocol version",
"An optimized HTTP version that preserves HTTP semantics while using binary framing and multiplexed streams.",
"A connection contains multiple concurrent streams. HPACK compresses header fields. TLS deployments negotiate h2 using ALPN.",
"Binary frames carry stream identifiers, flags and type-specific payloads. HEADERS and DATA frames form request and response messages.",
"A connection preface and SETTINGS exchange establish parameters. Streams open, carry frames, and close independently or through reset.",
"HTTPS deployments use TLS and certificate validation. Header compression state, stream limits and connection coalescing require careful implementation.",
"Multiplexing avoids HTTP/1.1 application-level serialization across streams. TCP packet loss can still delay delivery across the connection.",
"HTTP/2 normally uses TCP; TLS ALPN identifier is h2. Proxies must correctly translate connection-specific behavior and stream semantics.",
"Verify ALPN, connection preface, SETTINGS, stream IDs, flow-control windows, GOAWAY/RST_STREAM and proxy protocol support.",
"Multiplexing, header compression and fewer connection setups for many requests.",
"TCP-level head-of-line blocking remains; binary framing is less human-readable and flow control adds complexity.",
"RFC 9113; HPACK RFC 7541; ALPN RFC 7301. https://www.rfc-editor.org/rfc/rfc9113"),
ApplicationProtocolProfile("HTTP/3","Web transfer","Application over QUIC/UDP","Application protocol version",
"Maps HTTP semantics onto QUIC, providing independent streams and TLS 1.3 integrated into the transport.",
"HTTP/3 uses QUIC streams and QPACK header compression. QUIC manages connection IDs, encryption, loss recovery and stream flow control.",
"HTTP/3 frames are carried on QUIC streams. Control streams exchange SETTINGS; QPACK encodes fields.",
"Endpoints establish QUIC with TLS 1.3, negotiate h3, open request streams and exchange HTTP frames.",
"QUIC integrates TLS 1.3. Certificate validation, origin handling, QPACK limits and implementation updates remain essential.",
"Independent streams avoid TCP-level cross-stream head-of-line blocking. Connection migration can help mobility, but UDP path quality matters.",
"Uses QUIC over UDP, commonly port 443. Networks, load balancers and firewalls need UDP support; HTTP/2 or HTTP/1.1 fallback may be used.",
"Check UDP reachability, QUIC version negotiation, TLS handshake, ALPN h3, transport parameters, migration and fallback behavior.",
"Stream independence, integrated security, reduced handshake latency and connection migration.",
"UDP filtering can prevent use; packet inspection and debugging are more complex; gains depend on network conditions.",
"RFC 9114 (HTTP/3), RFC 9000 (QUIC), RFC 9001 (QUIC-TLS), RFC 9204 (QPACK). https://www.rfc-editor.org/rfc/rfc9114"),
ApplicationProtocolProfile("HTTPS","Web security","HTTP protected by TLS","Secure application scheme and deployment",
"HTTP communication protected by TLS. HTTPS combines HTTP semantics with a secure transport channel; it is not a separate HTTP message protocol.",
"The client resolves the origin, connects to the appropriate transport, validates the server certificate and exchanges HTTP messages inside the protected channel.",
"HTTP message structure depends on the negotiated version. TLS records protect application data; HTTP/3 uses QUIC with TLS 1.3 rather than TLS records over TCP.",
"TLS handshake authenticates the server and establishes traffic keys. HTTP is negotiated, then requests and responses flow until connection closure.",
"Certificate chain and hostname verification are essential. HSTS, secure cookies, modern TLS configuration and correct origin policy strengthen deployment.",
"TLS adds handshake and cryptographic work; session resumption and modern versions reduce overhead. HTTP version and network conditions also affect performance.",
"Common HTTPS port is TCP 443 for HTTP/1.1 and HTTP/2; HTTP/3 commonly uses UDP 443. Certificates must match the requested hostname.",
"Inspect DNS, destination reachability, certificate validity and SAN, trust chain, clock, ALPN, TLS alerts and application response separately.",
"Confidentiality, integrity and authenticated server identity for web traffic.",
"Misissued or expired certificates, disabled verification, weak TLS settings and mixed-content mistakes undermine protection. TLS is not end-to-end application encryption by itself.",
"HTTP semantics RFC 9110; TLS 1.3 RFC 8446; HSTS RFC 6797. https://www.rfc-editor.org/rfc/rfc8446"),
ApplicationProtocolProfile("TLS 1.3","Web security","Security layer below application data","Cryptographic protocol",
"Transport Layer Security version 1.3 protects application data with authenticated encryption and establishes session keys.",
"ClientHello and ServerHello negotiate parameters and key exchange. Certificates and CertificateVerify authenticate the server; Finished messages confirm handshake integrity.",
"Handshake messages negotiate versions, cipher suites, extensions and key shares. Application data is carried in authenticated encrypted records.",
"The peers negotiate parameters, authenticate as configured, derive traffic secrets and exchange protected application data. Alerts or closure end the session.",
"Use supported cipher suites, certificate validation, secure key storage and current libraries. 0-RTT data has replay considerations and must be restricted accordingly.",
"Fewer round trips than older TLS versions; cryptographic cost depends on hardware, key exchange, certificate chain and resumption.",
"Used by HTTPS, mail submission, APIs and many other application protocols. ALPN can negotiate the application protocol within TLS.",
"Check system time, trust store, hostname, certificate chain, SNI, ALPN, supported groups, handshake alerts and middlebox compatibility.",
"Modern cryptography, forward-secret key establishment and reduced handshake latency.",
"Requires certificate and key lifecycle management. TLS protects a channel, not data after it reaches an endpoint.",
"RFC 8446; deployment recommendations RFC 9325. https://www.rfc-editor.org/rfc/rfc8446"),
ApplicationProtocolProfile("WebSocket","Real-time web","Application framing over TCP or HTTP extended transports","Bidirectional communication protocol",
"Provides a persistent, full-duplex communication channel between a client and server after an opening handshake.",
"Classic WebSocket uses an HTTP opening handshake and then WebSocket framing. Extensions and subprotocols can be negotiated during the handshake.",
"Frames contain FIN, opcode, masking information, payload length and optional payload. Control opcodes include Close, Ping and Pong.",
"Client requests an upgrade; server accepts with the expected response. Both sides exchange messages independently until a closing handshake or failure.",
"Use wss for TLS-protected connections, validate Origin where applicable, authenticate sessions and enforce message size/rate limits.",
"Efficient for frequent two-way updates, but long-lived connections consume server state and need heartbeat, backpressure and reconnect policies.",
"Classic handshake is defined over HTTP/1.1; extensions support HTTP/2 and HTTP/3 bootstrapping. Common secure endpoint port is 443.",
"Inspect upgrade negotiation, Origin, subprotocol, proxy idle timeout, Ping/Pong, close codes, authentication expiry and reconnect storms.",
"Low-latency bidirectional messaging without repeated HTTP request headers.",
"Requires connection lifecycle management, load-balancer affinity or shared state, and careful authorization for each message or channel.",
"RFC 6455; HTTP/2 bootstrap RFC 8441; HTTP/3 bootstrap RFC 9220; WHATWG WebSockets API. https://www.rfc-editor.org/rfc/rfc6455"),
ApplicationProtocolProfile("Server-Sent Events (SSE)","Real-time web","HTTP response stream","Web API and event-stream format",
"A browser EventSource interface consumes a long-lived HTTP response formatted as text/event-stream for server-to-client updates.",
"The client opens an HTTP request and the server streams events. The browser manages reconnection and can send Last-Event-ID when reconnecting.",
"UTF-8 text lines can contain event, data, id and retry fields. A blank line dispatches an event; comment lines can act as keepalives.",
"Client requests an event stream. Server keeps the response open and emits events; client reconnects after interruption according to retry behavior.",
"Use HTTPS, authenticate the endpoint, authorize event subscriptions and avoid placing secrets in URLs. Apply origin and cache policy deliberately.",
"Simple one-way streaming works well for updates. Long-lived responses need connection capacity, heartbeat intervals and proxy buffering settings.",
"Uses HTTP; the browser EventSource API is defined by the WHATWG HTML Living Standard. Server-to-client only; client-to-server messages use another request.",
"Check Content-Type text/event-stream, response buffering, idle timeouts, cache headers, reconnect interval, event IDs and UTF-8 formatting.",
"Simple browser support, automatic reconnection and ordinary HTTP infrastructure.",
"One-way channel, text-only event framing and proxy buffering/connection limits can complicate high-volume use.",
"WHATWG HTML Living Standard — Server-sent events. https://html.spec.whatwg.org/multipage/server-sent-events.html"),
ApplicationProtocolProfile("WebDAV","Web transfer","HTTP extension","HTTP protocol extension",
"Extends HTTP with methods and properties for collaborative authoring and management of web resources.",
"WebDAV adds methods such as PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK and UNLOCK, plus XML property and multistatus structures.",
"Requests use HTTP methods and headers; XML request bodies describe properties, resource collections and lock information. Responses can use 207 Multi-Status.",
"A client discovers resource properties, performs permitted collection or content operations and may acquire locks to coordinate edits.",
"Use HTTPS, strict authentication and authorization, safe XML parsing and careful lock ownership and expiry. Never expose writable collections without access control.",
"Batch property operations can reduce round trips, but XML processing, locks and many small resources add server and client overhead.",
"Runs as an HTTP extension, commonly through TCP 443 when protected by TLS. Server support and enabled methods vary.",
"Check method allowlists, 207 response bodies, XML namespaces, lock tokens, lock timeout, path normalization and permissions.",
"Standardized authoring operations over existing HTTP infrastructure.",
"Lock conflicts, client interoperability, XML complexity and overly broad write permissions require careful management.",
"RFC 4918; WebDAV ACL RFC 3744. https://www.rfc-editor.org/rfc/rfc4918"),
ApplicationProtocolProfile("REST","API architecture","Application architecture using HTTP commonly","Architectural style, not a wire protocol",
"Representational State Transfer is an architectural style based on constraints such as client-server separation, stateless interaction, cacheability and a uniform interface.",
"Resources are identified by URIs. HTTP methods and status codes express operations and outcomes; representations commonly use JSON but are not limited to it.",
"REST does not define one universal packet format. The underlying protocol defines messages; representation formats and media types define payload syntax.",
"A client identifies a resource, sends a representation or request using the uniform interface, and interprets the response and links according to the API contract.",
"Use HTTPS, object-level authorization, input validation, rate limits and safe error handling. Statelessness does not remove authentication or authorization.",
"Cacheability and a uniform interface can simplify scaling. Chatty resource designs can increase round trips and payload overhead.",
"Usually implemented over HTTP, but REST is not synonymous with HTTP or JSON. OpenAPI can describe HTTP APIs but is not REST architecture itself.",
"Check resource URI design, method semantics, status codes, cache directives, content negotiation, authorization and representation compatibility.",
"Loose coupling, broad tooling and use of standard HTTP semantics.",
"REST is a style rather than a complete protocol specification; inconsistent API conventions can reduce interoperability.",
"R. Fielding, Architectural Styles and the Design of Network-based Software Architectures (2000); HTTP RFC 9110; OpenAPI https://spec.openapis.org/oas/latest.html"),
ApplicationProtocolProfile("GraphQL","API architecture","Application query layer","Query language and execution specification",
"A typed query language and execution model that lets clients request selected fields from a graph-shaped API schema.",
"A schema defines object, scalar, interface, union and operation types. A server validates and executes query, mutation or subscription operations.",
"GraphQL defines operation and type-system syntax, validation and execution semantics. HTTP is a common transport, but transport behavior is specified separately.",
"Client sends an operation and variables. Server parses, validates and executes resolvers, then returns data and any execution errors.",
"Authorize access at resolver and object level, limit query depth/complexity, protect introspection where appropriate and prevent excessive batching or resource use.",
"Field selection can reduce over-fetching, but resolver fan-out, N+1 queries, large nested operations and cache design affect performance.",
"The core GraphQL specification is maintained by the GraphQL project. GraphQL over HTTP is a separate specification effort; subscriptions require a transport convention.",
"Check schema version, validation errors, resolver latency, query complexity, response errors, HTTP status conventions and subscription transport.",
"Typed schema, client-selected fields and a single API query interface.",
"Requires disciplined schema and resolver governance; caching and authorization can be more complex than simple resource APIs.",
"GraphQL specification releases and working drafts: https://spec.graphql.org/ ; project: https://graphql.org/"),
ApplicationProtocolProfile("gRPC","Remote procedure calls","RPC framework over HTTP/2","RPC framework and protocol ecosystem",
"A high-performance RPC framework with generated client/server interfaces, streaming calls and Protocol Buffers as a common serialization format.",
"Service and method definitions generate stubs. gRPC maps calls to HTTP/2 streams and uses metadata, status and message framing.",
"A gRPC message uses a compressed flag, message length and serialized payload inside HTTP/2 DATA frames. Protobuf is a common payload encoding.",
"Client opens an HTTP/2 stream, sends metadata and request messages, receives response messages and final gRPC status/trailers.",
"Use TLS or an explicitly trusted secure channel, authenticate clients, authorize each RPC and set deadlines, message-size limits and resource quotas.",
"HTTP/2 multiplexing and compact binary messages suit service-to-service calls. Performance depends on serialization, network, concurrency and service design.",
"Native gRPC commonly uses HTTP/2 and Protocol Buffers. Browser use may require gRPC-Web or a compatible gateway; intermediary support varies.",
"Check HTTP/2 negotiation, content-type application/grpc, trailers, status codes, deadlines, flow control, message framing and generated-code compatibility.",
"Strong typed contracts, code generation, streaming and efficient binary serialization.",
"Browser and proxy compatibility can be narrower; schema evolution and generated-code coordination require discipline.",
"Official gRPC HTTP/2 protocol: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md ; docs: https://grpc.io/docs/"),
ApplicationProtocolProfile("SOAP 1.2","Enterprise messaging","Message framework over a binding","Messaging protocol",
"An XML-based messaging framework for structured information exchange across distributed systems.",
"SOAP defines an Envelope, optional Header and Body, processing model, extensibility model and binding framework for underlying protocols.",
"A SOAP message is an XML information set serialized as XML. Fault elements describe processing errors; headers carry extensible metadata.",
"A sender creates an Envelope and sends it through a binding. A receiver processes headers and body according to SOAP roles, modules and fault rules.",
"Use TLS for channel protection and apply message-level security standards when required. Validate XML safely and avoid unsafe external entity processing.",
"XML parsing and verbose envelopes add overhead. Interoperability depends on matching SOAP version, binding, WSDL contract and WS-* extensions.",
"SOAP 1.2 is transport-independent at the framework level; HTTP is a common binding. WSDL describes service interfaces and bindings.",
"Check SOAP version namespace, binding, action metadata, WSDL compatibility, XML namespaces, fault codes and intermediary header processing.",
"Formal message model, extensibility and established enterprise interoperability profiles.",
"Verbose XML, complex extension stacks and strict contract coordination can increase implementation cost.",
"W3C SOAP 1.2 Recommendations: https://www.w3.org/TR/soap12-part1/ and https://www.w3.org/TR/soap12-part2/"),
ApplicationProtocolProfile("JSON-RPC 2.0","Remote procedure calls","Application message format","RPC protocol specification",
"A lightweight stateless remote procedure call protocol that encodes requests and responses as JSON objects.",
"A client sends a method name, optional params and an id. A server returns result or error for calls with an id; notifications omit the id.",
"Request objects contain jsonrpc, method, optional params and id. Responses contain jsonrpc, result or error and the matching id. Batch arrays are supported.",
"Client sends a request. Server dispatches the method and returns a result or structured error; a notification receives no response.",
"JSON-RPC does not provide transport security or authorization. Use HTTPS or another secure transport and enforce method-level access control.",
"Small JSON messages are easy to inspect. Batch behavior, synchronous request handling and payload size affect throughput and latency.",
"Transport is not prescribed by the core specification; HTTP, WebSocket and other transports are used by implementations.",
"Check jsonrpc version, method spelling, id matching, params shape, error object, batch behavior and transport-specific status handling.",
"Simple, language-neutral RPC messages and easy debugging.",
"No built-in schema, authentication, encryption, discovery or transport guarantee; implementations must define surrounding concerns.",
"JSON-RPC 2.0 Specification: https://www.jsonrpc.org/specification"),
ApplicationProtocolProfile("Protocol Buffers","Data serialization","Payload encoding","Serialization format and interface definition language",
"A language-neutral, platform-neutral mechanism for serializing structured data. It is not itself a network transport protocol.",
"A .proto schema defines messages and field numbers. Generated code encodes and decodes typed messages for supported languages.",
"Binary wire format uses field keys that combine field number and wire type, followed by values such as varints, fixed-width values or length-delimited fields.",
"Application creates a schema-defined message, serializes it to bytes, transports those bytes through another protocol and parses them at the receiver.",
"Protect the transport separately. Treat untrusted payloads as untrusted input, bound message sizes and preserve field-number compatibility during schema evolution.",
"Compact binary representation and generated code can reduce payload size and parsing work. Performance depends on schema, payload and implementation.",
"Commonly used by gRPC but can be carried by other transports. Schema and runtime versions should be managed as part of the API contract.",
"Check schema package and version, field numbers, wire types, unknown fields, generated-code compatibility and length-delimited boundaries.",
"Compact encoding, typed schemas, code generation and cross-language support.",
"Binary payloads are less human-readable; unsafe field-number reuse or incompatible schema changes can break consumers.",
"Protocol Buffers official documentation and encoding guide: https://protobuf.dev/ ; gRPC transport is documented separately")
)

@Composable
fun ApplicationProtocolCenterScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProfile by remember { mutableStateOf<ApplicationProtocolProfile?>(null) }
    val categories = listOf("All") + applicationProtocolProfiles.map { it.category }.distinct()
    val visible = applicationProtocolProfiles.filter { profile ->
        (selectedCategory == "All" || profile.category == selectedCategory) &&
            (query.isBlank() || profile.name.contains(query, true) ||
                profile.overview.contains(query, true) || profile.category.contains(query, true) ||
                profile.classification.contains(query, true))
    }
    val current = selectedProfile
    if (current != null) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { selectedProfile = null }) { Text("← Back to Application Protocols") }
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
            Text(current.category + " · " + current.layer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(onClick = {}, label = { Text(current.classification) })
            Text(current.overview, style = MaterialTheme.typography.bodyLarge)
            ApplicationProtocolSection("1. Architecture & component roles", current.architecture)
            ApplicationProtocolSection("2. Packet, frame & message model", current.wireModel)
            ApplicationProtocolSection("3. Operation lifecycle", current.lifecycle)
            ApplicationProtocolSection("4. Security model & trust boundaries", current.security)
            ApplicationProtocolSection("5. Performance characteristics", current.performance)
            ApplicationProtocolSection("6. Deployment & interoperability", current.deployment)
            ApplicationProtocolSection("7. Troubleshooting methodology", current.troubleshooting)
            ApplicationProtocolSection("8. Advantages", current.advantages)
            ApplicationProtocolSection("9. Limitations & operational hurdles", current.limitations)
            ApplicationProtocolSection("10. Primary specifications & references", current.references)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("Classification matters: HTTP and WebSocket are protocols; HTTPS is HTTP protected by TLS; REST is an architectural style; GraphQL is a query language and execution specification; Protocol Buffers is a serialization format. They are related, but they are not interchangeable protocol layers.",
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Application Protocol Center", style = MaterialTheme.typography.headlineSmall)
            Text("A dedicated reference for web transfer, real-time web, API architecture, RPC, enterprise messaging and serialization. Select an entry for its technical chapter.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Search application protocols") }, placeholder = { Text("HTTP/3, GraphQL, WebSocket…") }, singleLine = true)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) })
                }
            }
            Text(visible.size.toString() + " entries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            visible.forEach { profile ->
                Card(onClick = { selectedProfile = profile }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(profile.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(profile.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(profile.classification + " · Tap for full technical details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Text("Educational reference only. This tab does not send API requests, open WebSocket sessions, execute RPC calls or configure external services.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ApplicationProtocolSection(title: String, body: String) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
