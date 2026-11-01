package vn.homekara.box.player

/**
 * Khử giọng ở giữa hai kênh: mỗi kênh ra = (L − R) / 2.
 * Giọng ca sĩ thường được trộn đều hai loa nên sẽ bị triệt tiêu; nhạc đệm lệch trái/phải còn lại.
 */
object VocalCut {
    /** Xử lý tại chỗ mảng PCM 16-bit xen kẽ L,R,L,R... */
    fun processInterleavedStereo(samples: ShortArray, count: Int = samples.size) {
        var i = 0
        while (i + 1 < count) {
            val diff = (samples[i].toInt() - samples[i + 1].toInt()) / 2
            val v = diff.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            samples[i] = v
            samples[i + 1] = v
            i += 2
        }
    }
}
