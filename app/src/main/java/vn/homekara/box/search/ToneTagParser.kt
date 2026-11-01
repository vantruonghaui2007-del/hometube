package vn.homekara.box.search

import vn.homekara.box.domain.ToneTag

/** Đoán nhãn bài từ tiêu đề video (không dấu, không phân biệt hoa thường, khớp nguyên từ). */
object ToneTagParser {
    private val rules = listOf(
        ToneTag.MALE to Regex("\\b(tone|giong) nam\\b"),
        ToneTag.FEMALE to Regex("\\b(tone|giong) nu\\b"),
        ToneTag.DUET to Regex("\\b(song ca|duet)\\b"),
        ToneTag.BEAT to Regex("\\bbeat\\b"),
    )

    fun parse(title: String): Set<ToneTag> {
        val t = TextNorm.fold(title)
        val out = LinkedHashSet<ToneTag>()
        for ((tag, re) in rules) if (re.containsMatchIn(t)) out.add(tag)
        return out
    }
}
