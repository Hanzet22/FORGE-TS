package com.HG.Forge.Ts.data.network

import java.math.BigInteger
import java.net.InetAddress

data class CidrResult(
    val address: String,
    val prefix: Int,
    val network: String,
    val lastAddress: String,
    val addressCount: String,
    val addressFamily: String
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

        return CidrResult(
            address = format(bytes),
            prefix = prefix,
            network = format(toBytes(networkValue, bytes.size)),
            lastAddress = format(toBytes(lastValue, bytes.size)),
            addressCount = block.toString(),
            addressFamily = if (bits == 32) "IPv4" else "IPv6"
        )
    }

    private fun parseIpv4(value: String): ByteArray {
        val octets = value.split(".")
        require(octets.size == 4) { "Invalid IPv4 address" }
        return octets.map {
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
