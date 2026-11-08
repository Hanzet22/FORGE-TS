package com.HG.Forge.Ts

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process handoff for Supabase auth callback links received by MainActivity.
 * The callback contains short-lived auth material; it is never logged or persisted.
 */
object AuthDeepLinkBus {
    private val events = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val callbacks = events.asSharedFlow()

    suspend fun publish(uri: String) {
        if (uri.startsWith("com.hg.forgets://auth/callback")) {
            events.emit(uri)
        }
    }
}
