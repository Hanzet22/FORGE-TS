package com.HG.Forge.Ts.data.simulation

/**
 * Fixed, distro-specific TigerVNC server packages for the ARM64 guest rootfs.
 */
internal data class ProotVncBootstrapPlan(
    val packageManager: String,
    val updateArguments: List<String>,
    val installArguments: List<String>,
    val verifyArguments: List<String>
)

internal object ProotVncBootstrapSpec {
    fun forDistribution(distribution: ProotDistribution): ProotVncBootstrapPlan =
        when (distribution) {
            ProotDistribution.DEBIAN, ProotDistribution.UBUNTU -> ProotVncBootstrapPlan(
                packageManager = "APT",
                updateArguments = listOf("apt-get", "update"),
                installArguments = listOf(
                    "/bin/sh", "-c",
                    "DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tigervnc-standalone-server tigervnc-tools"
                ),
                verifyArguments = listOf(
                    "/bin/sh", "-c",
                    "(command -v Xtigervnc || command -v Xvnc) >/dev/null 2>&1 && command -v vncpasswd >/dev/null 2>&1 && command -v startxfce4 >/dev/null 2>&1 && command -v dbus-launch >/dev/null 2>&1"
                )
            )
            ProotDistribution.ALPINE -> ProotVncBootstrapPlan(
                packageManager = "APK",
                updateArguments = listOf("apk", "update"),
                installArguments = listOf("apk", "add", "--no-cache", "tigervnc"),
                verifyArguments = listOf(
                    "/bin/sh", "-c",
                    "(command -v Xtigervnc || command -v Xvnc) >/dev/null 2>&1 && command -v vncpasswd >/dev/null 2>&1 && command -v startxfce4 >/dev/null 2>&1 && command -v dbus-launch >/dev/null 2>&1"
                )
            )
        }
}
