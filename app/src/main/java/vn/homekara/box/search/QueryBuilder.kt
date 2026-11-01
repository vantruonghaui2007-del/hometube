package vn.homekara.box.search

import vn.homekara.box.domain.SearchFilter

object QueryBuilder {
    /** Chuẩn hoá chữ người dùng gõ thành câu tìm trên YouTube: luôn có "karaoke" và từ khoá bộ lọc. */
    fun build(userText: String, filter: SearchFilter): String {
        val base = TextNorm.collapse(userText).lowercase()
        if (base.isEmpty()) return ""
        val parts = StringBuilder(base)
        if (!TextNorm.fold(base).split(' ').contains("karaoke")) parts.append(" karaoke")
        val kw = filter.keyword
        if (kw != null && !TextNorm.fold(parts.toString()).contains(TextNorm.fold(kw))) {
            parts.append(' ').append(kw)
        }
        return parts.toString()
    }
}
