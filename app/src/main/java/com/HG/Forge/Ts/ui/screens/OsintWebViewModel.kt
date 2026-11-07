package com.HG.Forge.Ts.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HG.Forge.Ts.data.network.DnsLookupRepository
import com.HG.Forge.Ts.data.network.NativeWebCheckRepository
import com.HG.Forge.Ts.data.network.NativeWebReport
import com.HG.Forge.Ts.data.network.DnsLookupResult
import com.HG.Forge.Ts.data.network.DnsResolver
import com.HG.Forge.Ts.data.network.RdapDomainRepository
import com.HG.Forge.Ts.data.network.RdapDomainReport
import com.HG.Forge.Ts.data.network.WebsiteCarbonRepository
import com.HG.Forge.Ts.data.network.WebsiteCarbonReport
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.NoRouteToHostException
import javax.net.ssl.SSLHandshakeException

data class OsintDnsCheck(
    val recordType: String,
    val queryName: String,
    val result: DnsLookupResult? = null,
    val error: String? = null
)

data class MailSecurityFinding(
    val title: String,
    val status: String,
    val details: List<String>
)

class OsintWebViewModel : ViewModel() {
    private val repository = DnsLookupRepository()
    private val nativeRepository = NativeWebCheckRepository()
    private val rdapRepository = RdapDomainRepository()
    private val carbonRepository = WebsiteCarbonRepository()
    private val recordTypes = listOf("A", "AAAA", "NS", "MX", "TXT", "CAA", "SOA", "CNAME", "SRV", "DS", "DNSKEY", "RRSIG", "NSEC", "TLSA", "HTTPS", "SVCB")

    var checks by mutableStateOf<List<OsintDnsCheck>>(emptyList())
        private set
    var target by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var nativeReport by mutableStateOf<NativeWebReport?>(null)
        private set
    var nativeError by mutableStateOf<String?>(null)
        private set
    var rdapReport by mutableStateOf<RdapDomainReport?>(null)
        private set
    var rdapError by mutableStateOf<String?>(null)
        private set
    var carbonReport by mutableStateOf<WebsiteCarbonReport?>(null)
        private set
    var carbonError by mutableStateOf<String?>(null)
        private set

    val dnssecFindings: List<MailSecurityFinding>
        get() {
            val signedTypes = listOf("DS", "DNSKEY", "RRSIG", "NSEC")
            val queried = checks.filter { it.recordType in signedTypes }
            val authenticated = queried.filter { it.result?.dnssecAuthenticated == true }
            val failed = queried.filter { it.result?.status == 2 }
            val details = buildList {
                add("Google Public DNS AD flag is resolver-provided evidence that the answer was authenticated by its validating resolver.")
                add("FORGE-TS does not locally verify the DS → DNSKEY → RRSIG cryptographic chain in this phase.")
                if (authenticated.isNotEmpty()) add("Authenticated responses: ${authenticated.joinToString { it.recordType }}.")
                if (failed.isNotEmpty()) add("SERVFAIL responses: ${failed.joinToString { it.recordType }}; this can have several causes and is not by itself a definitive DNSSEC-bogus verdict.")
            }
            return listOf(MailSecurityFinding(
                "DNSSEC validation signal",
                when { authenticated.isNotEmpty() -> "Resolver authenticated response(s)"; failed.isNotEmpty() -> "Resolver returned SERVFAIL"; queried.isNotEmpty() -> "No authenticated response flag observed"; else -> "Awaiting DNSSEC queries" },
                details
            ))
        }

    val caaFindings: List<MailSecurityFinding>
        get() {
            val records = checks.firstOrNull { it.recordType == "CAA" }?.result?.answers.orEmpty().map { it.data }
            val issuers = records.filter { it.contains("issue", true) }
            val wildcards = records.filter { it.contains("issuewild", true) }
            val iodef = records.filter { it.contains("iodef", true) }
            return listOf(MailSecurityFinding(
                "CAA certificate-authority policy",
                if (records.isEmpty()) "No CAA answer returned" else "${records.size} CAA record(s) observed",
                listOf(
                    if (issuers.isEmpty()) "No issue directive observed in returned answers." else "Issuance directives: ${issuers.joinToString("; ")}",
                    if (wildcards.isEmpty()) "No issuewild directive observed; wildcard handling may inherit issue policy under the standard." else "Wildcard directives: ${wildcards.joinToString("; ")}",
                    if (iodef.isEmpty()) "No iodef reporting directive observed." else "Incident-reporting directives: ${iodef.joinToString("; ")}",
                    "This is a record summary, not a complete CA authorization evaluation."
                )
            ))
        }

    val mailSecurityFindings: List<MailSecurityFinding>
        get() {
            if (checks.isEmpty()) return emptyList()
            fun answers(label: String): List<String> =
                checks.firstOrNull { it.recordType == label }?.result?.answers?.map { it.data }.orEmpty()
            fun txtContains(label: String, marker: String): Boolean =
                answers(label).any { it.contains(marker, ignoreCase = true) }

            val mx = answers("MX")
            val spf = answers("TXT").filter { it.contains("v=spf1", ignoreCase = true) }
            val dmarc = answers("DMARC TXT").filter { it.contains("v=DMARC1", ignoreCase = true) }
            val dmarcPolicy = dmarc.firstNotNullOfOrNull {
                Regex("""(?i)(?:^|;)\s*p\s*=\s*(none|quarantine|reject)""")
                    .find(it)?.groupValues?.getOrNull(1)?.lowercase()
            }
            val dkimChecks = checks.filter { it.recordType.startsWith("DKIM TXT") }
            val dkimFound = dkimChecks.filter { !it.result?.answers.isNullOrEmpty() }.map { it.recordType.substringAfter("·").trim() }
            val mtaSts = txtContains("MTA-STS TXT", "v=STSv1")
            val tlsRpt = txtContains("TLS-RPT TXT", "v=TLSRPTv1")

            return listOf(
                MailSecurityFinding(
                    "MX records",
                    if (mx.isNotEmpty()) "Detected" else "Not detected",
                    listOf(if (mx.isNotEmpty()) "${mx.size} mail-exchanger answer(s) returned." else "No MX answers were returned by the DNS resolver.")
                ),
                MailSecurityFinding(
                    "SPF",
                    when { spf.size > 1 -> "Multiple SPF policies detected"; spf.size == 1 -> "SPF TXT detected"; else -> "No SPF policy detected" },
                    listOf(
                        when { spf.size > 1 -> "More than one SPF policy was returned; multiple policies can cause SPF permerror."
                            spf.size == 1 -> spf.first().take(500)
                            else -> "No TXT answer containing v=spf1 was found at the domain apex." }
                    )
                ),
                MailSecurityFinding(
                    "DMARC",
                    when { dmarc.isEmpty() -> "No DMARC record detected"; dmarcPolicy == null -> "DMARC record found; policy not parsed"; else -> "DMARC policy: $dmarcPolicy" },
                    if (dmarc.isEmpty()) listOf("No v=DMARC1 TXT answer was found at _dmarc.") else dmarc.take(2).map { it.take(500) }
                ),
                MailSecurityFinding(
                    "DKIM",
                    if (dkimFound.isEmpty()) "No common-selector records detected" else "Candidate selector record(s): ${dkimFound.joinToString(", ")}",
                    listOf("Checked only selector1, selector2, google and default. DKIM selectors are provider-specific; absence here does not prove DKIM is not configured.")
                ),
                MailSecurityFinding(
                    "MTA-STS",
                    if (mtaSts) "Policy TXT detected" else "No MTA-STS TXT detected",
                    listOf(if (mtaSts) answers("MTA-STS TXT").filter { it.contains("v=STSv1", true) }.joinToString().take(500) else "Checked _mta-sts TXT for the v=STSv1 marker. The policy file itself is not fetched or validated.")
                ),
                MailSecurityFinding(
                    "SMTP TLS Reporting",
                    if (tlsRpt) "TLS-RPT TXT detected" else "No TLS-RPT TXT detected",
                    listOf(if (tlsRpt) answers("TLS-RPT TXT").filter { it.contains("v=TLSRPTv1", true) }.joinToString().take(500) else "Checked _smtp._tls TXT for the v=TLSRPTv1 marker.")
                )
            )
        }

    val dnsSecurityFindings: List<MailSecurityFinding>
        get() {
            if (checks.isEmpty()) return emptyList()
            val dnssecChecks = checks.filter { it.recordType in setOf("DS", "DNSKEY", "RRSIG") }
            val authenticated = dnssecChecks.any { it.result?.dnssecAuthenticated == true }
            val completed = dnssecChecks.count { it.result != null }
            val caa = checks.firstOrNull { it.recordType == "CAA" }?.result?.answers.orEmpty()
            val caaDetails = caa.map { answer ->
                val value = answer.data.trim()
                when {
                    value.contains("issuewild", true) -> "Wildcard issuance: $value"
                    value.contains("issue", true) -> "Certificate issuance: $value"
                    value.contains("iodef", true) -> "Incident reporting: $value"
                    else -> value
                }
            }
            return listOf(
                MailSecurityFinding(
                    "DNSSEC resolver validation",
                    when { authenticated -> "Authenticated-data (AD) signal present"; completed == 0 -> "Validation status unavailable"; else -> "No authenticated-data signal observed" },
                    listOf(
                        "Google Public DNS AD flag: ${if (authenticated) "true on at least one DS/DNSKEY/RRSIG response" else "not observed on sampled responses"}.",
                        "This is the recursive resolver validation signal, not an independent local DS to DNSKEY to RRSIG cryptographic-chain validator. A false AD flag does not by itself mean the domain is bogus."
                    )
                ),
                MailSecurityFinding(
                    "CAA certificate-authority policy",
                    if (caa.isEmpty()) "No CAA answers detected" else "${caa.size} CAA record(s) detected",
                    if (caaDetails.isEmpty()) listOf("No CAA records were returned. This does not mean certificate issuance is blocked; it means this lookup found no CAA policy.") else caaDetails.take(12)
                )
            )
        }
    fun inspect(input: String, includeCarbon: Boolean = false) {
        val domain = input.trim()
        if (domain.isBlank()) {
            error = "Enter a fully qualified domain name."
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            checks = emptyList()
            nativeReport = null
            nativeError = null
            rdapReport = null
            rdapError = null
            carbonReport = null
            carbonError = null
            target = domain
            val queries = recordTypes.map { type -> Triple(type, domain, type) } + listOf(
                Triple("DMARC TXT", "_dmarc.$domain", "TXT"),
                Triple("DKIM TXT · selector1", "selector1._domainkey.$domain", "TXT"),
                Triple("DKIM TXT · selector2", "selector2._domainkey.$domain", "TXT"),
                Triple("DKIM TXT · google", "google._domainkey.$domain", "TXT"),
                Triple("DKIM TXT · default", "default._domainkey.$domain", "TXT"),
                Triple("MTA-STS TXT", "_mta-sts.$domain", "TXT"),
                Triple("TLS-RPT TXT", "_smtp._tls.$domain", "TXT")
            )
            val collected = mutableListOf<OsintDnsCheck>()
            for (batch in queries.chunked(5)) {
                val completed = batch.map { (label, queryName, lookupType) ->
                    async {
                        try {
                            OsintDnsCheck(recordType = label, queryName = queryName, result = repository.lookup(queryName, lookupType, DnsResolver.GOOGLE))
                        } catch (exception: Exception) {
                            OsintDnsCheck(recordType = label, queryName = queryName, error = exception.message ?: "Lookup failed")
                        }
                    }
                }.awaitAll()
                collected += completed
                checks = collected.toList()
            }
            try {
                nativeReport = nativeRepository.inspect(domain)
            } catch (exception: Exception) {
                nativeError = explainNativeFailure(exception)
            }
            try {
                rdapReport = rdapRepository.lookup(domain)
            } catch (exception: Exception) {
                rdapError = exception.message?.take(400) ?: "RDAP lookup unavailable."
            }
            if (includeCarbon) {
                try {
                    carbonReport = carbonRepository.estimate(nativeReport?.finalUrl ?: "https://$domain")
                } catch (exception: Exception) {
                    carbonError = exception.message?.take(350) ?: "Carbon estimate unavailable."
                }
            }
            if (collected.all { it.result == null } && nativeReport == null) {
                error = "No DNS checks completed successfully. Review the domain and network connection."
            }
            isLoading = false
        }
    }

    private fun explainNativeFailure(exception: Exception): String {
        val causes = generateSequence(exception as Throwable?) { it.cause }.toList()
        val messages = causes.mapNotNull { it.message }
            .filterNot { it.contains(" from /") || it.contains("local address", true) }
            .joinToString(" | ")
        val connectTimeout = messages.contains("failed to connect", true) ||
            messages.contains("connect timed out", true) ||
            messages.contains("after 12000ms", true)
        return when {
            causes.any { it is UnknownHostException } ->
                "DNS resolution failed before a connection could be attempted. Check the domain and resolver availability."
            causes.any { it is SSLHandshakeException } ||
                messages.contains("certificate", true) || messages.contains("handshake", true) ->
                "TLS negotiation failed after the connection stage. Review certificate validity, hostname matching, device time and network interception. Details: ${messages.take(350)}"
            causes.any { it is NoRouteToHostException } ->
                "The device network reports no route to the destination. This is a routing/connectivity issue, not a certificate verdict."
            connectTimeout || causes.any { it is ConnectException } ->
                "TCP connection to the resolved HTTPS endpoint did not complete before timeout. Possible causes include remote service availability, edge filtering, routing or the current network. DNS resolution alone does not prove HTTPS reachability. Compare on another trusted network; do not treat this as an SSL verdict. Details: ${messages.take(350)}"
            causes.any { it is SocketTimeoutException } ->
                "The server connection was established or attempted, but a network read timed out while waiting for data. This differs from a DNS failure and is not an SSL verdict. Details: ${messages.take(350)}"
            else -> messages.take(500).ifBlank { "Native website inspection failed for an unspecified network reason." }
        }
    }
}
