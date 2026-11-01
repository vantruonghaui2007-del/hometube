package vn.homekara.box.remote

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONArray
import org.json.JSONObject
import vn.homekara.box.App
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.domain.Song
import vn.homekara.box.player.KaraokeSession
import vn.homekara.box.queue.AddResult
import vn.homekara.box.queue.QueueItem
import vn.homekara.box.search.ToneTagParser
import vn.homekara.box.ui.PlayerAction
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Trang chọn bài cho điện thoại trong mạng nhà: http://<IP-box>:8686/
 * API JSON: /api/search, /api/suggest, /api/queue, /api/control, /api/state.
 */
class PhoneRemoteServer(private val app: App, port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response = try {
        route(session)
    } catch (e: Exception) {
        json(Response.Status.INTERNAL_ERROR, JSONObject().put("error", e.message ?: "lỗi"))
    }

    private fun route(s: IHTTPSession): Response {
        val path = s.uri.trimEnd('/').ifEmpty { "/" }
        return when {
            path == "/" || path == "/index.html" -> asset("remote/index.html", "text/html; charset=utf-8")
            path == "/api/search" && s.method == Method.GET -> search(s)
            path == "/api/suggest" && s.method == Method.GET -> suggest(s)
            path == "/api/queue" && s.method == Method.GET -> json(Response.Status.OK, queueJson())
            path == "/api/queue" && s.method == Method.POST -> addToQueue(s)
            path.startsWith("/api/queue/") && s.method == Method.DELETE -> {
                app.queue.remove(path.removePrefix("/api/queue/"))
                json(Response.Status.OK, JSONObject().put("result", "removed"))
            }
            path == "/api/control" && s.method == Method.POST -> control(s)
            path == "/api/state" -> json(Response.Status.OK, stateJson())
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain; charset=utf-8", "Không tìm thấy")
        }
    }

    private fun param(s: IHTTPSession, name: String): String? = s.parameters[name]?.firstOrNull()

    private fun search(s: IHTTPSession): Response {
        val q = param(s, "q") ?: ""
        val filter = SearchFilter.parse(param(s, "filter"))
        val page = app.search.search(q, filter)
        val arr = JSONArray()
        for (song in page.songs) arr.put(songJson(song))
        return json(Response.Status.OK, arr)
    }

    private fun suggest(s: IHTTPSession): Response {
        val arr = JSONArray()
        for (x in app.suggest(param(s, "q") ?: "")) arr.put(x)
        return json(Response.Status.OK, arr)
    }

    private fun body(s: IHTTPSession): JSONObject {
        val files = HashMap<String, String>()
        s.parseBody(files)
        return JSONObject(files["postData"] ?: "{}")
    }

    private fun addToQueue(s: IHTTPSession): Response {
        val o = body(s)
        val id = o.optString("videoId")
        if (id.length != 11) return json(Response.Status.BAD_REQUEST, JSONObject().put("error", "videoId"))
        val song = Song(id, o.optString("title"), o.optString("channel"), o.optInt("durationSec"), o.optString("thumbnailUrl"))
        val by = o.optString("by").trim().take(20).ifEmpty { "Điện thoại" }
        val item = QueueItem(song, by)
        val r = if (o.optBoolean("priority")) app.queue.addNext(item) else app.queue.add(item)
        return if (r == AddResult.DUPLICATE)
            json(Response.Status.CONFLICT, JSONObject().put("result", "duplicate"))
        else json(Response.Status.OK, JSONObject().put("result", "added").put("position", positionOf(id)))
    }

    private fun positionOf(id: String) = app.queue.snapshot().indexOfFirst { it.song.videoId == id } + 1

    private fun control(s: IHTTPSession): Response {
        val action = try { PlayerAction.valueOf(body(s).optString("action")) } catch (e: Exception) {
            return json(Response.Status.BAD_REQUEST, JSONObject().put("error", "action"))
        }
        val ok = onMain { app.session.current != null && app.session.perform(action) } ?: false
        return if (ok) json(Response.Status.OK, stateJson())
        else json(Response.Status.CONFLICT, JSONObject().put("error", "not_playing"))
    }

    private fun stateJson(): JSONObject {
        val np: KaraokeSession.NowPlaying? = onMain { app.session.nowPlaying() }
        val o = JSONObject()
        if (np == null) o.put("playing", false)
        else o.put("playing", true).put("song", songJson(np.song)).put("by", np.addedBy)
            .put("semitones", np.semitones).put("paused", np.paused).put("vocalCut", np.vocalCut)
            .put("positionSec", np.positionMs / 1000).put("durationSec", np.durationMs / 1000)
        o.put("queue", queueJson())
        return o
    }

    private fun queueJson(): JSONArray {
        val arr = JSONArray()
        for (q in app.queue.snapshot()) arr.put(songJson(q.song).put("by", q.addedBy))
        return arr
    }

    private fun songJson(s: Song): JSONObject {
        val tags = JSONArray()
        for (t in ToneTagParser.parse(s.title)) tags.put(t.label)
        return JSONObject().put("videoId", s.videoId).put("title", s.title).put("channel", s.channel)
            .put("durationSec", s.durationSec).put("thumbnailUrl", s.thumbnailUrl).put("tags", tags)
    }

    /** Chạy trên main thread (ExoPlayer chỉ được gọi từ đó) và chờ kết quả tối đa 3 giây. */
    private fun <T> onMain(block: () -> T): T? {
        var result: T? = null
        val latch = CountDownLatch(1)
        app.main.post { try { result = block() } finally { latch.countDown() } }
        latch.await(3, TimeUnit.SECONDS)
        return result
    }

    private fun asset(name: String, mime: String): Response {
        val bytes = app.assets.open(name).use { it.readBytes() }
        return newFixedLengthResponse(Response.Status.OK, mime, bytes.inputStream(), bytes.size.toLong())
    }

    private fun json(status: Response.Status, body: Any): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", body.toString()).apply {
            addHeader("Cache-Control", "no-store")
        }
}
