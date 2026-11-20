package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CanvasMode
import com.example.model.MusicalScale
import com.example.model.OrbitPattern
import com.example.model.OrbitVoicing
import com.example.model.SynthParams
import com.example.model.SynthTimbre
import com.example.model.Temperament
import com.example.model.TouchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.torusDataStore: DataStore<Preferences> by preferencesDataStore(name = "torustone_prefs")

data class PersistedStudioConfig(
    val synthParams: SynthParams = SynthParams(),
    val octave: Int = 4,
    val scale: MusicalScale = MusicalScale.MAJOR,
    val canvasMode: CanvasMode = CanvasMode.TORUS_3D,
    val touchMode: TouchMode = TouchMode.TRIAD,
    val orbitBpm: Int = 92,
    val orbitRootPitchClass: Int = 0,
    val orbitPattern: OrbitPattern = OrbitPattern.NEO_RIEMANNIAN_PLR,
    val orbitVoicing: OrbitVoicing = OrbitVoicing.CHORD
)

class SynthPreferencesRepository(private val context: Context) {

    private object Keys {
        val TIMBRE = stringPreferencesKey("timbre")
        val WARMTH = floatPreferencesKey("warmth")
        val ECHO_MIX = floatPreferencesKey("echo_mix")
        val ECHO_FEEDBACK = floatPreferencesKey("echo_feedback")
        val ATTACK_MS = floatPreferencesKey("attack_ms")
        val RELEASE_MS = floatPreferencesKey("release_ms")
        val DETUNE_CENTS = floatPreferencesKey("detune_cents")
        val MASTER_GAIN = floatPreferencesKey("master_gain")
        val TEMPERAMENT = stringPreferencesKey("temperament")
        val REFERENCE_A4 = intPreferencesKey("reference_a4")
        val OCTAVE = intPreferencesKey("octave")
        val SCALE = stringPreferencesKey("scale")
        val CANVAS_MODE = stringPreferencesKey("canvas_mode")
        val TOUCH_MODE = stringPreferencesKey("touch_mode")
        val ORBIT_BPM = intPreferencesKey("orbit_bpm")
        val ORBIT_ROOT = intPreferencesKey("orbit_root")
        val ORBIT_PATTERN = stringPreferencesKey("orbit_pattern")
        val ORBIT_VOICING = stringPreferencesKey("orbit_voicing")
    }

    val configFlow: Flow<PersistedStudioConfig> = context.torusDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            PersistedStudioConfig(
                synthParams = SynthParams(
                    timbre = prefs[Keys.TIMBRE]?.let { runCatching { SynthTimbre.valueOf(it) }.getOrNull() }
                        ?: SynthTimbre.WARM_RHODES,
                    warmth = prefs[Keys.WARMTH] ?: 0.72f,
                    echoMix = prefs[Keys.ECHO_MIX] ?: 0.34f,
                    echoFeedback = prefs[Keys.ECHO_FEEDBACK] ?: 0.42f,
                    attackMs = prefs[Keys.ATTACK_MS] ?: 24f,
                    releaseMs = prefs[Keys.RELEASE_MS] ?: 620f,
                    detuneCents = prefs[Keys.DETUNE_CENTS] ?: 10f,
                    masterGain = prefs[Keys.MASTER_GAIN] ?: 0.78f,
                    temperament = prefs[Keys.TEMPERAMENT]?.let { runCatching { Temperament.valueOf(it) }.getOrNull() }
                        ?: Temperament.EQUAL_12TET,
                    referenceA4Hz = prefs[Keys.REFERENCE_A4] ?: 440
                ),
                octave = (prefs[Keys.OCTAVE] ?: 4).coerceIn(2, 6),
                scale = prefs[Keys.SCALE]?.let { runCatching { MusicalScale.valueOf(it) }.getOrNull() }
                    ?: MusicalScale.MAJOR,
                canvasMode = prefs[Keys.CANVAS_MODE]?.let { runCatching { CanvasMode.valueOf(it) }.getOrNull() }
                    ?: CanvasMode.TORUS_3D,
                touchMode = prefs[Keys.TOUCH_MODE]?.let { runCatching { TouchMode.valueOf(it) }.getOrNull() }
                    ?: TouchMode.TRIAD,
                orbitBpm = (prefs[Keys.ORBIT_BPM] ?: 92).coerceIn(45, 160),
                orbitRootPitchClass = (prefs[Keys.ORBIT_ROOT] ?: 0).coerceIn(0, 11),
                orbitPattern = prefs[Keys.ORBIT_PATTERN]?.let { runCatching { OrbitPattern.valueOf(it) }.getOrNull() }
                    ?: OrbitPattern.NEO_RIEMANNIAN_PLR,
                orbitVoicing = prefs[Keys.ORBIT_VOICING]?.let { runCatching { OrbitVoicing.valueOf(it) }.getOrNull() }
                    ?: OrbitVoicing.CHORD
            )
        }

    suspend fun saveConfig(config: PersistedStudioConfig) {
        context.torusDataStore.edit { prefs ->
            prefs[Keys.TIMBRE] = config.synthParams.timbre.name
            prefs[Keys.WARMTH] = config.synthParams.warmth
            prefs[Keys.ECHO_MIX] = config.synthParams.echoMix
            prefs[Keys.ECHO_FEEDBACK] = config.synthParams.echoFeedback
            prefs[Keys.ATTACK_MS] = config.synthParams.attackMs
            prefs[Keys.RELEASE_MS] = config.synthParams.releaseMs
            prefs[Keys.DETUNE_CENTS] = config.synthParams.detuneCents
            prefs[Keys.MASTER_GAIN] = config.synthParams.masterGain
            prefs[Keys.TEMPERAMENT] = config.synthParams.temperament.name
            prefs[Keys.REFERENCE_A4] = config.synthParams.referenceA4Hz
            prefs[Keys.OCTAVE] = config.octave
            prefs[Keys.SCALE] = config.scale.name
            prefs[Keys.CANVAS_MODE] = config.canvasMode.name
            prefs[Keys.TOUCH_MODE] = config.touchMode.name
            prefs[Keys.ORBIT_BPM] = config.orbitBpm
            prefs[Keys.ORBIT_ROOT] = config.orbitRootPitchClass
            prefs[Keys.ORBIT_PATTERN] = config.orbitPattern.name
            prefs[Keys.ORBIT_VOICING] = config.orbitVoicing.name
        }
    }
}
