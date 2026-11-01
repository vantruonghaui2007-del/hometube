package vn.homekara.box.domain

data class Song(
    val videoId: String,
    val title: String,
    val channel: String,
    val durationSec: Int,
    val thumbnailUrl: String,
) {
    val durationText: String
        get() = if (durationSec <= 0) "" else "%d:%02d".format(durationSec / 60, durationSec % 60)
}

enum class ToneTag(val label: String) { MALE("Tone Nam"), FEMALE("Tone Nữ"), DUET("Song ca"), BEAT("Beat") }

enum class SearchFilter(val label: String, val keyword: String?) {
    ALL("Tất cả", null),
    BEAT("Beat", "beat"),
    MALE("Tone Nam", "tone nam"),
    FEMALE("Tone Nữ", "tone nữ"),
    DUET("Song ca", "song ca");

    companion object {
        fun parse(s: String?): SearchFilter = values().firstOrNull { it.name.equals(s, true) } ?: ALL
    }
}
