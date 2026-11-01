package vn.homekara.box.youtube

import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.domain.Song

data class StreamCandidate(
    val url: String,
    val kind: Kind,
    val codec: String,
    val height: Int,
    val bitrate: Int,
) {
    enum class Kind { VIDEO_ONLY, AUDIO_ONLY, PROGRESSIVE }
}

sealed class PlayableStreams {
    data class Split(val videoUrl: String, val audioUrl: String) : PlayableStreams()
    data class Progressive(val url: String) : PlayableStreams()
}

/** Video không xem được; [reason] là câu tiếng Việt hiển thị cho người dùng. */
class UnplayableException(val reason: String, cause: Throwable? = null) : Exception(reason, cause)

/** Lấy danh sách luồng của một video. Chạy trên luồng nền. */
interface StreamResolver {
    @Throws(Exception::class)
    fun resolve(videoId: String): List<StreamCandidate>
    fun invalidate(videoId: String) {}
}

data class SearchPage(val songs: List<Song>, val next: Any?)

/** Tìm bài. Chạy trên luồng nền. [next] là mã trang do lần tìm trước trả về. */
interface SongSearch {
    @Throws(Exception::class)
    fun search(userText: String, filter: SearchFilter, next: Any? = null): SearchPage
}
