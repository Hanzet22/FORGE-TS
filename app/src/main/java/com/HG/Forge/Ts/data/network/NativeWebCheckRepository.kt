package com.HG.Forge.Ts.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

data class NativeWebFinding(
    val category: String,
    val title: String,
    val status: String,
    val details: List<String>,
    val source: String
)

data class NativeWebReport(
    val finalUrl: String,
    val checkedAt: String,
    val findings: List<NativeWebFinding>
)

/**
 * Passive, bounded website inspection for domains the user owns or is authorized to assess.
 * Redirects are handled manually and every destination is revalidated before a request.
 */
class NativeWebCheckRepository {
    private data class FetchResult(
        val url: String,
        val status: Int,
        val headers: Map<String, List<String>>,
        val body: String,
        val redirectChain: List<String>,
        val tlsDetails: List<String>
    )

    suspend fun inspect(input: String): NativeWebReport = withContext(Dispatchers.IO) {
        val initial = normalizeUrl(input)
        val fetched = fetchPublic(initial)
        val findings = mutableListOf<NativeWebFinding>()
        val headers = fetched.headers
        fun header(name: String): String? = headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value?.firstOrNull()

        findings += NativeWebFinding(
            category = "HTTP",
            title = "HTTP response",
            status = if (fetched.status in 200..299) "Successful" else "HTTP ${fetched.status}",
            details = listOf(
                "Final URL: ${fetched.url}",
                "Status code: ${fetched.status}",
                "Redirects followed: ${fetched.redirectChain.size - 1}",
                "Content-Type: ${header("Content-Type") ?: "Not provided"}",
                "Content-Length: ${header("Content-Length") ?: "Not provided"}"
            ) + fetched.redirectChain.mapIndexed { index, item -> "${index + 1}. $item" },
            source = "Direct HTTP response"
        )

        val securityHeaders = listOf(
            "Strict-Transport-Security",
            "Content-Security-Policy",
            "X-Content-Type-Options",
            "X-Frame-Options",
            "Referrer-Policy",
            "Permissions-Policy",
            "Cross-Origin-Opener-Policy",
            "Cross-Origin-Resource-Policy"
        )
        val present = securityHeaders.mapNotNull { name -> header(name)?.let { name to it } }
        val missing = securityHeaders.filter { name -> present.none { it.first.equals(name, true) } }
        findings += NativeWebFinding(
            category = "Security",
            title = "HTTP security headers",
            status = when {
                missing.isEmpty() -> "All checked headers present"
                present.isEmpty() -> "No checked headers detected"
                else -> "${present.size}/${securityHeaders.size} detected"
            },
            details = present.map { "${it.first}: ${it.second.take(500)}" } +
                if (missing.isEmpty()) listOf("No missing headers in this checklist.")
                else listOf("Not detected: ${missing.joinToString(", ")}"),
            source = "HTTP response headers"
        )

        val cookies = headers.entries
            .filter { it.key.equals("Set-Cookie", true) }
            .flatMap { it.value }
        val cookieDetails = if (cookies.isEmpty()) {
            listOf("No Set-Cookie response headers were observed.")
        } else cookies.take(20).map { cookie ->
            val name = cookie.substringBefore('=').take(80)
            val flags = listOf("Secure", "HttpOnly", "SameSite")
                .filter { flag -> cookie.contains(flag, ignoreCase = true) }
            "$name — flags: ${if (flags.isEmpty()) "none detected" else flags.joinToString(", ")}"
        }
        findings += NativeWebFinding(
            category = "Privacy",
            title = "Cookie attributes",
            status = if (cookies.isEmpty()) "No cookies set in this response" else "${cookies.size} cookie header(s)",
            details = cookieDetails,
            source = "Set-Cookie response headers; values are not retained"
        )

        val tls = fetched.tlsDetails
        findings += NativeWebFinding(
            category = "TLS",
            title = "TLS certificate and connection",
            status = if (fetched.url.startsWith("https://")) "HTTPS connection established" else "HTTP only",
            details = if (tls.isEmpty()) listOf("No TLS certificate metadata available for this connection.")
                else tls,
            source = "Platform TLS stack and peer certificate"
        )

        val redirectDetails = if (fetched.redirectChain.size <= 1) {
            listOf("No redirect was observed.")
        } else fetched.redirectChain.mapIndexed { index, item -> "${index + 1}. $item" }
        findings += NativeWebFinding(
            category = "HTTP",
            title = "Redirect chain",
            status = "${fetched.redirectChain.size - 1} redirect(s)",
            details = redirectDetails,
            source = "Manually followed HTTP Location headers; maximum 5 redirects"
        )

        val base = URI(fetched.url)
        val root = base.resolve("/")
        val robots = safeFetchText(root.resolve("/robots.txt").toString())
        findings += NativeWebFinding(
            category = "SEO",
            title = "robots.txt",
            status = robots?.let { "Found" } ?: "Not available",
            details = robots?.lineSequence()?.filter { it.isNotBlank() }?.take(30)?.toList()
                ?: listOf("robots.txt could not be retrieved."),
            source = root.resolve("/robots.txt").toString()
        )

        val sitemapCandidates = linkedSetOf(root.resolve("/sitemap.xml").toString())
        robots?.lineSequence()?.filter { it.trim().startsWith("Sitemap:", true) }
            ?.map { it.substringAfter(':').trim() }
            ?.filter { it.startsWith("https://") || it.startsWith("http://") }
            ?.take(5)?.forEach { sitemapCandidates += it }
        val sitemapFound = sitemapCandidates.mapNotNull { candidate ->
            safeFetchText(candidate)?.let { candidate to it }
        }
        findings += NativeWebFinding(
            category = "SEO",
            title = "XML sitemap",
            status = if (sitemapFound.isEmpty()) "Not detected" else "${sitemapFound.size} sitemap source(s) found",
            details = if (sitemapFound.isEmpty()) listOf("No sitemap was found at the common path or robots.txt declarations.")
                else sitemapFound.map { (url, body) -> "$url — ${Regex("<loc>", RegexOption.IGNORE_CASE).findAll(body).count()} location tag(s)" },
            source = "Common sitemap path and robots.txt declarations"
        )

        val securityTxtUrls = listOf(
            root.resolve("/.well-known/security.txt").toString(),
            root.resolve("/security.txt").toString()
        )
        val securityTxt = securityTxtUrls.firstNotNullOfOrNull { url -> safeFetchText(url)?.let { url to it } }
        findings += NativeWebFinding(
            category = "Security",
            title = "security.txt",
            status = if (securityTxt == null) "Not detected" else "Found",
            details = securityTxt?.second?.lineSequence()?.filter { it.isNotBlank() }?.take(25)?.toList()
                ?: listOf("No security.txt file was found at the two conventional locations."),
            source = securityTxt?.first ?: "RFC 9116 conventional paths"
        )

        val html = fetched.body
        val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim()?.take(250)
        val description = metaContent(html, "description")
        val canonical = Regex("<link[^>]+rel=[\"']?canonical[\"']?[^>]+href=[\"']([^\"']+)", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        val social = listOf("og:title", "og:description", "og:image", "twitter:card", "twitter:title")
            .mapNotNull { key -> metaContent(html, key)?.let { "$key: ${it.take(250)}" } }
        findings += NativeWebFinding(
            category = "SEO",
            title = "Page metadata and social tags",
            status = if (title != null || description != null || social.isNotEmpty()) "Metadata detected" else "Limited metadata",
            details = listOfNotNull(title?.let { "Title: $it" }, description?.let { "Description: $it" }, canonical?.let { "Canonical: $it" }) +
                if (social.isEmpty()) listOf("No selected Open Graph or Twitter metadata found.") else social,
            source = "HTML document; metadata-only extraction"
        )

        val scriptSources = Regex("<script[^>]+src=[\"']([^\"']+)", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1].lowercase() }.toList()
        val indicators = buildList {
            if (html.contains("__NEXT_DATA__") || scriptSources.any { it.contains("/_next/") }) add("Next.js indicator")
            if (html.contains("data-reactroot") || html.contains("react")) add("React indicator")
            if (html.contains("ng-version") || html.contains("angular")) add("Angular indicator")
            if (html.contains("data-v-app") || html.contains("vue")) add("Vue indicator")
            if (html.contains("wp-content") || html.contains("wp-includes")) add("WordPress indicator")
            if (html.contains("cdn.shopify.com") || html.contains("shopify")) add("Shopify indicator")
            if (html.contains("data-sveltekit") || scriptSources.any { it.contains("svelte") }) add("Svelte indicator")
        }.distinct()
        findings += NativeWebFinding(
            category = "Technology",
            title = "Public technology indicators",
            status = if (indicators.isEmpty()) "No strong indicators detected" else "${indicators.size} indicator(s)",
            details = indicators.ifEmpty { listOf("No supported framework or platform signatures were detected in the sampled HTML.") },
            source = "Public HTML signatures; heuristic only, not definitive identification"
        )

        val mixedCount = if (fetched.url.startsWith("https://")) {
            Regex("(?:src|href)=[\"']http://", RegexOption.IGNORE_CASE).findAll(html).count()
        } else 0
        findings += NativeWebFinding(
            category = "Security",
            title = "Mixed-content references",
            status = if (!fetched.url.startsWith("https://")) "HTTPS not used" else "$mixedCount reference(s)",
            details = listOf(
                if (!fetched.url.startsWith("https://")) "The final page URL uses HTTP."
                else if (mixedCount == 0) "No simple HTTP src/href references were detected in the sampled HTML."
                else "Potential HTTP resource references were found; browser behavior and actual loading are not tested."
            ),
            source = "Bounded HTML response scan"
        )

        val serverSignals = listOf("Server", "X-Powered-By", "Via", "X-Cache", "X-CDN")
            .mapNotNull { name -> header(name)?.let { "$name: ${it.take(250)}" } }
        findings += NativeWebFinding(
            category = "Server",
            title = "Public server response signals",
            status = if (serverSignals.isEmpty()) "No identifying headers exposed" else "${serverSignals.size} signal(s)",
            details = serverSignals.ifEmpty { listOf("The response did not expose the selected server-identification headers.") },
            source = "HTTP response headers; values may be absent, masked or intentionally modified"
        )

        val wafSignals = listOf("cf-ray", "x-sucuri-id", "x-akamai-transformed", "x-iinfo", "x-cdn")
            .mapNotNull { name -> header(name)?.let { "$name: ${it.take(160)}" } }
        findings += NativeWebFinding(
            category = "Security",
            title = "Edge and firewall response hints",
            status = if (wafSignals.isEmpty()) "No selected edge signals detected" else "${wafSignals.size} signal(s)",
            details = wafSignals.ifEmpty { listOf("No selected CDN/WAF response headers were observed; this does not prove that no firewall is present.") },
            source = "Passive response-header hints only; no firewall detection probes"
        )

        val links = Regex("<a[^>]+href=[\\"']([^\\"']+)", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1] }.distinct().take(250).toList()
        val internalLinks = links.count { link ->
            runCatching { URI(fetched.url).resolve(link).host.equals(URI(fetched.url).host, true) }.getOrDefault(false)
        }
        findings += NativeWebFinding(
            category = "SEO",
            title = "Linked-page inventory",
            status = "${links.size} unique link(s) in sampled HTML",
            details = listOf(
                "Same-host links: $internalLinks",
                "Other-host or unresolved links: ${links.size - internalLinks}",
                "This is a static link count; linked pages are not crawled."
            ),
            source = "Bounded HTML document; maximum 250 unique links"
        )

        val trackerPatterns = listOf(
            "Google Analytics" to listOf("google-analytics.com", "googletagmanager.com", "gtag("),
            "Meta Pixel" to listOf("connect.facebook.net", "fbq("),
            "Hotjar" to listOf("static.hotjar.com", "hotjar.com"),
            "Microsoft Clarity" to listOf("clarity.ms"),
            "Matomo" to listOf("matomo.js", "piwik.js")
        )
        val trackerHits = trackerPatterns.mapNotNull { (name, signatures) ->
            if (signatures.any { html.contains(it, true) }) name else null
        }
        findings += NativeWebFinding(
            category = "Privacy",
            title = "Common tracker indicators",
            status = if (trackerHits.isEmpty()) "No selected signatures detected" else "${trackerHits.size} tracker family hint(s)",
            details = trackerHits.ifEmpty { listOf("No selected tracker signatures were found in the sampled HTML.") } +
                listOf("Static signature matching only; dynamically loaded trackers may be missed."),
            source = "HTML and script-source signature list"
        )

        val socialHosts = listOf("github.com", "linkedin.com", "instagram.com", "facebook.com", "x.com", "twitter.com", "youtube.com")
        val socialLinks = links.mapNotNull { link ->
            runCatching {
                val host = URI(fetched.url).resolve(link).host?.lowercase() ?: return@runCatching null
                if (socialHosts.any { host == it || host.endsWith(".$it") }) link else null
            }.getOrNull()
        }.distinct().take(20).toList()
        findings += NativeWebFinding(
            category = "Social",
            title = "Public social links",
            status = if (socialLinks.isEmpty()) "No selected social links found" else "${socialLinks.size} link(s)",
            details = socialLinks.ifEmpty { listOf("No selected social profile links were found in the sampled HTML.") },
            source = "Anchor links in the public HTML response"
        )

        NativeWebReport(
            finalUrl = fetched.url,
            checkedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date()),
            findings = findings
        )
    }

    private fun metaContent(html: String, key: String): String? {
        val escaped = Regex.escape(key)
        val patterns = listOf(
            Regex("<meta[^>]+(?:name|property)=[\"']$escaped[\"'][^>]+content=[\"']([^\"']*)", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+(?:name|property)=[\"']$escaped[\"']", RegexOption.IGNORE_CASE)
        )
        return patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.getOrNull(1) }
            ?.replace("&amp;", "&")?.replace("&quot;", "\"")?.trim()?.take(500)
    }

    private fun normalizeUrl(input: String): URL {
        val raw = input.trim()
        require(raw.isNotBlank()) { "Enter a website domain or URL." }
        val candidate = if (raw.contains("://")) raw else "https://$raw"
        val uri = URI(candidate)
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "Only HTTP and HTTPS URLs are supported."
        }
        require(uri.rawUserInfo == null && uri.host != null && uri.port <= 65535) {
            "Enter a valid public website URL without credentials."
        }
        require(uri.host!!.contains('.') && !uri.host!!.equals("localhost", true)) {
            "Enter a fully qualified public domain name."
        }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "Query strings and fragments are not accepted as inspection targets."
        }
        validatePublicHost(uri.host!!)
        return uri.toURL()
    }

    private fun validatePublicHost(host: String) {
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty()) { "The domain did not resolve to a public address." }
        require(addresses.all { address ->
            !address.isAnyLocalAddress && !address.isLoopbackAddress &&
                !address.isLinkLocalAddress && !address.isSiteLocalAddress &&
                !address.isMulticastAddress &&
                !(address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)
        }) { "Private, local, link-local, and multicast destinations are blocked." }
    }

    private fun fetchPublic(start: URL): FetchResult {
        var current = start
        val chain = mutableListOf<String>()
        repeat(6) { hop ->
            validatePublicHost(current.host)
            chain += current.toString()
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "FORGE-TS-NativeOSINT/1.0")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.2")
                setRequestProperty("Range", "bytes=0-131071")
                setRequestProperty("Connection", "close")
            }
            try {
                val status = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (status in listOf(301, 302, 303, 307, 308) && !location.isNullOrBlank()) {
                    require(hop < 5) { "Redirect limit exceeded." }
                    val next = current.toURI().resolve(location).toURL()
                    require(next.protocol == "https" || next.protocol == "http") { "Unsupported redirect protocol." }
                    require(!(current.protocol == "https" && next.protocol == "http")) {
                        "HTTPS-to-HTTP downgrade redirects are blocked."
                    }
                    current = next
                    return@repeat
                }
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val bytes = stream?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0 || total >= 131072) break
                        val allowed = minOf(read, 131072 - total)
                        output.write(buffer, 0, allowed)
                        total += allowed
                    }
                    output.toByteArray()
                } ?: ByteArray(0)
                val tls = if (connection is HttpsURLConnection) {
                    runCatching {
                        val cert = connection.serverCertificates.firstOrNull() as? java.security.cert.X509Certificate
                        if (cert == null) emptyList() else {
                            val fingerprint = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                                .joinToString(":") { "%02X".format(it) }
                            listOf(
                                "TLS cipher suite: ${connection.cipherSuite}",
                                "Subject: ${cert.subjectX500Principal.name}",
                                "Issuer: ${cert.issuerX500Principal.name}",
                                "Valid from: ${cert.notBefore}",
                                "Valid until: ${cert.notAfter}",
                                "SHA-256 fingerprint: $fingerprint"
                            )
                        }
                    }.getOrDefault(emptyList())
                } else emptyList()
                return FetchResult(
                    url = current.toString(),
                    status = status,
                    headers = connection.headerFields.entries.filter { it.key != null && it.value != null }.associate { it.key!! to it.value!! },
                    body = bytes.toString(Charsets.UTF_8),
                    redirectChain = chain.toList(),
                    tlsDetails = tls
                )
            } finally {
                connection.disconnect()
            }
        }
        error("Unable to complete the website request.")
    }

    private fun safeFetchText(rawUrl: String): String? = runCatching {
        val url = URI(rawUrl).toURL()
        require(url.protocol == "https" || url.protocol == "http")
        validatePublicHost(url.host)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "FORGE-TS-NativeOSINT/1.0")
            setRequestProperty("Range", "bytes=0-65535")
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0 || total >= 65536) break
                    val allowed = minOf(read, 65536 - total)
                    output.write(buffer, 0, allowed)
                    total += allowed
                }
                output.toByteArray()
            }
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
