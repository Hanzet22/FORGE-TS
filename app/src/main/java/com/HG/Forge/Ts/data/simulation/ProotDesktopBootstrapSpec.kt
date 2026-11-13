package com.HG.Forge.Ts.data.simulation

/**
 * Distro-specific, fixed package plans for the optional XFCE4 guest bootstrap.
 * Commands are argument arrays; no user-provided shell text is interpolated.
 */
internal data class ProotDesktopBootstrapPlan(
    val packageManager: String,
    val updateArguments: List<String>,
    val installArguments: List<String>,
    val verifyArguments: List<String>
)

internal object ProotDesktopBootstrapSpec {
    fun forDistribution(distribution: ProotDistribution): ProotDesktopBootstrapPlan =
        when (distribution) {
            ProotDistribution.DEBIAN, ProotDistribution.UBUNTU -> ProotDesktopBootstrapPlan(
                packageManager = "APT",
                updateArguments = listOf("apt-get", "update"),
                installArguments = listOf(
                    "/bin/sh", "-c",
                    "DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends xfce4 dbus-x11"
                ),
                verifyArguments = listOf(
                    "/bin/sh", "-c",
                    "command -v startxfce4 >/dev/null 2>&1 && command -v dbus-launch >/dev/null 2>&1"
                )
            )
            ProotDistribution.ALPINE -> ProotDesktopBootstrapPlan(
                packageManager = "APK",
                updateArguments = listOf("apk", "update"),
                installArguments = listOf("apk", "add", "--no-cache", "xfce4", "dbus-x11"),
                verifyArguments = listOf(
                    "/bin/sh", "-c",
                    "command -v startxfce4 >/dev/null 2>&1 && command -v dbus-launch >/dev/null 2>&1"
                )
            )
        }
}