package vn.homekara.box.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import vn.homekara.box.App
import vn.homekara.box.R
import vn.homekara.box.player.KaraokeSession
import vn.homekara.box.player.Tone
import vn.homekara.box.queue.QueueItem
import vn.homekara.box.remote.NetworkInfo
import vn.homekara.box.remote.QrCode

class PlayerActivity : TvActivity() {

    private val app get() = App.instance
    private val session get() = app.session
    private lateinit var bindings: KeyBindings

    private lateinit var surface: SurfaceView
    private lateinit var hud: View
    private lateinit var idle: View
    private lateinit var loading: TextView
    private lateinit var toneBig: TextView
    private lateinit var centerNotice: TextView
    private lateinit var errorBanner: TextView
    private lateinit var upNext: View

    private var skipArmedAt = 0L

    private lateinit var hint: TextView
    private val hideHint = Runnable { hint.visibility = View.GONE }
    private val hideHud = Runnable { if (!session.player.isPaused && session.current != null) hideHudNow() }
    private val hideToneBig = Runnable { toneBig.visibility = View.GONE }
    private val hideCenter = Runnable { refreshCenter() }
    private val hideError = Runnable { errorBanner.visibility = View.GONE }
    private val ticker = object : Runnable {
        override fun run() { updateProgress(); surface.postDelayed(this, 500) }
    }

    private val listener = object : KaraokeSession.Listener {
        override fun onSessionChanged() = render()
        override fun onToneChanged(semitones: Int) = showTone(semitones, big = true)
        override fun onMessage(text: String) {
            errorBanner.text = text
            errorBanner.visibility = View.VISIBLE
            errorBanner.removeCallbacks(hideError)
            errorBanner.postDelayed(hideError, 3500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        bindings = KeyBindings(app.prefs.keyStore)
        surface = findViewById(R.id.surface)
        hud = findViewById(R.id.hud)
        idle = findViewById(R.id.idle)
        loading = findViewById(R.id.loading)
        toneBig = findViewById(R.id.toneBig)
        centerNotice = findViewById(R.id.centerNotice)
        errorBanner = findViewById(R.id.errorBanner)
        upNext = findViewById(R.id.upNext)
        hint = findViewById(R.id.hint)

        // Chạm / nhấp chuột vào màn hình (máy giả lập, điện thoại): bật/tắt thanh điều khiển.
        surface.setOnClickListener { if (hud.visibility == View.VISIBLE) hideHudNow() else showHud(focus = R.id.btnPause) }
        hud.isClickable = true

        click(R.id.btnToneDown) { session.perform(PlayerAction.TONE_DOWN) }
        click(R.id.btnToneUp) { session.perform(PlayerAction.TONE_UP) }
        click(R.id.btnToneReset) { session.perform(PlayerAction.TONE_RESET) }
        click(R.id.btnPause) { session.perform(PlayerAction.TOGGLE_PAUSE) }
        click(R.id.btnNext) { requestSkip() }
        click(R.id.btnVocal) { session.perform(PlayerAction.TOGGLE_VOCAL_CUT) }
        click(R.id.btnMore) { goHome() }
        click(R.id.btnIdleHome) { goHome() }
    }

    private fun click(id: Int, f: () -> Unit) = findViewById<View>(id).setOnClickListener { f(); pokeHud() }

    override fun onResume() {
        super.onResume()
        session.addListener(listener)
        session.player.attach(surface)
        if (session.current == null && app.queue.size > 0) session.playNext()
        render()
        showTone(session.player.semitones, big = false)
        showHud(focus = R.id.btnPause)
        surface.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        session.removeListener(listener)
        surface.removeCallbacks(ticker)
        session.player.exo.clearVideoSurfaceView(surface)
    }

    override fun onDestroy() {
        super.onDestroy()
        session.player.exo.clearVideoSurfaceView(surface)
    }

    // ---------- Hiển thị ----------

    private fun render() {
        val np = session.nowPlaying()
        val isIdle = np == null && session.status == KaraokeSession.Status.IDLE
        idle.visibility = if (isIdle) View.VISIBLE else View.GONE
        if (isIdle) {
            hud.visibility = View.GONE
            upNext.visibility = View.GONE
            loading.visibility = View.GONE
            centerNotice.visibility = View.GONE
            showIdleQr()
            findViewById<View>(R.id.btnIdleHome).requestFocus()
            return
        }
        val preparing = session.status == KaraokeSession.Status.PREPARING
        loading.visibility = if (preparing && np != null) View.VISIBLE else View.GONE
        if (np != null) {
            loading.text = "Đang tải: ${np.song.title}"
            findViewById<TextView>(R.id.title).text = np.song.title
            findViewById<TextView>(R.id.by).text = "Chọn bởi: ${np.addedBy} · Tone chỉnh cho bài này sẽ được nhớ cho lần sau"
            findViewById<TextView>(R.id.btnPause).text = if (np.paused) "▶ Tiếp tục" else "❚❚ Tạm dừng"
            val vocal = findViewById<TextView>(R.id.vocalChip)
            vocal.text = "Tắt lời: " + if (np.vocalCut) "Bật" else "Tắt"
            vocal.setBackgroundColor(resources.getColor(if (np.vocalCut) R.color.ok else R.color.surface3))
            vocal.setTextColor(resources.getColor(if (np.vocalCut) R.color.on_ok else R.color.text))
            showTone(np.semitones, big = false)
            if (np.paused) showHud(focus = null)
        }
        findViewById<TextView>(R.id.left).text = "Còn ${app.queue.size} bài trong hàng chờ"
        refreshCenter()
    }

    private fun refreshCenter() {
        val armed = System.currentTimeMillis() - skipArmedAt < 3000
        when {
            armed -> {
                centerNotice.text = "Bỏ qua bài này?\nBấm ► (Bài kế) lần nữa để xác nhận"
                centerNotice.visibility = View.VISIBLE
            }
            session.current != null && session.player.isPaused -> {
                centerNotice.text = "❚❚  Tạm dừng"
                centerNotice.visibility = View.VISIBLE
            }
            else -> centerNotice.visibility = View.GONE
        }
    }

    private fun showTone(semitones: Int, big: Boolean) {
        val label = "Tone " + Tone.label(semitones)
        val (bg, fg) = when {
            semitones > 0 -> R.color.accent to R.color.on_accent
            semitones < 0 -> R.color.tone_neg to R.color.on_tone_neg
            else -> R.color.surface3 to R.color.text
        }
        for (id in intArrayOf(R.id.toneChip, R.id.toneBig)) {
            val v = findViewById<TextView>(id)
            v.text = label
            v.setBackgroundColor(resources.getColor(bg))
            v.setTextColor(resources.getColor(fg))
        }
        findViewById<TextView>(R.id.toneValue).text = Tone.label(semitones)
        if (big) {
            toneBig.visibility = View.VISIBLE
            toneBig.removeCallbacks(hideToneBig)
            toneBig.postDelayed(hideToneBig, 2000)
        }
    }

    private fun updateProgress() {
        val np = session.nowPlaying() ?: run { upNext.visibility = View.GONE; return }
        val dur = np.durationMs
        val pos = np.positionMs.coerceAtLeast(0)
        findViewById<TextView>(R.id.pos).text = fmt(pos)
        findViewById<TextView>(R.id.dur).text = if (dur > 0) fmt(dur) else "--:--"
        findViewById<ProgressBar>(R.id.progress).progress = if (dur > 0) (pos * 1000 / dur).toInt() else 0
        val next = app.queue.peek()
        val nearEnd = dur > 0 && dur - pos in 1L..15_000L
        if (nearEnd && next != null) {
            findViewById<TextView>(R.id.upNextTitle).text = next.song.title
            findViewById<TextView>(R.id.upNextBy).text = "Chọn bởi: ${next.addedBy} · còn ${(dur - pos) / 1000} giây"
            upNext.visibility = View.VISIBLE
        } else upNext.visibility = View.GONE
    }

    private fun showIdleQr() {
        val ip = NetworkInfo.lanIpv4() ?: run { findViewById<TextView>(R.id.idleUrl).text = "Box chưa có mạng"; return }
        val url = "http://$ip:${App.PORT}/"
        val qr = findViewById<ImageView>(R.id.idleQr)
        if (qr.tag != url) { qr.setImageBitmap(QrCode.bitmap(url, dp(204))); qr.tag = url }
        findViewById<TextView>(R.id.idleUrl).text = "$ip:${App.PORT}"
    }

    private fun showHud(focus: Int?) {
        if (idle.visibility == View.VISIBLE) return
        hud.visibility = View.VISIBLE
        if (focus != null && currentFocus?.let { isInHud(it) } != true) findViewById<View>(focus).requestFocus()
        pokeHud()
    }

    private fun pokeHud() {
        hud.removeCallbacks(hideHud)
        hud.postDelayed(hideHud, 8000)
        hint.visibility = View.GONE
    }

    private fun hideHudNow() {
        hud.removeCallbacks(hideHud)
        hud.visibility = View.GONE
        // Nhắc cách gọi lại thanh điều khiển
        hint.visibility = View.VISIBLE
        hint.removeCallbacks(hideHint)
        hint.postDelayed(hideHint, 4000)
    }

    private fun isInHud(v: View): Boolean {
        var p: Any? = v
        while (p is View) { if (p === hud) return true; p = p.parent }
        return false
    }

    private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

    // ---------- Hành động ----------

    private fun requestSkip() {
        if (session.current == null) return
        val now = System.currentTimeMillis()
        if (now - skipArmedAt < 3000) {
            skipArmedAt = 0
            session.perform(PlayerAction.NEXT_SONG)
        } else {
            skipArmedAt = now
            centerNotice.removeCallbacks(hideCenter)
            centerNotice.postDelayed(hideCenter, 3000)
        }
        refreshCenter()
    }

    private fun showQueueDialog() {
        val items = app.queue.snapshot()
        if (items.isEmpty()) { listener.onMessage("Hàng chờ đang trống — bấm “Chọn thêm bài”"); return }
        choose("Hàng chờ (${items.size} bài)", items.mapIndexed { i, q -> "${i + 1}. ${q.song.title} — ${q.addedBy}" }) { which ->
            val item: QueueItem = items[which]
            choose(item.song.title, listOf("Hát ngay", "Đưa lên", "Xoá khỏi hàng chờ")) { w ->
                when (w) {
                    0 -> { app.queue.remove(item.song.videoId); session.playNow(item) }
                    1 -> app.queue.moveUp(item.song.videoId)
                    2 -> app.queue.remove(item.song.videoId)
                }
                render()
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        finish()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (idle.visibility == View.VISIBLE || event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        val hudVisible = hud.visibility == View.VISIBLE
        val action = bindings.actionFor(event.keyCode, hudVisible)
        if (action == null) {
            if (hudVisible && event.action == KeyEvent.ACTION_DOWN) pokeHud()
            return super.dispatchKeyEvent(event)
        }
        if (event.action != KeyEvent.ACTION_DOWN) return true
        when (action) {
            PlayerAction.SHOW_HUD -> showHud(focus = R.id.btnPause)
            PlayerAction.SHOW_QUEUE -> showQueueDialog()
            PlayerAction.NEXT_SONG -> requestSkip()
            PlayerAction.TONE_UP, PlayerAction.TONE_DOWN -> {
                // giữ phím sẽ lặp lại; chỉ nhận lần bấm đầu để tránh nhảy nhiều tone
                if (event.repeatCount == 0) session.perform(action)
            }
            else -> session.perform(action)
        }
        return true
    }

    override fun onBackPressed() {
        if (hud.visibility == View.VISIBLE && session.current != null && !session.player.isPaused) {
            hideHudNow()
            return
        }
        goHome()
    }
}
