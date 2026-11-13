package com.HG.Forge.Ts.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.SystemClock
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics

/**
 * TextureView-backed intro player with an adaptive center-crop transform.
 *
 * The transform is recalculated whenever the available view size changes, so
 * portrait, landscape, and resizable multi-window bounds use the same player.
 */
internal class IntroVideoTextureView(
    context: Context,
    @RawRes private val videoResource: Int,
    private val onFinished: () -> Unit
) : TextureView(context), TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var videoSurface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var finished = false
    private var terminalEventLogged = false
    private var firstFrameLogged = false
    private var playbackTraceId: String? = null
    private var playbackStartedAtMs: Long? = null
    private var lastLayoutWidth = 0
    private var lastLayoutHeight = 0

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        videoSurface = Surface(surfaceTexture)
        preparePlayer()
        logLayoutIfChanged(width, height)
    }

    private fun preparePlayer() {
        if (mediaPlayer != null || videoSurface == null) return

        val traceId = ForgeDiagnostics.newTraceId()
        playbackTraceId = traceId
        ForgeDiagnostics.log(
            context, "INFO", "IntroVideo", "intro.prepare.started", traceId = traceId
        )

        val player = MediaPlayer()
        mediaPlayer = player

        try {
            resources.openRawResourceFd(videoResource).use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            player.setSurface(videoSurface)
            player.setOnVideoSizeChangedListener { _, width, height ->
                videoWidth = width
                videoHeight = height
                applyCenterCrop()
            }
            player.setOnPreparedListener {
                ForgeDiagnostics.log(
                    context, "INFO", "IntroVideo", "intro.player.prepared", traceId = traceId
                )
                applyCenterCrop()
                try {
                    it.isLooping = false
                    it.start()
                    playbackStartedAtMs = SystemClock.elapsedRealtime()
                    ForgeDiagnostics.log(
                        context, "INFO", "IntroVideo", "intro.playback.started", traceId = traceId
                    )
                } catch (error: Exception) {
                    ForgeDiagnostics.log(
                        context, "ERROR", "IntroVideo", "intro.playback.start_failed",
                        error, traceId
                    )
                    finishOnce("start_failed")
                }
            }
            player.setOnCompletionListener { finishOnce("completed") }
            player.setOnErrorListener { _, what, extra ->
                ForgeDiagnostics.log(
                    context, "ERROR", "IntroVideo",
                    "intro.player.error_${what}_${extra}", traceId = traceId
                )
                finishOnce("error")
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            ForgeDiagnostics.log(
                context, "ERROR", "IntroVideo", "intro.prepare.exception", error, traceId
            )
            finishOnce("prepare_failed")
        }
    }

    /**
     * Fill the currently available bounds without stretching the source video.
     * Recomputed for every view-size change, including split-screen resizing.
     */
    private fun applyCenterCrop() {
        val viewWidth = width
        val viewHeight = height
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return

        val scale = maxOf(
            viewWidth.toFloat() / videoWidth.toFloat(),
            viewHeight.toFloat() / videoHeight.toFloat()
        )
        val scaledWidth = videoWidth * scale
        val scaledHeight = videoHeight * scale
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                (viewWidth - scaledWidth) / 2f,
                (viewHeight - scaledHeight) / 2f
            )
        }
        setTransform(matrix)
        invalidate()
    }

    private fun logLayoutIfChanged(viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        if (viewWidth == lastLayoutWidth && viewHeight == lastLayoutHeight) return

        lastLayoutWidth = viewWidth
        lastLayoutHeight = viewHeight
        val orientation = if (viewWidth >= viewHeight) "landscape" else "portrait"
        ForgeDiagnostics.log(
            context,
            "INFO",
            "IntroVideo",
            "intro.layout.changed_${viewWidth}x${viewHeight}_${orientation}",
            traceId = playbackTraceId
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCenterCrop()
        logLayoutIfChanged(w, h)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        applyCenterCrop()
        logLayoutIfChanged(width, height)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        ForgeDiagnostics.log(
            context, "DEBUG", "IntroVideo", "intro.surface.destroyed",
            traceId = playbackTraceId
        )
        releasePlayer("surface_destroyed")
        videoSurface?.release()
        videoSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (!firstFrameLogged && playbackStartedAtMs != null && !terminalEventLogged) {
            firstFrameLogged = true
            ForgeDiagnostics.log(
                context, "INFO", "IntroVideo", "intro.playback.first_frame",
                traceId = playbackTraceId
            )
        }
    }

    private fun finishOnce(reason: String) {
        if (finished) return
        finished = true
        terminalEventLogged = true

        val traceId = playbackTraceId
        val terminalEvent = if (reason == "completed") {
            "intro.playback.completed"
        } else {
            "intro.playback.terminated_${reason}"
        }
        ForgeDiagnostics.log(context, "INFO", "IntroVideo", terminalEvent, traceId = traceId)

        playbackStartedAtMs?.let { startedAt ->
            val durationMs = (SystemClock.elapsedRealtime() - startedAt)
                .coerceIn(0L, MAX_PLAYBACK_DURATION_MS)
            ForgeDiagnostics.log(
                context,
                "INFO",
                "IntroVideo",
                "intro.playback.duration_ms_${durationMs}",
                traceId = traceId
            )
        }

        releasePlayer("playback_finished")
        onFinished()
    }

    private fun releasePlayer(reason: String) {
        val player = mediaPlayer ?: return
        mediaPlayer = null

        if (playbackStartedAtMs != null && !terminalEventLogged) {
            terminalEventLogged = true
            ForgeDiagnostics.log(
                context, "WARNING", "IntroVideo",
                "intro.playback.terminated_${reason}",
                traceId = playbackTraceId
            )
            val durationMs = (SystemClock.elapsedRealtime() - playbackStartedAtMs!!)
                .coerceIn(0L, MAX_PLAYBACK_DURATION_MS)
            ForgeDiagnostics.log(
                context, "INFO", "IntroVideo",
                "intro.playback.duration_ms_${durationMs}",
                traceId = playbackTraceId
            )
        } else if (!terminalEventLogged) {
            ForgeDiagnostics.log(
                context, "DEBUG", "IntroVideo",
                "intro.player.released_before_start_${reason}",
                traceId = playbackTraceId
            )
        }

        runCatching { player.stop() }
        runCatching { player.release() }
        playbackStartedAtMs = null
    }

    override fun onDetachedFromWindow() {
        releasePlayer("view_detached")
        videoSurface?.release()
        videoSurface = null
        super.onDetachedFromWindow()
    }

    private companion object {
        const val MAX_PLAYBACK_DURATION_MS = 600_000L
    }
}
