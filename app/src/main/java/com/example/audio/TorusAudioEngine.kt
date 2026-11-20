package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.example.model.SynthParams
import com.example.model.SynthTimbre
import com.example.model.TonnetzMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time polyphonic DSP synthesizer engine backed by Android [AudioTrack].
 * Renders 12 voices with 5 acoustic timbres, smooth ADSR envelopes, detuned chorus,
 * low-pass filter, circular-buffer ambient echo delay, and live oscilloscope telemetry.
 */
class TorusAudioEngine {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val TWO_PI = 2.0 * PI
        private const val MAX_VOICES = 12
        private const val DELAY_SECONDS = 0.29
        private const val DELAY_BUFFER_SIZE = (SAMPLE_RATE * DELAY_SECONDS).toInt()
        private const val BLOCK_FRAMES = 512
        private const val SCOPE_SAMPLES = 96
    }

    private class Voice {
        var midiNote: Int = -1
        var active: Boolean = false
        var gateOpen: Boolean = false
        var frequencyHz: Double = 440.0
        var phase1: Double = 0.0
        var phase2: Double = 0.0
        var modPhase: Double = 0.0
        var env: Double = 0.0
        var ageSamples: Long = 0L
        var velocity: Double = 0.85
    }

    private val voicesLock = Any()
    private val voices = Array(MAX_VOICES) { Voice() }

    @Volatile
    private var params: SynthParams = SynthParams()

    @Volatile
    private var justRootPitchClass: Int = 0

    private val running = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private val _waveform = MutableStateFlow(FloatArray(SCOPE_SAMPLES))
    val waveform: StateFlow<FloatArray> = _waveform.asStateFlow()

    private val _peakLevel = MutableStateFlow(0f)
    val peakLevel: StateFlow<Float> = _peakLevel.asStateFlow()

    fun updateParams(newParams: SynthParams, rootPitchClass: Int = justRootPitchClass) {
        val tuningChanged =
            newParams.temperament != params.temperament ||
                newParams.referenceA4Hz != params.referenceA4Hz ||
                rootPitchClass != justRootPitchClass
        params = newParams
        justRootPitchClass = rootPitchClass
        if (tuningChanged) {
            synchronized(voicesLock) {
                for (v in voices) {
                    if (v.active && v.midiNote >= 0) {
                        v.frequencyHz = TonnetzMath.midiToFrequency(
                            midi = v.midiNote,
                            temperament = newParams.temperament,
                            referenceA4Hz = newParams.referenceA4Hz,
                            justRootPitchClass = rootPitchClass
                        )
                    }
                }
            }
        }
    }

    fun noteOn(midi: Int, velocity: Float = 0.85f) {
        val currentParams = params
        val freq = TonnetzMath.midiToFrequency(
            midi = midi,
            temperament = currentParams.temperament,
            referenceA4Hz = currentParams.referenceA4Hz,
            justRootPitchClass = justRootPitchClass
        )
        synchronized(voicesLock) {
            // Re-trigger existing voice for this MIDI note cleanly
            val existing = voices.firstOrNull { it.active && it.midiNote == midi }
            val target = existing
                ?: voices.firstOrNull { !it.active }
                ?: voices.minByOrNull { if (it.gateOpen) it.env + 10.0 else it.env }!!

            target.midiNote = midi
            target.frequencyHz = freq
            target.velocity = velocity.toDouble().coerceIn(0.15, 1.0)
            target.gateOpen = true
            if (!target.active) {
                target.phase1 = 0.0
                target.phase2 = 0.0
                target.modPhase = 0.0
                target.env = 0.0
            }
            target.ageSamples = 0L
            target.active = true
        }
    }

    fun noteOff(midi: Int) {
        synchronized(voicesLock) {
            for (v in voices) {
                if (v.active && v.midiNote == midi) {
                    v.gateOpen = false
                }
            }
        }
    }

    fun setActiveNotes(targetMidiNotes: Set<Int>) {
        synchronized(voicesLock) {
            for (v in voices) {
                if (v.active && v.midiNote !in targetMidiNotes) {
                    v.gateOpen = false
                }
            }
        }
        for (midi in targetMidiNotes) {
            val alreadyGated = synchronized(voicesLock) {
                voices.any { it.active && it.gateOpen && it.midiNote == midi }
            }
            if (!alreadyGated) {
                noteOn(midi)
            }
        }
    }

    fun allNotesOff() {
        synchronized(voicesLock) {
            for (v in voices) {
                v.gateOpen = false
            }
        }
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        audioThread = Thread(::renderLoop, "TorusToneAudioThread").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        audioThread?.let { thread ->
            try {
                thread.join(350)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        audioThread = null
    }

    private fun renderLoop() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (_: Throwable) {
            // Safe fallback in JVM test environments
        }

        val minBufBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufBytes <= 0) {
            running.set(false)
            return
        }

        val bufferBytes = maxOf(minBufBytes, BLOCK_FRAMES * 2 * 2)
        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            running.set(false)
            return
        }

        val pcmBuffer = ShortArray(BLOCK_FRAMES)
        val delayLine = DoubleArray(DELAY_BUFFER_SIZE)
        var delayIndex = 0
        var lowPassState = 0.0
        var scopeFrameCounter = 0

        try {
            audioTrack.play()
            while (running.get()) {
                val p = params
                val attackStep = 1.0 / ((p.attackMs.coerceIn(4f, 500f) / 1000.0) * SAMPLE_RATE)
                val releaseCoeff = exp(-4.8 / ((p.releaseMs.coerceIn(60f, 2500f) / 1000.0) * SAMPLE_RATE))
                val cutoffHz = TonnetzMath.filterCutoffHz(p.warmth).toDouble()
                val rc = 1.0 / (TWO_PI * cutoffHz)
                val dt = 1.0 / SAMPLE_RATE
                val lpAlpha = (dt / (rc + dt)).coerceIn(0.01, 0.99)
                val detuneRatio = Math.pow(2.0, p.detuneCents.toDouble() / 1200.0)
                val echoMix = p.echoMix.toDouble().coerceIn(0.0, 0.78)
                val echoFb = p.echoFeedback.toDouble().coerceIn(0.0, 0.78)
                val gain = p.masterGain.toDouble().coerceIn(0.05, 1.0)
                val timbre = p.timbre

                var blockPeak = 0.0
                val captureScope = (scopeFrameCounter++ % 2 == 0)
                val scopeSnap = if (captureScope) FloatArray(SCOPE_SAMPLES) else null

                for (i in 0 until BLOCK_FRAMES) {
                    var mix = 0.0
                    synchronized(voicesLock) {
                        for (v in voices) {
                            if (!v.active) continue

                            if (v.gateOpen) {
                                v.env += attackStep
                                if (v.env > 1.0) v.env = 1.0
                            } else {
                                v.env *= releaseCoeff
                                if (v.env < 0.0004) {
                                    v.env = 0.0
                                    v.active = false
                                    continue
                                }
                            }

                            val inc1 = TWO_PI * v.frequencyHz / SAMPLE_RATE
                            val inc2 = TWO_PI * (v.frequencyHz * detuneRatio) / SAMPLE_RATE
                            v.phase1 += inc1
                            if (v.phase1 >= TWO_PI) v.phase1 -= TWO_PI
                            v.phase2 += inc2
                            if (v.phase2 >= TWO_PI) v.phase2 -= TWO_PI

                            val sample = synthesizeVoice(timbre, v)
                            mix += sample * v.env * v.velocity * 0.24
                            v.ageSamples++
                        }
                    }

                    // One-pole musical low-pass warmth filter
                    lowPassState += lpAlpha * (mix - lowPassState)
                    val filtered = lowPassState

                    // Circular-buffer ambient echo
                    val delayed = delayLine[delayIndex]
                    delayLine[delayIndex] = tanh(filtered + delayed * echoFb)
                    delayIndex++
                    if (delayIndex >= DELAY_BUFFER_SIZE) delayIndex = 0

                    val wetDry = filtered * (1.0 - echoMix * 0.35) + delayed * echoMix
                    val limited = tanh(wetDry * gain * 1.45)

                    val absVal = kotlin.math.abs(limited)
                    if (absVal > blockPeak) blockPeak = absVal

                    pcmBuffer[i] = (limited * 32767.0).toInt().coerceIn(-32767, 32767).toShort()

                    if (scopeSnap != null) {
                        val scopeIdx = (i * SCOPE_SAMPLES) / BLOCK_FRAMES
                        if (scopeIdx < SCOPE_SAMPLES) {
                            scopeSnap[scopeIdx] = limited.toFloat()
                        }
                    }
                }

                if (scopeSnap != null) {
                    _waveform.value = scopeSnap
                    _peakLevel.value = blockPeak.toFloat()
                }

                audioTrack.write(pcmBuffer, 0, BLOCK_FRAMES)
            }
        } catch (_: Throwable) {
            // Audio device interrupted or released
        } finally {
            try {
                audioTrack.pause()
                audioTrack.flush()
                audioTrack.release()
            } catch (_: Throwable) {
                // Ignore cleanup exceptions
            }
        }
    }

    private fun synthesizeVoice(timbre: SynthTimbre, v: Voice): Double {
        return when (timbre) {
            SynthTimbre.CELESTIAL_SINE -> {
                val fundamental = sin(v.phase1)
                val shimmer = sin(v.phase2 * 2.0) * 0.22 + sin(v.phase1 * 3.0) * 0.06
                (fundamental * 0.78 + shimmer)
            }

            SynthTimbre.WARM_RHODES -> {
                // Decaying FM bell index for authentic electric tine sparkle
                val ageSec = v.ageSamples.toDouble() / SAMPLE_RATE
                val modIndex = 1.6 * exp(-ageSec * 4.5) + 0.12
                val mod = sin(v.phase1 * 2.0) * modIndex
                val body = sin(v.phase1 + mod) * 0.76
                val chorus = sin(v.phase2) * 0.28
                body + chorus
            }

            SynthTimbre.ANALOG_BRASS -> {
                // Soft band-limited additive sawtooth (first 6 harmonics) + detuned partner
                val p1 = v.phase1
                val p2 = v.phase2
                val saw1 = sin(p1) +
                    0.50 * sin(2.0 * p1) +
                    0.33 * sin(3.0 * p1) +
                    0.25 * sin(4.0 * p1) +
                    0.18 * sin(5.0 * p1) +
                    0.12 * sin(6.0 * p1)
                val saw2 = sin(p2) + 0.48 * sin(2.0 * p2) + 0.28 * sin(3.0 * p2)
                (saw1 * 0.38 + saw2 * 0.26)
            }

            SynthTimbre.GLASS_ORGAN -> {
                // Classic drawbar harmonics: 8' (1x), 4' (2x), 2-2/3' (3x fifth), 2' (4x)
                val p1 = v.phase1
                val p2 = v.phase2
                (sin(p1) * 0.55 +
                    sin(p1 * 2.0) * 0.30 +
                    sin(p2 * 3.0) * 0.16 +
                    sin(p1 * 4.0) * 0.11)
            }

            SynthTimbre.SUB_DRONE -> {
                // Warm triangle + sub-octave sine
                val norm1 = v.phase1 / TWO_PI
                val tri1 = 4.0 * kotlin.math.abs(norm1 - 0.5) - 1.0
                val norm2 = v.phase2 / TWO_PI
                val tri2 = 4.0 * kotlin.math.abs(norm2 - 0.5) - 1.0
                val sub = sin(v.phase1 * 0.5)
                (tri1 * 0.45 + tri2 * 0.30 + sub * 0.38)
            }
        }
    }
}
