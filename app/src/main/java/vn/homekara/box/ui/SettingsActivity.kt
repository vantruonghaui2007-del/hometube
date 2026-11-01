package vn.homekara.box.ui

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import vn.homekara.box.App
import vn.homekara.box.BuildConfig
import vn.homekara.box.R
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.net.Http
import vn.homekara.box.player.StreamSelector
import vn.homekara.box.player.Tone
import vn.homekara.box.remote.NetworkInfo
import vn.homekara.box.update.UpdateChecker

class SettingsActivity : TvActivity() {

    private val app get() = App.instance
    private lateinit var list: LinearLayout
    private lateinit var detail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(resources.getColor(R.color.bg))
            setPadding(dp(36), dp(28), dp(36), dp(28))
        }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(TextView(this).apply {
            text = "Cài đặt"; textSize = 30f; setTypeface(typeface, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text)); setPadding(0, 0, 0, dp(16))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(dp(460), 0, 1f))
        root.addView(left, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))

        detail = TextView(this).apply {
            setBackgroundResource(R.drawable.bg_panel)
            setPadding(dp(28), dp(24), dp(28), dp(24))
            setTextColor(resources.getColor(R.color.text))
            textSize = 17f
            setLineSpacing(0f, 1.25f)
            gravity = Gravity.TOP
        }
        root.addView(ScrollView(this).apply { addView(detail) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dp(28) })
        setContentView(root)
        build()
        list.getChildAt(0)?.requestFocus()
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) build() }

    private fun build() {
        val focusedIndex = (0 until list.childCount).firstOrNull { list.getChildAt(it).hasFocus() }
        list.removeAllViews()
        val p = app.prefs
        row("Chất lượng video tối đa", "${p.maxHeight}p",
            "720p là mức mượt nhất cho SmartBox 2. Chọn 1080p nếu dùng mạng dây và video không giật; 480p khi Wi‑Fi yếu.\n\nApp chỉ dùng video H.264 vì chip S805 giải mã phần cứng tốt nhất định dạng này.") {
            val opts = listOf(480, 720, 1080)
            choose("Chất lượng tối đa", opts.map { "${it}p" }) { p.maxHeight = opts[it]; build() }
        }
        row("Tắt lời ca sĩ khi bắt đầu bài", if (p.vocalCutDefault) "Bật" else "Tắt",
            "Giảm giọng hát gốc nằm giữa hai loa. Hợp với video còn giọng ca sĩ; với bản beat thì không cần.\nKhi đang hát bấm phím 5 để bật/tắt.") {
            p.vocalCutDefault = !p.vocalCutDefault; build()
        }
        row("Tone mặc định cho bài mới", Tone.label(p.defaultTone),
            "Áp cho bài chưa từng hát. Bài đã hát sẽ dùng tone bạn chỉnh lần trước.") {
            val opts = (Tone.MIN..Tone.MAX).toList()
            choose("Tone mặc định", opts.map { Tone.label(it) }) { p.defaultTone = opts[it]; build() }
        }
        row("Gán phím remote", "", describeKeys()) {
            startActivity(Intent(this, KeyLearnActivity::class.java))
        }
        row("Kiểm tra hệ thống", "Chạy thử",
            "Thử kết nối YouTube, tìm một bài và lấy link video ngay trên box này. Dùng khi app không tìm được bài hay không phát được.") {
            runDiagnostics()
        }
        row("Máy chủ phân giải (nâng cao)", p.resolverServer.ifEmpty { "Không dùng" },
            "Để trống: box tự lấy dữ liệu YouTube.\nChỉ điền khi dùng máy chủ yt-dlp trong nhà (phương án dự phòng, xem hướng dẫn), ví dụ http://192.168.1.10:8000") {
            askText("Địa chỉ máy chủ phân giải", p.resolverServer, "http://192.168.1.10:8000") { p.resolverServer = it; build() }
        }
        row("Địa chỉ kiểm tra cập nhật", UpdateChecker.manifestUrl(this).ifEmpty { "Chưa đặt" },
            "Link tới file update.json (xem hướng dẫn). Box tự kiểm tra mỗi ngày một lần khi mở app.") {
            askText("Link file update.json", p.updateManifestUrl, "https://.../update.json") { p.updateManifestUrl = it; build() }
        }
        row("Kiểm tra cập nhật ngay", "Phiên bản ${BuildConfig.VERSION_NAME}", "Tải và cài bản mới nếu có.") {
            UpdateChecker.checkNow(this, manual = true)
        }
        row("Thông tin thiết bị", "", deviceInfo()) { detail.text = deviceInfo() }
        focusedIndex?.let { list.getChildAt(it)?.requestFocus() }
    }

    private fun row(title: String, value: String, help: String, onClick: () -> Unit) {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_now_playing)
            setPadding(dp(18), 0, dp(18), 0)
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
            setOnFocusChangeListener { _, has -> if (has) detail.text = "$title\n\n$help" }
        }
        v.addView(TextView(this).apply {
            text = title; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setTextColor(resources.getColor(R.color.text))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(TextView(this).apply { text = value; textSize = 15f; setTextColor(resources.getColor(R.color.muted)) })
        list.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(62)).apply { bottomMargin = dp(8) })
    }

    private fun describeKeys(): String {
        val names = mapOf(19 to "▲", 20 to "▼", 21 to "◄", 22 to "►", 23 to "OK", 66 to "Enter", 82 to "Menu",
            85 to "Play/Pause", 87 to "Next", 89 to "Tua lui", 90 to "Tua tới", 92 to "Page▲", 93 to "Page▼",
            166 to "CH+", 167 to "CH−", 7 to "0", 12 to "5")
        val map = KeyBindings(app.prefs.keyStore).describe()
        return "Phím đang dùng trên màn hình hát:\n\n" + map.entries.joinToString("\n") { (a, keys) ->
            "• ${a.label}: " + keys.joinToString(", ") { names[it] ?: "mã $it" }.ifEmpty { "—" }
        } + "\n\nBấm OK để gán lại theo remote của bạn."
    }

    private fun deviceInfo(): String {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return "Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Bản dựng: ${Build.DISPLAY}\n" +
            "Chip: ${Build.HARDWARE} · ${Runtime.getRuntime().availableProcessors()} nhân\n" +
            "RAM: ${mi.totalMem / (1024 * 1024)} MB (trống ${mi.availMem / (1024 * 1024)} MB)\n" +
            "Mạng: ${NetworkInfo.connectionLabel()} · ${NetworkInfo.lanIpv4() ?: "chưa có IP"}\n" +
            "Cổng chọn bài điện thoại: ${App.PORT}\n" +
            "HomeKara ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun askText(title: String, current: String, hint: String, onOk: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(current); this.hint = hint; setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
            .setTitle(title).setView(input)
            .setPositiveButton("Lưu") { _, _ -> onOk(input.text.toString()) }
            .setNeutralButton("Xoá trống") { _, _ -> onOk("") }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun runDiagnostics() {
        val log = StringBuilder("Kiểm tra hệ thống\n\n")
        fun line(s: String) = runOnUiThread { log.append(s).append('\n'); detail.text = log }
        line("Đang chạy… (mất khoảng 10–30 giây)")
        app.io.execute {
            val t0 = System.currentTimeMillis()
            try {
                val c = Http.open("https://www.youtube.com/")
                val code = c.responseCode; c.disconnect()
                line("✔ Kết nối HTTPS tới YouTube: HTTP $code")
            } catch (e: Exception) { line("✘ Không kết nối được YouTube: ${e.javaClass.simpleName}: ${e.message}") }
            var firstId: String? = null
            try {
                val t = System.currentTimeMillis()
                val page = app.search.search("dem lanh", SearchFilter.ALL)
                firstId = page.songs.firstOrNull()?.videoId
                line("✔ Tìm kiếm: ${page.songs.size} bài (${System.currentTimeMillis() - t} ms)")
            } catch (e: Exception) { line("✘ Tìm kiếm lỗi: ${e.javaClass.simpleName}: ${e.message}") }
            if (firstId != null) try {
                val t = System.currentTimeMillis()
                val cands = app.resolver.resolve(firstId)
                val sel = StreamSelector.select(cands, app.prefs.maxHeight)
                line("✔ Lấy link video: ${cands.size} luồng (${System.currentTimeMillis() - t} ms)")
                line(if (sel != null) "✔ Có luồng H.264 phù hợp: ${sel.javaClass.simpleName}" else "✘ Không có luồng H.264 phù hợp")
            } catch (e: Exception) { line("✘ Lấy link video lỗi: ${e.javaClass.simpleName}: ${e.message}") }
            line("\nXong sau ${(System.currentTimeMillis() - t0) / 1000} giây.")
            line("Nếu dòng “Tìm kiếm” hoặc “Lấy link video” báo ✘: thử cập nhật app, hoặc dùng Máy chủ phân giải.")
        }
    }
}

/** Màn hình gán phím: chọn hành động rồi bấm nút trên remote. */
class KeyLearnActivity : TvActivity() {
    private var waitingFor: PlayerAction? = null
    private lateinit var status: TextView
    private lateinit var bindings: KeyBindings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = KeyBindings(App.instance.prefs.keyStore)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bg))
            setPadding(dp(36), dp(28), dp(36), dp(28))
        }
        root.addView(TextView(this).apply {
            text = "Gán phím remote"; textSize = 30f; setTypeface(typeface, Typeface.BOLD); setTextColor(resources.getColor(R.color.text))
        })
        status = TextView(this).apply {
            text = "Chọn một hành động, rồi bấm nút bất kỳ trên remote để gán."
            textSize = 17f; setTextColor(resources.getColor(R.color.muted)); setPadding(0, dp(8), 0, dp(16))
        }
        root.addView(status)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (a in PlayerAction.values()) {
            grid.addView(TextView(this).apply {
                text = a.label; textSize = 18f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(resources.getColor(R.color.text))
                setBackgroundResource(R.drawable.bg_now_playing)
                setPadding(dp(18), dp(12), dp(18), dp(12))
                isFocusable = true; isClickable = true
                setOnClickListener {
                    waitingFor = a
                    status.text = "Bấm nút trên remote cho: “${a.label}” (Back để huỷ)"
                    status.setTextColor(resources.getColor(R.color.accent))
                }
            }, LinearLayout.LayoutParams(dp(520), ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
        }
        grid.addView(TextView(this).apply {
            text = "Khôi phục mặc định"; textSize = 16f; setTextColor(resources.getColor(R.color.muted))
            setBackgroundResource(R.drawable.bg_now_playing); setPadding(dp(18), dp(12), dp(18), dp(12))
            isFocusable = true; isClickable = true
            setOnClickListener { bindings.reset(); status.text = "Đã khôi phục phím mặc định." }
        })
        root.addView(ScrollView(this).apply { addView(grid) })
        setContentView(root)
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val a = waitingFor
        if (a != null && event.action == android.view.KeyEvent.ACTION_DOWN) {
            if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                waitingFor = null; status.text = "Đã huỷ."; status.setTextColor(resources.getColor(R.color.muted)); return true
            }
            bindings.bind(event.keyCode, a)
            waitingFor = null
            status.text = "Đã gán nút (mã ${event.keyCode}) cho “${a.label}”."
            status.setTextColor(resources.getColor(R.color.ok))
            return true
        }
        if (a != null) return true
        return super.dispatchKeyEvent(event)
    }
}
