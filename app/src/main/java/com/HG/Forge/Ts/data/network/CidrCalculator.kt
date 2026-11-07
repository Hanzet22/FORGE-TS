package com.HG.Forge.Ts.data.network

import java.math.BigInteger
import java.net.InetAddress

data class CidrResult(
    val address: String,
    val prefix: Int,
    val network: String,
    val lastAddress: String,
    val addressCount: String,
    val addressFamily: String,
    val subnetMask: String? = null,
    val broadcastAddress: String? = null,
    val firstUsableAddress: String? = null,
    val lastUsableAddress: String? = null,
    val usableHostCount: String? = null
)

object CidrCalculator {
    fun calculate(input: String): CidrResult {
        val parts = input.trim().split("/")
        require(parts.size == 2) { "Use address/prefix, for example 192.168.1.10/24" }
        val addressText = parts[0].trim()
        val bytes = if (addressText.contains(":")) parseIpv6(addressText) else parseIpv4(addressText)
        val bits = bytes.size * 8
        val prefix = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Prefix must be a number")
        require(prefix in 0..bits) { "Prefix must be between 0 and $bits" }

        val value = BigInteger(1, bytes)
        val hostBits = bits - prefix
        val block = BigInteger.ONE.shiftLeft(hostBits)
        val networkValue = value.divide(block).multiply(block)
        val lastValue = networkValue.add(block).subtract(BigInteger.ONE)
        val isIpv4 = bits == 32
        val ipv4Usable = when {
            !isIpv4 -> null
            prefix <= 30 -> block.subtract(BigInteger.TWO)
            prefix == 31 -> BigInteger.TWO // RFC 3021 point-to-point links
            else -> BigInteger.ONE
        }
        val firstUsable = when {
            !isIpv4 -> null
            prefix <= 30 -> networkValue.add(BigInteger.ONE)
            else -> networkValue
        }
        val lastUsable = when {
            !isIpv4 -> null
            prefix <= 30 -> lastValue.subtract(BigInteger.ONE)
            else -> lastValue
        }

        return CidrResult(
            address = format(bytes),
            prefix = prefix,
            network = format(toBytes(networkValue, bytes.size)),
            lastAddress = format(toBytes(lastValue, bytes.size)),
            addressCount = block.toString(),
            addressFamily = if (isIpv4) "IPv4" else "IPv6",
            subnetMask = if (isIpv4) format(toBytes(maskValue(prefix, 32), 4)) else null,
            broadcastAddress = if (isIpv4 && prefix <= 30) format(toBytes(lastValue, 4)) else null,
            firstUsableAddress = firstUsable?.let { format(toBytes(it, 4)) },
            lastUsableAddress = lastUsable?.let { format(toBytes(it, 4)) },
            usableHostCount = ipv4Usable?.toString()
        )
    }

    fun toBinary(input: String): String {
        val address = input.trim()
        val bytes = if (address.contains(":")) parseIpv6(address) else parseIpv4(address)
        return if (bytes.size == 4) {
            bytes.joinToString(".") { (it.toInt() and 255).toString(2).padStart(8, '0') }
        } else {
            bytes.toList().chunked(2).joinToString(":") { pair ->
                (((pair[0].toInt() and 255) shl 8) or (pair[1].toInt() and 255))
                    .toString(2).padStart(16, '0')
            }
        }
    }

    private fun maskValue(prefix: Int, bits: Int): BigInteger {
        if (prefix == 0) return BigInteger.ZERO
        return BigInteger.ONE.shiftLeft(prefix).subtract(BigInteger.ONE).shiftLeft(bits - prefix)
    }

    private fun parseIpv4(value: String): ByteArray {
        val octets = value.split(".")
        require(octets.size == 4) { "Invalid IPv4 address" }
        return octets.map {
            require(it.isNotEmpty() && (it == "0" || !it.startsWith("0"))) { "IPv4 octets must use decimal notation without leading zeros" }
            val n = it.toIntOrNull() ?: throw IllegalArgumentException("Invalid IPv4 octet")
            require(n in 0..255) { "IPv4 octets must be 0–255" }
            n.toByte()
        }.toByteArray()
    }

    private fun parseIpv6(input: String): ByteArray {
        val value = input.substringBefore("%")
        require(value.contains(":")) { "Invalid IPv6 address" }
        val doubleColon = value.indexOf("::")
        require(doubleColon == -1 || value.indexOf("::", doubleColon + 2) == -1) { "Invalid IPv6 compression" }

        fun groups(text: String): List<String> {
            if (text.isEmpty()) return emptyList()
            val raw = text.split(":").toMutableList()
            if (raw.any { it.isEmpty() }) throw IllegalArgumentException("Invalid IPv6 group")
            if (raw.lastOrNull()?.contains(".") == true) {
                val ipv4 = parseIpv4(raw.removeAt(raw.lastIndex))
                raw += "%02x%02x".format(ipv4[0].toInt() and 255, ipv4[1].toInt() and 255)
                raw += "%02x%02x".format(ipv4[2].toInt() and 255, ipv4[3].toInt() and 255)
            }
            return raw
        }

        val parsed = if (doubleColon >= 0) {
            val left = groups(value.substring(0, doubleColon))
            val right = groups(value.substring(doubleColon + 2))
            val missing = 8 - left.size - right.size
            require(missing >= 1) { "Invalid IPv6 address length" }
            left + List(missing) { "0" } + right
        } else groups(value)

        require(parsed.size == 8) { "IPv6 address must contain eight groups (or use ::)" }
        val bytes = ByteArray(16)
        parsed.forEachIndexed { index, group ->
            require(group.length <= 4 && group.all { it.digitToIntOrNull(16) != null }) { "Invalid IPv6 group" }
            val number = group.toInt(16)
            bytes[index * 2] = (number shr 8).toByte()
            bytes[index * 2 + 1] = number.toByte()
        }
        return bytes
    }

    private fun toBytes(value: BigInteger, size: Int): ByteArray {
        val source = value.toByteArray()
        val result = ByteArray(size)
        val copyLength = minOf(source.size, size)
        System.arraycopy(source, source.size - copyLength, result, size - copyLength, copyLength)
        return result
    }

    private fun format(bytes: ByteArray): String =
        InetAddress.getByAddress(bytes).hostAddress ?: bytes.joinToString(".") { (it.toInt() and 255).toString() }
}

data class VlsmRequest(val name: String, val requiredHosts: Int)
data class VlsmAllocation(
    val name: String,
    val cidr: String,
    val network: String,
    val firstHost: String,
    val lastHost: String,
    val broadcast: String,
    val requestedHosts: Int,
    val availableHosts: String
)

object VlsmCalculator {
    fun allocate(baseCidr: String, requests: List<VlsmRequest>): List<VlsmAllocation> {
        require(requests.isNotEmpty()) { "Add at least one subnet requirement" }
        require(requests.all { it.name.isNotBlank() && it.requiredHosts > 0 }) { "Each subnet needs a name and a positive host count" }
        val base = CidrCalculator.calculate(baseCidr)
        require(base.addressFamily == "IPv4") { "VLSM planner currently supports IPv4 networks" }
        val baseStart = ipv4ToBigInteger(base.network)
        val baseEnd = ipv4ToBigInteger(base.lastAddress)
        var cursor = baseStart

        return requests.sortedByDescending { it.requiredHosts }.map { request ->
            val needed = BigInteger.valueOf(request.requiredHosts.toLong()).add(BigInteger.TWO)
            val hostBits = needed.subtract(BigInteger.ONE).bitLength()
            require(hostBits <= 30) { "A subnet request is too large for a usable IPv4 host allocation" }
            val blockSize = BigInteger.ONE.shiftLeft(hostBits.coerceAtLeast(2))
            val offset = cursor.subtract(baseStart)
            val alignedOffset = if (offset.mod(blockSize) == BigInteger.ZERO) offset
                else offset.add(blockSize).subtract(offset.mod(blockSize))
            val start = baseStart.add(alignedOffset)
            val end = start.add(blockSize).subtract(BigInteger.ONE)
            require(end <= baseEnd) { "Address space exhausted while allocating ${request.name}" }
            val prefix = 32 - hostBits.coerceAtLeast(2)
            val available = blockSize.subtract(BigInteger.TWO)
            val allocation = VlsmAllocation(
                name = request.name.trim(),
                cidr = "${formatIpv4(start)}/$prefix",
                network = formatIpv4(start),
                firstHost = formatIpv4(start.add(BigInteger.ONE)),
                lastHost = formatIpv4(end.subtract(BigInteger.ONE)),
                broadcast = formatIpv4(end),
                requestedHosts = request.requiredHosts,
                availableHosts = available.toString()
            )
            cursor = end.add(BigInteger.ONE)
            allocation
        }
    }

    private fun ipv4ToBigInteger(value: String): BigInteger =
        BigInteger(1, value.split(".").map { it.toInt().toByte() }.toByteArray())

    private fun formatIpv4(value: BigInteger): String {
        val bytes = value.toByteArray()
        val result = ByteArray(4)
        val copy = minOf(4, bytes.size)
        System.arraycopy(bytes, bytes.size - copy, result, 4 - copy, copy)
        return result.joinToString(".") { (it.toInt() and 255).toString() }
    }
}
