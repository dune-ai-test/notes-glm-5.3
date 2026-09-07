package com.fieldnotes.app.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Plays voice memos and audio blocks; reports progress for waveform coloring. */
class AudioPlayer(private val context: Context) {

    data class State(
        val activeId: String? = null,
        val isPlaying: Boolean = false,
        val progress: Float = 0f
    )

    private var player: MediaPlayer? = null
    private var currentId: String? = null
    private var tickJob: kotlinx.coroutines.Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun toggle(id: String, path: String) {
        if (currentId == id && player != null) {
            val p = player ?: return
            if (p.isPlaying) {
                p.pause()
                _state.value = _state.value.copy(isPlaying = false)
            } else {
                p.start()
                _state.value = _state.value.copy(isPlaying = true)
                tick()
            }
        } else {
            play(id, path)
        }
    }

    private fun play(id: String, path: String) {
        stop()
        if (path.isBlank() || !File(path).exists()) {
            Toast.makeText(context, "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val p = MediaPlayer()
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            p.setDataSource(path)
            p.setOnCompletionListener {
                stop()
            }
            p.prepare()
            p.start()
        } catch (_: Exception) {
            try {
                p.release()
            } catch (_: Exception) {
            }
            Toast.makeText(context, "Couldn't play this recording", Toast.LENGTH_SHORT).show()
            return
        }
        player = p
        currentId = id
        _state.value = State(activeId = id, isPlaying = true, progress = 0f)
        tick()
    }

    private fun tick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (player?.isPlaying == true) {
                val duration = player?.duration ?: 0
                val position = player?.currentPosition ?: 0
                if (duration > 0) {
                    _state.value = _state.value.copy(progress = position.toFloat() / duration)
                }
                delay(100)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        currentId = null
        _state.value = State()
    }
}
