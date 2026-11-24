package com.example.bridge

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceAudioBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onEncodedAudioFrame: (ByteArray) -> Unit
) {
    private val sampleRate = 16_000
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private val _state = MutableStateFlow(VoiceTunnelState())
    val state: StateFlow<VoiceTunnelState> = _state.asStateFlow()

    private var recordJob: Job? = null
    private var timerJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (_state.value.isActive) return true
        if (!hasRecordAudioPermission()) return false

        val minRecBuf = AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding).coerceAtLeast(1280)
        val minPlayBuf = AudioTrack.getMinBufferSize(sampleRate, channelOut, encoding).coerceAtLeast(1280)

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = _state.value.isSpeakerOn

            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelIn,
                encoding,
                minRecBuf * 2
            )

            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return false
            }

            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(rec.audioSessionId)?.apply { enabled = true }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(rec.audioSessionId)?.apply { enabled = true }
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelOut)
                        .build()
                )
                .setBufferSizeInBytes(minPlayBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord = rec
            audioTrack = track
            rec.startRecording()
            track.play()

            _state.value = VoiceTunnelState(
                isActive = true,
                isMuted = false,
                isSpeakerOn = _state.value.isSpeakerOn,
                durationSeconds = 0
            )

            recordJob = scope.launch(Dispatchers.IO) {
                val frameBytes = 640 // 20ms @ 16kHz 16-bit mono
                val buffer = ByteArray(frameBytes)
                while (isActive && _state.value.isActive) {
                    val read = rec.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (!_state.value.isMuted) {
                            val amp = computeRmsAmplitude(buffer, read)
                            _state.update { it.copy(localAmplitude = amp) }
                            onEncodedAudioFrame(buffer.copyOf(read))
                        } else {
                            _state.update { it.copy(localAmplitude = 0f) }
                        }
                    }
                }
            }

            timerJob = scope.launch {
                while (isActive && _state.value.isActive) {
                    delay(1000L)
                    _state.update { it.copy(durationSeconds = it.durationSeconds + 1) }
                }
            }
            true
        } catch (_: Exception) {
            stop()
            false
        }
    }

    fun playIncomingPcm(pcmBytes: ByteArray) {
        if (!_state.value.isActive) return
        val track = audioTrack ?: return
        runCatching {
            track.write(pcmBytes, 0, pcmBytes.size)
            val amp = computeRmsAmplitude(pcmBytes, pcmBytes.size)
            _state.update { it.copy(remoteAmplitude = amp) }
        }
    }

    fun toggleMute() {
        _state.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        val next = !_state.value.isSpeakerOn
        _state.update { it.copy(isSpeakerOn = next) }
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = next
        }
    }

    fun stop() {
        _state.update { VoiceTunnelState(isActive = false, isSpeakerOn = it.isSpeakerOn) }
        recordJob?.cancel()
        recordJob = null
        timerJob?.cancel()
        timerJob = null
        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_NORMAL
        }
    }

    private fun computeRmsAmplitude(pcm: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sumSquares = 0.0
        var count = 0
        var i = 0
        while (i + 1 < length) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toDouble()
            sumSquares += sample * sample
            count++
            i += 2
        }
        if (count == 0) return 0f
        val rms = sqrt(sumSquares / count)
        return (rms / 12_000.0).toFloat().coerceIn(0f, 1f)
    }
}
