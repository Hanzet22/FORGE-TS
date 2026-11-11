package com.HG.Forge.Ts.data.simulation

import android.os.Build
import java.io.File

enum class MagicDeskX11GateState {
    ANDROID_API_TOO_LOW,
    ABI_UNSUPPORTED,
    NATIVE_LIBRARY_MISSING,
    HOST_BRIDGE_NOT_IMPLEMENTED,
    PLATFORM_ELIGIBLE
}

data class MagicDeskX11GateResult(
    val state: MagicDeskX11GateState,
    val eligibleForNativeInitialization: Boolean,
    val minimumAndroidApi: Int,
    val requiredAbi: String,
    val detail: String
)

/**
 * Platform admission policy for the reviewed MagicDesk embedded-X11 candidate.
 *
 * This is deliberately not a DisplaySessionAdapter and never reports a running
 * or available display. Passing this gate only means that platform prerequisites
 * are met; a future JNI host must still load the native engine and confirm the
 * X server's readiness before a display session can start.
 */
object MagicDeskX11PlatformGate {
    const val MINIMUM_ANDROID_API = 34
    const val REQUIRED_ABI = "arm64-v8a"
    const val NATIVE_LIBRARY_NAME = "Xlorie"

    // Keep false until the JNI/Binder lifecycle and output-surface host are implemented.
    private const val HOST_BRIDGE_IMPLEMENTED = false

    fun evaluate(
        androidApi: Int,
        supportedAbis: List<String>,
        nativeLibraryPackaged: Boolean,
        hostBridgeImplemented: Boolean = HOST_BRIDGE_IMPLEMENTED
    ): MagicDeskX11GateResult {
        fun result(state: MagicDeskX11GateState, eligible: Boolean, detail: String) =
            MagicDeskX11GateResult(
                state = state,
                eligibleForNativeInitialization = eligible,
                minimumAndroidApi = MINIMUM_ANDROID_API,
                requiredAbi = REQUIRED_ABI,
                detail = detail
            )

        if (androidApi < MINIMUM_ANDROID_API) {
            return result(
                MagicDeskX11GateState.ANDROID_API_TOO_LOW,
                false,
                "Embedded X11 requires Android 14 (API 34) or newer. The app itself remains Android 10+."
            )
        }
        if (REQUIRED_ABI !in supportedAbis) {
            return result(
                MagicDeskX11GateState.ABI_UNSUPPORTED,
                false,
                "Embedded X11 candidate currently targets arm64-v8a only."
            )
        }
        if (!nativeLibraryPackaged) {
            return result(
                MagicDeskX11GateState.NATIVE_LIBRARY_MISSING,
                false,
                "The native X11 engine is not packaged in this build."
            )
        }
        if (!hostBridgeImplemented) {
            return result(
                MagicDeskX11GateState.HOST_BRIDGE_NOT_IMPLEMENTED,
                false,
                "Platform prerequisites pass, but the Android JNI/session host is not implemented."
            )
        }
        return result(
            MagicDeskX11GateState.PLATFORM_ELIGIBLE,
            true,
            "Platform prerequisites pass. Native initialization and server readiness are still unverified."
        )
    }

    fun evaluateCurrentDevice(nativeLibraryPackaged: Boolean): MagicDeskX11GateResult =
        evaluate(
            androidApi = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            nativeLibraryPackaged = nativeLibraryPackaged
        )

    fun isNativeLibraryPackaged(nativeLibraryDir: String): Boolean =
        File(nativeLibraryDir, "lib$NATIVE_LIBRARY_NAME.so").isFile
}
