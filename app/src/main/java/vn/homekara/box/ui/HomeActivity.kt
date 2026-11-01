package vn.homekara.box.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.widget.AbsListView
import android.widget.GridLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import vn.homekara.box.App
import vn.homekara.box.R
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.domain.Song
import vn.homekara.box.player.KaraokeSession
import vn.homekara.box.player.Tone
import vn.homekara.box.queue.AddResult
import vn.homekara.box.queue.QueueItem
import vn.homekara.box.remote.NetworkInfo
import vn.homekara.box.remote.QrCode
import vn.homekara.box.update.UpdateChecker
import vn.homekara.box.youtube.SearchPage

class HomeActivity : TvActivity() {

    private enum class Tab(val label: String) { SEARCH("Tìm kiếm"), FAV("Yêu thích"), RECENT("Hát gần đây"), TOP("Hát nhiều nhất") }

    private val app get() = App.instance
    private lateinit var grid: GridView
    private lateinit var gridAdapter: SongGridAdapter
    private lateinit var queueAdapter: QueueAdapter
    private lateinit var toaster: Toaster
    private lateinit var searchText: TextView
    private lateinit var resultsStatus: TextView
    private lateinit var heading: TextView
    private lateinit var subheading: TextView
    private lateinit var miniSurface: SurfaceView

    private val tabViews = HashMap<Tab, TextView>()
    private val filterViews = HashMap<SearchFilter, TextView>()

    private var tab = Tab.SEARCH
    private var filter = SearchFilter.ALL
    private var text = ""
    private var lastQuery = ""
    private var nextPage: Any? = null
    private var loading = false
    private var loadToken = 0
    private val suggestRunnable = Runnable { loadSuggestions() }

    private val queueListener: (List<QueueItem>) -> Unit = { snap -> runOnUiThread { showQueue(snap) } }
    private val sessionListener = object : KaraokeSession.Listener {
        override fun onSessionChanged() = showNowPlaying()
        override fun onToneChanged(semitones: Int) = showNowPlaying()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        toaster = Toaster(findViewById(R.id.toast))
        searchText = findViewById(R.id.searchText)
        resultsStatus = findViewById(R.id.resultsStatus)
        heading = findViewById(R.id.heading)
        subheading = findViewById(R.id.subheading)

        buildTabs()
        buildFilters()
        buildKeyboard()

        grid = findViewById(R.id.results)
        gridAdapter = SongGridAdapter(layoutInflater)
        grid.adapter = gridAdapter
        grid.setOnItemClickListener { _, _, pos, _ -> openSongMenu(gridAdapter.getItem(pos)) }
        grid.setOnItemLongClickListener { _, _, pos, _ -> openSongMenu(gridAdapter.getItem(pos)); true }
        grid.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(view: AbsListView, first: Int, visible: Int, total: Int) {
                if (tab == Tab.SEARCH && total > 0 && first + visible >= total - 4) loadMore()
            }
        })

        val queueList = findViewById<ListView>(R.id.queueList)
        queueAdapter = QueueAdapter(layoutInflater)
        queueList.adapter = queueAdapter
        queueList.setOnItemClickListener { _, _, pos, _ -> openQueueMenu(queueAdapter.getItem(pos)) }

        findViewById<View>(R.id.shuffleBtn).setOnClickListener {
            app.queue.shuffle(); toaster.show("Đã trộn hàng chờ")
        }
        miniSurface = findViewById(R.id.miniSurface)
        findViewById<View>(R.id.nowPlaying).setOnClickListener {
            when {
                // Đang hát: mở toàn màn hình (chỉ khi người dùng chọn).
                app.session.current != null -> startActivity(Intent(this, PlayerActivity::class.java))
                // Còn bài trong hàng chờ (ví dụ sau khi mất điện): hát tiếp ngay trong khung nhỏ.
                app.queue.size > 0 -> { app.session.playNext(); toaster.show("Đang phát tiếp hàng chờ") }
                else -> toaster.show("Chưa có bài nào — chọn một bài trước nhé")
            }
        }

        updateSearchText()
        selectTab(Tab.SEARCH)
        findViewById<GridLayout>(R.id.keyboard).getChildAt(0)?.requestFocus()
        UpdateChecker.maybeCheck(this)
    }

    override fun onResume() {
        super.onResume()
        app.queue.addListener(queueListener)
        app.session.addListener(sessionListener)
        app.session.player.attach(miniSurface)
        showQueue(app.queue.snapshot())
        showNowPlaying()
        showNetwork()
        if (tab != Tab.SEARCH || lastQuery.isEmpty()) selectTab(tab)
        gridAdapter.favorites = app.store.favoriteIds()
    }

    override fun onPause() {
        super.onPause()
        app.queue.removeListener(queueListener)
        app.session.removeListener(sessionListener)
        app.session.player.exo.clearVideoSurfaceView(miniSurface)
    }

    // ---------- Thanh trên ----------

    private fun buildTabs() {
        val box = findViewById<LinearLayout>(R.id.tabs)
        for (t in Tab.values()) {
            val v = chip(t.label) { selectTab(t) }
            tabViews[t] = v
            box.addView(v)
        }
        box.addView(chip("Cài đặt") { startActivity(Intent(this, SettingsActivity::class.java)) })
    }

    private fun showNetwork() {
        val ip = NetworkInfo.lanIpv4()
        findViewById<TextView>(R.id.networkText).text =
            NetworkInfo.connectionLabel() + (ip?.let { " · $it" } ?: "")
        val url = if (ip != null) "http://$ip:${App.PORT}/" else ""
        findViewById<TextView>(R.id.phoneUrl).text = if (ip != null) "$ip:${App.PORT}" else "Chưa có mạng"
        val qr = findViewById<ImageView>(R.id.qrImage)
        if (url.isNotEmpty() && qr.tag != url) {
            qr.setImageBitmap(QrCode.bitmap(url, dp(96)))
            qr.tag = url
        }
    }

    private fun selectTab(t: Tab) {
        tab = t
        for ((k, v) in tabViews) v.isSelected = k == t
        findViewById<View>(R.id.searchArea).visibility = if (t == Tab.SEARCH) View.VISIBLE else View.GONE
        findViewById<View>(R.id.keyboard).visibility = if (t == Tab.SEARCH) View.VISIBLE else View.GONE
        grid.numColumns = if (t == Tab.SEARCH) 3 else 4
        when (t) {
            Tab.SEARCH -> if (lastQuery.isEmpty()) showLocal(
                "Gợi ý cho bạn", "Hát gần đây trên box này",
                "Gõ tên bài hát bằng bàn phím bên trái rồi bấm “Tìm”.\nKhông cần gõ dấu.") { app.store.history(24) }
            else if (gridAdapter.count == 0 && !loading) runSearch()
            Tab.FAV -> showLocal("Bài yêu thích", "", "Chưa có bài yêu thích. Bấm OK trên một bài › Thêm vào Yêu thích.") { app.store.favorites() }
            Tab.RECENT -> showLocal("Hát gần đây", "Mới nhất trước", "Chưa hát bài nào.") { app.store.history(60) }
            Tab.TOP -> showLocal("Hát nhiều nhất", "Theo số lần hát trên box này", "Chưa hát bài nào.") { app.store.mostPlayed(40) }
        }
    }

    private fun showLocal(title: String, sub: String, emptyText: String, load: () -> List<Song>) {
        val token = ++loadToken
        heading.text = title
        subheading.text = sub
        app.io.execute {
            val songs = try { load() } catch (e: Exception) { emptyList() }
            runOnUiThread {
                if (token != loadToken) return@runOnUiThread
                nextPage = null
                gridAdapter.songs = songs
                setStatus(if (songs.isEmpty()) emptyText else null)
            }
        }
    }

    // ---------- Tìm kiếm ----------

    private fun buildFilters() {
        val box = findViewById<LinearLayout>(R.id.filters)
        box.addView(TextView(this).apply {
            text = "Lọc:"; setTextColor(resources.getColor(R.color.muted)); textSize = 15f
            setPadding(0, 0, dp(8), 0)
        })
        for (f in SearchFilter.values()) {
            val v = chip(f.label) {
                filter = f
                for ((k, cv) in filterViews) cv.isSelected = k == f
                if (lastQuery.isNotEmpty()) runSearch()
            }
            v.isSelected = f == filter
            filterViews[f] = v
            box.addView(v)
        }
    }

    private fun buildKeyboard() {
        val kb = findViewById<GridLayout>(R.id.keyboard)
        val cell = dp(50)
        fun key(label: String, span: Int = 1, primary: Boolean = false, height: Int = 44, action: () -> Unit) {
            val b = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = if (label.length > 1) 15f else 19f
                setTextColor(resources.getColor(if (primary) R.color.on_accent else R.color.text))
                setBackgroundResource(if (primary) R.drawable.bg_button_primary else R.drawable.bg_key)
                isFocusable = true
                isClickable = true
                setOnClickListener { action() }
            }
            val lp = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, span))
            lp.width = cell * span - dp(6)
            lp.height = dp(height)
            lp.setMargins(dp(3), dp(3), dp(3), dp(3))
            kb.addView(b, lp)
        }
        for (c in "abcdefghijklmnopqrstuvwxyz0123456789") key(c.toString()) { type(c.toString()) }
        key("Cách", 2) { type(" ") }
        key("Xoá", 2) { backspace() }
        key("Tìm", 2, primary = true) { submit() }
        key("Xoá hết", 6, height = 36) { text = ""; updateSearchText() }
    }

    private fun type(s: String) {
        if (text.length >= 60) return
        if (s == " " && (text.isEmpty() || text.endsWith(" "))) return
        text += s
        updateSearchText()
    }

    private fun backspace() {
        if (text.isNotEmpty()) { text = text.dropLast(1); updateSearchText() }
    }

    private fun updateSearchText() {
        if (text.isEmpty()) {
            searchText.text = "Gõ tên bài hát (không cần dấu)…"
            searchText.setTextColor(resources.getColor(R.color.muted))
        } else {
            searchText.text = text + "▏"
            searchText.setTextColor(resources.getColor(R.color.text))
        }
        searchText.removeCallbacks(suggestRunnable)
        searchText.postDelayed(suggestRunnable, 450)
    }

    private fun loadSuggestions() {
        val prefix = text.trim()
        val box = findViewById<LinearLayout>(R.id.suggestions)
        if (prefix.isEmpty()) { box.removeAllViews(); return }
        app.io.execute {
            val list = app.suggest(prefix)
            runOnUiThread {
                if (text.trim() != prefix) return@runOnUiThread
                box.removeAllViews()
                if (list.isNotEmpty()) box.addView(TextView(this).apply {
                    text = "Gợi ý:"; setTextColor(resources.getColor(R.color.muted)); textSize = 14f
                    setPadding(0, 0, dp(8), 0)
                })
                for (s in list.take(6)) box.addView(chip(s) { text = s; updateSearchText(); submit() }.apply { textSize = 14f })
            }
        }
    }

    private fun submit() {
        val q = text.trim()
        if (q.isEmpty()) { toaster.show("Hãy gõ tên bài hát trước"); return }
        lastQuery = q
        if (tab != Tab.SEARCH) selectTab(Tab.SEARCH)
        runSearch()
        grid.post { if (gridAdapter.count > 0) grid.requestFocus() }
    }

    private fun runSearch() {
        val q = lastQuery
        val f = filter
        val token = ++loadToken
        loading = true
        nextPage = null
        gridAdapter.songs = emptyList()
        heading.text = "Kết quả cho “$q”"
        subheading.text = "OK để chọn bài"
        setStatus("Đang tìm “$q”…")
        app.io.execute {
            val result: Any = try { app.search.search(q, f) } catch (e: Exception) { e }
            runOnUiThread {
                if (token != loadToken) return@runOnUiThread
                loading = false
                when (result) {
                    is SearchPage -> {
                        nextPage = result.next
                        gridAdapter.songs = result.songs
                        setStatus(if (result.songs.isEmpty()) "Không thấy bài nào. Thử tên khác hoặc bộ lọc “Tất cả”." else null)
                        if (result.songs.isNotEmpty() && currentFocus?.parent === findViewById<View>(R.id.keyboard)) grid.requestFocus()
                    }
                    is Exception -> setStatus("Không tìm được: kiểm tra mạng của box.\n(${result.javaClass.simpleName})")
                }
            }
        }
    }

    private fun loadMore() {
        val cursor = nextPage ?: return
        if (loading) return
        loading = true
        val token = loadToken
        val q = lastQuery
        val f = filter
        app.io.execute {
            val page = try { app.search.search(q, f, cursor) } catch (e: Exception) { null }
            runOnUiThread {
                if (token != loadToken) return@runOnUiThread
                loading = false
                nextPage = page?.next
                if (page != null && page.songs.isNotEmpty()) {
                    val seen = gridAdapter.songs.map { it.videoId }.toHashSet()
                    gridAdapter.songs = gridAdapter.songs + page.songs.filter { seen.add(it.videoId) }
                }
            }
        }
    }

    private fun setStatus(msg: String?) {
        resultsStatus.text = msg ?: ""
        resultsStatus.visibility = if (msg == null) View.GONE else View.VISIBLE
    }

    // ---------- Chọn bài ----------

    private fun openSongMenu(song: Song) {
        val fav = app.store.isFavorite(song.videoId)
        val options = listOf("Thêm vào hàng chờ", "Hát ngay", "Ưu tiên (lên đầu hàng chờ)",
            if (fav) "Bỏ khỏi Yêu thích" else "Thêm vào Yêu thích ★")
        choose(song.title, options) { which ->
            val item = QueueItem(song, "TV")
            when (which) {
                0 -> when (app.queue.add(item)) {
                    AddResult.DUPLICATE -> toaster.show("Bài đã có trong danh sách")
                    AddResult.ADDED -> toaster.show("Đã thêm vào hàng chờ (vị trí ${app.queue.size})")
                }
                1 -> { app.session.playNow(item); toaster.show("Đang phát — chọn khung video để xem toàn màn hình", 3500) }
                2 -> { app.queue.addNext(item); toaster.show("Đã ưu tiên lên đầu hàng chờ") }
                3 -> {
                    app.store.setFavorite(song, !fav)
                    gridAdapter.favorites = app.store.favoriteIds()
                    toaster.show(if (fav) "Đã bỏ khỏi Yêu thích" else "Đã thêm vào Yêu thích")
                    if (tab == Tab.FAV) selectTab(Tab.FAV)
                }
            }
        }
    }

    private fun openQueueMenu(item: QueueItem) {
        choose(item.song.title, listOf("Hát ngay", "Đưa lên", "Đưa xuống", "Xoá khỏi hàng chờ")) { which ->
            when (which) {
                0 -> { app.queue.remove(item.song.videoId); app.session.playNow(item); toaster.show("Đang phát — chọn khung video để xem toàn màn hình", 3500) }
                1 -> app.queue.moveUp(item.song.videoId)
                2 -> app.queue.moveDown(item.song.videoId)
                3 -> app.queue.remove(item.song.videoId)
            }
        }
    }

    private fun showQueue(snap: List<QueueItem>) {
        queueAdapter.items = snap
        findViewById<TextView>(R.id.queueTitle).text = "Hàng chờ · ${snap.size} bài"
        findViewById<View>(R.id.queueEmpty).visibility = if (snap.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showNowPlaying() {
        val np = app.session.nowPlaying()
        val status = app.session.status
        val label = findViewById<TextView>(R.id.nowLabel)
        val title = findViewById<TextView>(R.id.nowTitle)
        val sub = findViewById<TextView>(R.id.nowSub)
        val placeholder = findViewById<TextView>(R.id.miniPlaceholder)
        val hint = findViewById<View>(R.id.miniHint)
        if (np == null) {
            miniSurface.visibility = View.GONE
            placeholder.visibility = View.VISIBLE
            placeholder.text = "♪\nVideo bài hát sẽ phát ở đây"
            hint.visibility = View.GONE
            label.text = "CHƯA HÁT"
            val waiting = app.queue.size
            title.text = if (waiting > 0) "Bấm vào đây để hát $waiting bài trong hàng chờ" else "Chọn một bài để bắt đầu"
            sub.text = ""
        } else {
            miniSurface.visibility = View.VISIBLE
            val preparing = status == KaraokeSession.Status.PREPARING
            placeholder.visibility = if (preparing) View.VISIBLE else View.GONE
            placeholder.text = "Đang tải bài…"
            hint.visibility = if (preparing) View.GONE else View.VISIBLE
            label.text = if (np.paused) "❚❚ TẠM DỪNG" else "● ĐANG HÁT"
            title.text = np.song.title
            sub.text = "Tone ${Tone.label(np.semitones)} · Chọn bởi: ${np.addedBy}"
        }
    }

    // ---------- Phím ----------

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Bàn phím USB: gõ thẳng vào ô tìm.
        if (event.action == KeyEvent.ACTION_DOWN && tab == Tab.SEARCH) {
            val c = event.unicodeChar
            if (c != 0 && !event.isCtrlPressed && (Character.isLetterOrDigit(c) || c == ' '.code)) {
                type(String(Character.toChars(c)).lowercase()); return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_DEL) { backspace(); return true }
            if (event.keyCode == KeyEvent.KEYCODE_SEARCH) { submit(); return true }
        }
        // Đang hát trong khung nhỏ: phím media / CH+ CH- vẫn điều khiển được.
        if (event.action == KeyEvent.ACTION_DOWN && app.session.current != null) {
            val action = when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> PlayerAction.TONE_UP
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> PlayerAction.TONE_DOWN
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> PlayerAction.TOGGLE_PAUSE
                KeyEvent.KEYCODE_MEDIA_NEXT -> PlayerAction.NEXT_SONG
                else -> null
            }
            if (action != null) {
                app.session.perform(action)
                if (action == PlayerAction.TONE_UP || action == PlayerAction.TONE_DOWN)
                    toaster.show("Tone ${Tone.label(app.session.player.semitones)}")
                return true
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MENU && grid.hasFocus()) {
            val pos = grid.selectedItemPosition
            if (pos >= 0) { openSongMenu(gridAdapter.getItem(pos)); return true }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (tab != Tab.SEARCH) { selectTab(Tab.SEARCH); return }
        confirm("Thoát HomeKara?", "Nhạc đang hát (nếu có) sẽ dừng.", "Thoát") {
            app.session.stopAll()
            finish()
        }
    }
}
