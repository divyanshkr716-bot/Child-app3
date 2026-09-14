package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

class ScreenCastEngine(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCastEngine"
        private const val VIRTUAL_DISPLAY_NAME = "AirDroidVirtualDisplay"
    }

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _latestFrame = MutableStateFlow<ByteArray?>(null)
    val latestFrame: StateFlow<ByteArray?> = _latestFrame.asStateFlow()

    // Configurable frame rate and scale
    private var targetFps = 20
    private var scaleFactor = 0.5f // 540x960 instead of 1080x1920 for bandwidth optimization
    private var lastFrameTime = 0L

    fun createScreenCaptureIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    fun startCasting(resultCode: Int, data: Intent, metrics: DisplayMetrics) {
        stopCasting()
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to obtain MediaProjection")
                return
            }

            val targetWidth = (metrics.widthPixels * scaleFactor).toInt().coerceAtLeast(360)
            val targetHeight = (metrics.heightPixels * scaleFactor).toInt().coerceAtLeast(640)
            val densityDpi = (metrics.densityDpi * scaleFactor).toInt().coerceAtLeast(160)

            imageReader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)

            imageReader?.setOnImageAvailableListener({ reader ->
                val now = System.currentTimeMillis()
                val minIntervalMs = 1000 / targetFps
                if (now - lastFrameTime < minIntervalMs) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                lastFrameTime = now

                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * image.width

                    val bitmap = Bitmap.createBitmap(
                        image.width + rowPadding / pixelStride,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    // Crop to actual width if padded
                    val finalBitmap = if (rowPadding != 0) {
                        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                    } else bitmap

                    val stream = ByteArrayOutputStream()
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                    _latestFrame.value = stream.toByteArray()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing screen frame", e)
                } finally {
                    image.close()
                }
            }, mainHandler)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                targetWidth,
                targetHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            _isStreaming.value = true
            Log.i(TAG, "Screen casting successfully started at ${targetWidth}x${targetHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startCasting", e)
            stopCasting()
        }
    }

    fun adjustQuality(fps: Int, scale: Float) {
        targetFps = fps.coerceIn(10, 30)
        scaleFactor = scale.coerceIn(0.25f, 1.0f)
    }

    fun stopCasting() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            _isStreaming.value = false
            _latestFrame.value = null
            Log.i(TAG, "Screen casting stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen cast", e)
        }
    }
}
