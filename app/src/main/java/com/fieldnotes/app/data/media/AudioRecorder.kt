package com.fieldnotes.app.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Wraps MediaRecorder for voice memos. While recording it polls the peak
 * amplitude every 100ms so the UI can draw a live waveform.
 */
class AudioRecorder(private val context: Context) {

    data class State(
        val isRecording: Boolean = false,
        val elapsedMs: Long = 0L,
        val amplitudes: List<Int> = emptyList()
    )

    data class Result(val path: String, val durationMs: Long, val amplitudes: List<Int>)

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var job: Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun isRecording(): Boolean = _state.value.isRecording

    fun start(scope: CoroutineScope, dir: File, bitRate: Int = 192_000): Boolean {
        if (_state.value.isRecording) return false
        return try {
            dir.mkdirs()
            val file = File(dir, "rec_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(bitRate)
            r.setAudioChannels(1)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            outputFile = file
            _state.value = State(isRecording = true)
            job = scope.launch(Dispatchers.Default) {
                var elapsed = 0L
                val amplitudes = mutableListOf<Int>()
                while (isActive) {
                    delay(100)
                    val raw = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    amplitudes += normalize(raw)
                    elapsed += 100
                    _state.value = State(
                        isRecording = true,
                        elapsedMs = elapsed,
                        amplitudes = amplitudes.toList()
                    )
                }
            }
            true
        } catch (_: Exception) {
            release()
            _state.value = State()
            false
        }
    }

    fun stop(): Result? {
        val state = _state.value
        val r = recorder
        val file = outputFile
        job?.cancel()
        job = null
        recorder = null
        outputFile = null
        _state.value = State()
        if (r == null || file == null || !state.isRecording) return null
        return try {
            r.stop()
            r.release()
            Result(file.absolutePath, state.elapsedMs.coerceAtLeast(300), state.amplitudes)
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // stop() fails if nothing was captured; safe to ignore on cancel.
        }
        release()
        outputFile?.delete()
        outputFile = null
        _state.value = State()
    }

    private fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    private fun normalize(raw: Int): Int = (raw / 1400).coerceIn(3, 24)
}
