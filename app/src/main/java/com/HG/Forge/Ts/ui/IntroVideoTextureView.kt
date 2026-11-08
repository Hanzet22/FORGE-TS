package com.HG.Forge.Ts.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes

/**
 * TextureView-backed intro player. Unlike VideoView, this lets us apply a
 * center-crop transform so the video fills the complete available display.
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

    init {
        setBackgroundColor(Color.BLACK)
        isOpaque = true
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        videoSurface = Surface(surfaceTexture)
        preparePlayer()
    }

    private fun preparePlayer() {
        if (mediaPlayer != null || videoSurface == null) return

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
                applyCenterCrop()
                it.isLooping = false
                it.start()
            }
            player.setOnCompletionListener { finishOnce() }
            player.setOnErrorListener { _, _, _ ->
                finishOnce()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            releasePlayer()
            finishOnce()
        }
    }

    private fun applyCenterCrop() {
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) return

        val scale = maxOf(
            width.toFloat() / videoWidth.toFloat(),
            height.toFloat() / videoHeight.toFloat()
        )
        val scaledWidth = videoWidth * scale
        val scaledHeight = videoHeight * scale
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate((width - scaledWidth) / 2f, (height - scaledHeight) / 2f)
        }
        setTransform(matrix)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCenterCrop()
    }

    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
        applyCenterCrop()
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
        releasePlayer()
        videoSurface?.release()
        videoSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) = Unit

    private fun finishOnce() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.runCatching { stop() }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDetachedFromWindow() {
        releasePlayer()
        videoSurface?.release()
        videoSurface = null
        super.onDetachedFromWindow()
    }
}
