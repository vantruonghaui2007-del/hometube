package vn.homekara.box.player

import android.content.Intent
import vn.homekara.box.App
import vn.homekara.box.domain.Song
import vn.homekara.box.queue.QueueItem
import vn.homekara.box.ui.PlayerAction
import vn.homekara.box.ui.PlayerActivity
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Phiên hát dùng chung cho cả app: giữ trình phát, tự sang bài kế, nhớ tone theo bài.
 * Sống suốt vòng đời app nên đang hát mà về màn hình chính chọn thêm bài thì nhạc vẫn chạy.
 * Mọi hàm gọi trên main thread (điện thoại gọi qua [post]).
 */
class KaraokeSession(private val app: App) {

    enum class Status { IDLE, PREPARING, PLAYING }

    data class NowPlaying(
        val song: Song,
        val addedBy: String,
        val semitones: Int,
        val paused: Boolean,
        val vocalCut: Boolean,
        val positionMs: Long,
        val durationMs: Long,
    )

    interface Listener {
        fun onSessionChanged() {}
        fun onMessage(text: String) {}
        fun onToneChanged(semitones: Int) {}
    }

    val player = KaraokePlayer(app, { app.resolver }, { app.prefs.maxHeight })
    private val listeners = CopyOnWriteArrayList<Listener>()

    var status = Status.IDLE
        private set
    var current: QueueItem? = null
        private set

    private val advance = Runnable { playNext() }

    init {
        player.listener = object : KaraokePlayer.Listener {
            override fun onPreparing(song: Song) { status = Status.PREPARING; changed() }

            override fun onStarted(song: Song) {
                status = Status.PLAYING
                app.dbWriter.execute { runCatching { app.store.recordPlayed(song) } }
                changed()
            }

            override fun onEnded(song: Song) { playNext() }

            override fun onUnplayable(song: Song, reason: String) {
                message("$reason — chuyển sang bài kế…")
                status = Status.PREPARING
                changed()
                app.main.removeCallbacks(advance)
                app.main.postDelayed(advance, 2_500)
            }

            override fun onPauseChanged(paused: Boolean) = changed()
        }
        // Hàng chờ đang trống mà có người thêm bài (TV hoặc điện thoại) -> tự hát.
        // Không tự bật toàn màn hình: bài phát trong khung nhỏ ở màn hình chính,
        // người dùng tự chọn khung đó khi muốn xem toàn màn hình.
        app.queue.addListener { snap ->
            if (snap.isNotEmpty()) post {
                if (status == Status.IDLE && current == null) playNext()
            }
            post { changed() }
        }
    }

    fun addListener(l: Listener) { listeners.add(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }

    fun post(r: () -> Unit) { app.main.post(r) }

    /** Lấy bài đầu hàng chờ và hát; hết bài thì về trạng thái chờ. */
    fun playNext() {
        app.main.removeCallbacks(advance)
        val next = app.queue.popNext()
        if (next == null) {
            player.stop()
            current = null
            status = Status.IDLE
            changed()
            return
        }
        current = next
        val tone = app.store.savedTone(next.song.videoId) ?: app.prefs.defaultTone
        player.vocalCut.enabled = app.prefs.vocalCutDefault
        player.play(next.song, tone)
        listeners.forEach { it.onToneChanged(player.semitones) }
        changed()
    }

    /** Hát ngay một bài (chen lên đầu hàng chờ rồi chuyển bài). */
    fun playNow(item: QueueItem) {
        app.queue.addNext(item)
        playNext()
    }

    fun stopAll() {
        app.main.removeCallbacks(advance)
        player.stop()
        current = null
        status = Status.IDLE
        changed()
    }

    fun perform(action: PlayerAction): Boolean {
        val song = current?.song
        when (action) {
            PlayerAction.TONE_UP -> setTone(player.semitones + 1)
            PlayerAction.TONE_DOWN -> setTone(player.semitones - 1)
            PlayerAction.TONE_RESET -> setTone(0)
            PlayerAction.TOGGLE_PAUSE -> { if (song == null) return false; player.togglePause(); changed() }
            PlayerAction.NEXT_SONG -> playNext()
            PlayerAction.TOGGLE_VOCAL_CUT -> {
                player.vocalCut.enabled = !player.vocalCut.enabled
                message(if (player.vocalCut.enabled) "Đã tắt lời ca sĩ" else "Đã bật lại lời ca sĩ")
                changed()
            }
            PlayerAction.SEEK_FWD -> player.seekBy(10_000)
            PlayerAction.SEEK_BACK -> player.seekBy(-10_000)
            PlayerAction.SHOW_QUEUE, PlayerAction.SHOW_HUD -> return false
        }
        return true
    }

    private fun setTone(value: Int) {
        player.semitones = value
        val v = player.semitones
        current?.song?.let { s -> app.dbWriter.execute { runCatching { app.store.saveTone(s.videoId, v) } } }
        listeners.forEach { it.onToneChanged(v) }
        changed()
    }

    fun nowPlaying(): NowPlaying? {
        val c = current ?: return null
        return NowPlaying(c.song, c.addedBy, player.semitones, player.isPaused, player.vocalCut.enabled,
            player.exo.currentPosition, player.exo.duration.coerceAtLeast(0))
    }

    fun openPlayerScreen() {
        app.startActivity(Intent(app, PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun message(text: String) = listeners.forEach { it.onMessage(text) }
    private fun changed() = listeners.forEach { it.onSessionChanged() }
}
