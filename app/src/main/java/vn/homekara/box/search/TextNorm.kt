package vn.homekara.box.search

import java.text.Normalizer

object TextNorm {
    private val marks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    /** "Đêm Lạnh  TONE NỮ" -> "dem lanh tone nu" */
    fun fold(s: String): String {
        val noD = s.replace('đ', 'd').replace('Đ', 'D')
        val decomposed = Normalizer.normalize(noD, Normalizer.Form.NFD)
        return collapse(marks.replace(decomposed, "").lowercase())
    }

    fun collapse(s: String): String = spaces.replace(s.trim(), " ")
}
