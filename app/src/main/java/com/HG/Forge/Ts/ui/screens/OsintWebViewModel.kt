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
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.net.ConnectException
import java.net.SocketTimeoutException

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

    fun inspect(input: String) {
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
            if (collected.all { it.result == null } && nativeReport == null) {
                error = "No DNS checks completed successfully. Review the domain and network connection."
            }
            isLoading = false
        }
    }

    private fun explainNativeFailure(exception: Exception): String {
        val causes = generateSequence(exception as Throwable?) { it.cause }.toList()
        val messages = causes.mapNotNull { it.message }.joinToString(" | ")
        val isConnectTimeout = causes.any { it is SocketTimeoutException || it is ConnectException } ||
            messages.contains("failed to connect", true) ||
            messages.contains("connect timed out", true) ||
            messages.contains("after 7000ms", true) ||
            messages.contains("after 12000ms", true)

        return when {
            isConnectTimeout ->
                "TCP connection could not be established before the timeout. DNS may have resolved the host, but the HTTPS server or network route did not complete the connection. Try again later or compare on another trusted network. This is not an SSL-certificate verdict. Details: ${messages.take(500)}"
            messages.contains("UnknownHost", true) || messages.contains("unable to resolve", true) ->
                "DNS resolution failed before the website connection. Check the domain spelling and DNS/network availability. Details: ${messages.take(500)}"
            messages.contains("SSL", true) || messages.contains("certificate", true) || messages.contains("handshake", true) ->
                "The HTTPS/TLS negotiation failed. The site may have a certificate, protocol or interception issue. Details: ${messages.take(500)}"
            else -> messages.take(700).ifBlank { "Native website inspection failed for an unspecified network reason." }
        }
    }
}
