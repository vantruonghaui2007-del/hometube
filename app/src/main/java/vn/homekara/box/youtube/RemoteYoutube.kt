package vn.homekara.box.youtube

import org.json.JSONObject
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.domain.Song
import vn.homekara.box.net.Http
import vn.homekara.box.search.QueryBuilder
import java.net.URLEncoder

/**
 * Phương án dự phòng: máy chủ yt-dlp trong nhà (thư mục resolver-server/).
 * Chỉ dùng khi Cài đặt › Máy chủ phân giải có địa chỉ.
 */
class RemoteYoutube(private val baseUrl: String) : SongSearch, StreamResolver {

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    override fun search(userText: String, filter: SearchFilter, next: Any?): SearchPage {
        val query = (next as? Pair<*, *>)?.first as? String ?: QueryBuilder.build(userText, filter)
        val page = (next as? Pair<*, *>)?.second as? Int ?: 1
        if (query.isEmpty()) return SearchPage(emptyList(), null)
        val json = JSONObject(Http.getString(url("/search?q=" + enc(query) + "&page=" + page)))
        val arr = json.getJSONArray("songs")
        val songs = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Song(o.getString("videoId"), o.optString("title"), o.optString("channel"),
                o.optInt("durationSec"), o.optString("thumbnailUrl"))
        }.filter { it.durationSec == 0 || it.durationSec >= 60 }
        return SearchPage(songs, if (json.optBoolean("hasMore")) Pair(query, page + 1) else null)
    }

    override fun resolve(videoId: String): List<StreamCandidate> {
        val c = Http.open(url("/resolve?v=" + enc(videoId)), readMs = 60_000)
        try {
            val code = c.responseCode
            val body = (if (code >= 400) c.errorStream else c.inputStream)
                ?.use { String(Http.readAll(it, 4 * 1024 * 1024), Charsets.UTF_8) } ?: "{}"
            val json = JSONObject(body)
            if (code == 422) throw UnplayableException(json.optString("reason", "Video không xem được"))
            if (code !in 200..299) throw Http.HttpException(code, "Máy chủ phân giải trả lỗi $code")
            val arr = json.getJSONArray("candidates")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                StreamCandidate(o.getString("url"), StreamCandidate.Kind.valueOf(o.getString("kind")),
                    o.optString("codec"), o.optInt("height"), o.optInt("bitrate"))
            }
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
