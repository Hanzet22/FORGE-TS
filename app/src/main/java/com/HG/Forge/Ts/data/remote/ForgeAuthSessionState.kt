package com.HG.Forge.Ts.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Process-wide session shared by the central gate and remote connector panels. */
object ForgeAuthSessionState {
    var session: SupabaseAuthSession? by mutableStateOf(null)
}
