package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.log10
import kotlin.math.sqrt

class RemoteAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "RemoteAudioManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val audioScope = CoroutineScope(Dispatchers.IO + Job())
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _ambientDecibels = MutableStateFlow(0f)
    val ambientDecibels: StateFlow<Float> = _ambientDecibels.asStateFlow()

    private val _latestAudioChunk = MutableStateFlow<ByteArray?>(null)
    val latestAudioChunk: StateFlow<ByteArray?> = _latestAudioChunk.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (_isRecording.value) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Audio recording permission not granted")
            return false
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = audioScope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)

                while (isActive && _isRecording.value) {
                    val readShorts = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readShorts > 0) {
                        // Calculate RMS decibels for ambient volume meter
                        var sum = 0.0
                        for (i in 0 until readShorts) {
                            val sample = buffer[i]
                            // Convert short to byte array (little-endian)
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readShorts)
                        val db = if (rms > 1) (20 * log10(rms)).toFloat().coerceIn(0f, 100f) else 0f
                        _ambientDecibels.value = db

                        val chunkBytes = byteBuffer.copyOf(readShorts * 2)
                        _latestAudioChunk.value = chunkBytes
                    }
                }
            }
            Log.i(TAG, "Remote ambient audio recording started successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            stopListening()
            return false
        }
    }

    fun stopListening() {
        _isRecording.value = false
        _ambientDecibels.value = 0f
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }
        Log.i(TAG, "Remote ambient audio stopped")
    }
}
