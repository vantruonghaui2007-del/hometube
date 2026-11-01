package vn.homekara.box.player

import kotlin.math.pow

object Tone {
    const val MIN = -6
    const val MAX = 6

    fun clamp(semitones: Int): Int = semitones.coerceIn(MIN, MAX)

    /** Hệ số cao độ cho ExoPlayer: 2^(n/12). */
    fun pitchFactor(semitones: Int): Float = 2.0.pow(semitones / 12.0).toFloat()

    /** "+2", "0", "−3" (dấu trừ Unicode cho dễ đọc trên TV). */
    fun label(semitones: Int): String = when {
        semitones > 0 -> "+$semitones"
        semitones < 0 -> "−${-semitones}"
        else -> "0"
    }
}
