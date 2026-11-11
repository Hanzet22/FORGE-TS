package com.HG.Forge.Ts.bench

import com.HG.Forge.Ts.data.network.CidrCalculator
import com.HG.Forge.Ts.data.network.CidrResult
import com.HG.Forge.Ts.data.network.VlsmAllocation
import com.HG.Forge.Ts.data.network.VlsmCalculator
import com.HG.Forge.Ts.data.network.VlsmRequest
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

// Parameters are labels rather than raw addresses: IPv6 colons would make the
// benchmark names hard to read in reports.
private val CIDR_INPUTS = mapOf(
    "ipv4-24" to "192.168.1.10/24",
    "ipv4-31" to "10.0.0.1/31",
    "ipv6-64" to "2001:db8:85a3::8a2e:370:7334/64",
    "ipv4-mapped-ipv6-120" to "::ffff:192.0.2.128/120"
)

private val ADDRESS_INPUTS = mapOf(
    "ipv4" to "172.16.254.1",
    "ipv6" to "2001:db8:85a3::8a2e:370:7334"
)

@State(Scope.Benchmark)
open class CidrCalculatorBenchmark {

    @Param("ipv4-24", "ipv4-31", "ipv6-64", "ipv4-mapped-ipv6-120")
    var input: String = ""

    private lateinit var cidr: String

    @Setup
    fun setup() {
        cidr = CIDR_INPUTS.getValue(input)
    }

    @Benchmark
    fun calculate(): CidrResult = CidrCalculator.calculate(cidr)
}

@State(Scope.Benchmark)
open class AddressBinaryBenchmark {

    @Param("ipv4", "ipv6")
    var input: String = ""

    private lateinit var address: String

    @Setup
    fun setup() {
        address = ADDRESS_INPUTS.getValue(input)
    }

    @Benchmark
    fun toBinary(): String = CidrCalculator.toBinary(address)
}

@State(Scope.Benchmark)
open class VlsmCalculatorBenchmark {

    @Param("4", "64")
    var subnets: Int = 0

    private lateinit var baseCidr: String
    private lateinit var requests: List<VlsmRequest>

    @Setup
    fun setup() {
        baseCidr = if (subnets <= 4) "192.168.10.0/24" else "10.20.0.0/16"
        val hostCounts = intArrayOf(2, 6, 14, 30, 50, 120)
        requests = List(subnets) { index ->
            val hosts = if (subnets <= 4) hostCounts[index] else hostCounts[index % hostCounts.size] * 4
            VlsmRequest("subnet-$index", hosts)
        }
    }

    @Benchmark
    fun allocate(): List<VlsmAllocation> = VlsmCalculator.allocate(baseCidr, requests)
}
