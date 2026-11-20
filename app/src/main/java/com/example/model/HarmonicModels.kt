package com.example.model

import kotlin.math.pow
import kotlin.math.roundToInt

enum class SynthTimbre(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    CELESTIAL_SINE(
        displayName = "Celestial Sine",
        shortLabel = "Sine",
        description = "Pure fundamental with gentle octave shimmer"
    ),
    WARM_RHODES(
        displayName = "Warm Rhodes",
        shortLabel = "Rhodes",
        description = "FM electric tine bell with warm decay"
    ),
    ANALOG_BRASS(
        displayName = "Analog Brass",
        shortLabel = "Brass",
        description = "Rich sawtooth harmonics through warm filter"
    ),
    GLASS_ORGAN(
        displayName = "Glass Organ",
        shortLabel = "Organ",
        description = "Additive fifth & octave harmonic drawbars"
    ),
    SUB_DRONE(
        displayName = "Sub Drone",
        shortLabel = "Drone",
        description = "Deep resonant triangle & sub-octave pad"
    )
}

enum class Temperament {
    EQUAL_12TET,
    JUST_INTONATION
}

enum class CanvasMode {
    TORUS_3D,
    TONNETZ_GRID
}

enum class TouchMode {
    TRIAD,
    SINGLE_NOTE
}

enum class MusicalScale(
    val displayName: String,
    val intervals: Set<Int>
) {
    CHROMATIC("Chromatic", setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)),
    MAJOR("Major", setOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR("Minor", setOf(0, 2, 3, 5, 7, 8, 10)),
    DORIAN("Dorian", setOf(0, 2, 3, 5, 7, 9, 10)),
    LYDIAN("Lydian", setOf(0, 2, 4, 6, 7, 9, 11)),
    PENTATONIC("Pentatonic", setOf(0, 2, 4, 7, 9))
}

enum class OrbitVoicing {
    CHORD,
    ARP_UP,
    TORUS_WAVE
}

data class OrbitChordStep(
    val rootOffset: Int, // semitone offset from selected root key
    val isMinor: Boolean,
    val addSeventh: Boolean = false
)

enum class OrbitPattern(
    val displayName: String,
    val subtitle: String,
    val steps: List<OrbitChordStep>
) {
    NEO_RIEMANNIAN_PLR(
        displayName = "PLR Cycle",
        subtitle = "Classic Tonnetz Parallel-Leading-Relative",
        steps = listOf(
            OrbitChordStep(0, isMinor = false),
            OrbitChordStep(0, isMinor = true),
            OrbitChordStep(8, isMinor = false),
            OrbitChordStep(8, isMinor = true),
            OrbitChordStep(4, isMinor = false),
            OrbitChordStep(4, isMinor = true),
            OrbitChordStep(9, isMinor = true),
            OrbitChordStep(7, isMinor = false)
        )
    ),
    CIRCLE_OF_FIFTHS(
        displayName = "Fifths Ring",
        subtitle = "Major torus meridian around fifths",
        steps = listOf(
            OrbitChordStep(0, isMinor = false),
            OrbitChordStep(7, isMinor = false),
            OrbitChordStep(2, isMinor = true),
            OrbitChordStep(9, isMinor = true),
            OrbitChordStep(4, isMinor = true),
            OrbitChordStep(11, isMinor = true),
            OrbitChordStep(5, isMinor = false, addSeventh = true),
            OrbitChordStep(7, isMinor = false)
        )
    ),
    HEXATONIC_POLES(
        displayName = "Major-Third Orbit",
        subtitle = "Minor ring symmetry by major thirds",
        steps = listOf(
            OrbitChordStep(0, isMinor = false, addSeventh = true),
            OrbitChordStep(4, isMinor = false),
            OrbitChordStep(8, isMinor = false, addSeventh = true),
            OrbitChordStep(0, isMinor = true),
            OrbitChordStep(4, isMinor = true),
            OrbitChordStep(8, isMinor = true),
            OrbitChordStep(5, isMinor = false),
            OrbitChordStep(7, isMinor = false)
        )
    ),
    LYDIAN_DRIFT(
        displayName = "Lydian Drift",
        subtitle = "Floating cinematic suspension loop",
        steps = listOf(
            OrbitChordStep(0, isMinor = false, addSeventh = true),
            OrbitChordStep(2, isMinor = false),
            OrbitChordStep(4, isMinor = true),
            OrbitChordStep(7, isMinor = false),
            OrbitChordStep(9, isMinor = true),
            OrbitChordStep(2, isMinor = false),
            OrbitChordStep(11, isMinor = true),
            OrbitChordStep(0, isMinor = false, addSeventh = true)
        )
    ),
    MINOR_SPIRAL(
        displayName = "Minor Spiral",
        subtitle = "Deep nocturnal harmonic descent",
        steps = listOf(
            OrbitChordStep(0, isMinor = true),
            OrbitChordStep(8, isMinor = false, addSeventh = true),
            OrbitChordStep(3, isMinor = false),
            OrbitChordStep(10, isMinor = false),
            OrbitChordStep(5, isMinor = true),
            OrbitChordStep(0, isMinor = true),
            OrbitChordStep(8, isMinor = false),
            OrbitChordStep(7, isMinor = true)
        )
    )
}

data class SynthParams(
    val timbre: SynthTimbre = SynthTimbre.WARM_RHODES,
    val warmth: Float = 0.72f, // 0..1 low-pass filter cutoff
    val echoMix: Float = 0.34f, // 0..0.75 spatial delay wet mix
    val echoFeedback: Float = 0.42f, // 0..0.78 delay tail feedback
    val attackMs: Float = 24f, // 4..400 ms
    val releaseMs: Float = 620f, // 80..2200 ms
    val detuneCents: Float = 10f, // 0..35 cents chorus warmth
    val masterGain: Float = 0.78f, // 0.1..1.0
    val temperament: Temperament = Temperament.EQUAL_12TET,
    val referenceA4Hz: Int = 440
)

object TonnetzMath {
    val NOTE_NAMES = listOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

    // Pure 5-limit Just Intonation ratios relative to root pitch class
    private val JUST_RATIOS = doubleArrayOf(
        1.0,        // Unison
        16.0 / 15.0, // Minor 2nd
        9.0 / 8.0,   // Major 2nd
        6.0 / 5.0,   // Minor 3rd
        5.0 / 4.0,   // Major 3rd
        4.0 / 3.0,   // Perfect 4th
        45.0 / 32.0, // Augmented 4th
        3.0 / 2.0,   // Perfect 5th
        8.0 / 5.0,   // Minor 6th
        5.0 / 3.0,   // Major 6th
        9.0 / 5.0,   // Minor 7th
        15.0 / 8.0   // Major 7th
    )

    fun positiveMod12(value: Int): Int {
        val m = value % 12
        return if (m < 0) m + 12 else m
    }

    /**
     * Standard Tonnetz torus pitch-class formula:
     * u = steps of Perfect Fifth (+7 semitones)
     * v = steps of Major Third (+4 semitones)
     */
    fun pitchClassAt(u: Int, v: Int): Int = positiveMod12(7 * u + 4 * v)

    fun noteName(pitchClass: Int): String = NOTE_NAMES[positiveMod12(pitchClass)]

    fun midiNoteLabel(midi: Int): String {
        val pc = positiveMod12(midi)
        val octave = (midi / 12) - 1
        return "${NOTE_NAMES[pc]}$octave"
    }

    fun midiToFrequency(
        midi: Int,
        temperament: Temperament,
        referenceA4Hz: Int,
        justRootPitchClass: Int = 0
    ): Double {
        if (temperament == Temperament.EQUAL_12TET) {
            return referenceA4Hz * 2.0.pow((midi - 69) / 12.0)
        }
        // Compute pure Just Intonation relative to root pitch class in octave 4
        val rootMidiBase = 60 + positiveMod12(justRootPitchClass)
        val rootFreq = referenceA4Hz * 2.0.pow((rootMidiBase - 69) / 12.0)
        val semitoneDiff = midi - rootMidiBase
        val octaveShift = kotlin.math.floor(semitoneDiff / 12.0).toInt()
        val interval = positiveMod12(semitoneDiff)
        return rootFreq * JUST_RATIOS[interval] * 2.0.pow(octaveShift.toDouble())
    }

    fun buildTriadMidi(
        rootPitchClass: Int,
        octave: Int,
        isMinor: Boolean,
        addSeventh: Boolean = false
    ): List<Int> {
        val baseMidi = ((octave + 1) * 12) + positiveMod12(rootPitchClass)
        val third = if (isMinor) baseMidi + 3 else baseMidi + 4
        val fifth = baseMidi + 7
        return if (addSeventh) {
            val seventh = if (isMinor) baseMidi + 10 else baseMidi + 11
            listOf(baseMidi, third, fifth, seventh)
        } else {
            listOf(baseMidi, third, fifth)
        }
    }

    fun buildSingleMidi(pitchClass: Int, octave: Int): Int {
        return ((octave + 1) * 12) + positiveMod12(pitchClass)
    }

    fun detectChordLabel(midiNotes: Collection<Int>): String {
        if (midiNotes.isEmpty()) return ""
        val sorted = midiNotes.sorted()
        val uniquePcs = sorted.map { positiveMod12(it) }.distinct()
        if (uniquePcs.size == 1) {
            return midiNoteLabel(sorted.first())
        }
        if (uniquePcs.size == 2) {
            val interval = positiveMod12(uniquePcs[1] - uniquePcs[0])
            val intervalName = when (interval) {
                3 -> "m3"
                4 -> "M3"
                5 -> "P4"
                7 -> "P5"
                else -> "Dyad"
            }
            return "${noteName(uniquePcs[0])} + ${noteName(uniquePcs[1])} ($intervalName)"
        }
        val pcSet = uniquePcs.toSet()
        for (root in uniquePcs) {
            val rel = pcSet.map { positiveMod12(it - root) }.toSet()
            when {
                rel == setOf(0, 4, 7) -> return "${noteName(root)} Major"
                rel == setOf(0, 3, 7) -> return "${noteName(root)} Minor"
                rel == setOf(0, 4, 7, 11) -> return "${noteName(root)} Maj7"
                rel == setOf(0, 3, 7, 10) -> return "${noteName(root)} Min7"
                rel == setOf(0, 4, 7, 10) -> return "${noteName(root)}7"
                rel == setOf(0, 4, 8) -> return "${noteName(root)} Aug"
                rel == setOf(0, 3, 6) -> return "${noteName(root)} Dim"
                rel == setOf(0, 5, 7) -> return "${noteName(root)} Sus4"
                rel == setOf(0, 2, 7) -> return "${noteName(root)} Sus2"
            }
        }
        return uniquePcs.take(4).joinToString(" · ") { noteName(it) }
    }

    fun filterCutoffHz(warmth: Float): Int {
        val clamped = warmth.coerceIn(0f, 1f)
        return (260.0 * (52.0.pow(clamped.toDouble()))).roundToInt()
    }
}
