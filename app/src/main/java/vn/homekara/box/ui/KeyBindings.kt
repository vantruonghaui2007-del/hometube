package vn.homekara.box.ui

import android.view.KeyEvent

enum class PlayerAction(val label: String) {
    TOGGLE_PAUSE("Tạm dừng / tiếp tục"),
    TONE_UP("Tăng tone"),
    TONE_DOWN("Giảm tone"),
    TONE_RESET("Về tone gốc"),
    NEXT_SONG("Bài kế"),
    SHOW_QUEUE("Mở hàng chờ"),
    TOGGLE_VOCAL_CUT("Tắt lời ca sĩ"),
    SEEK_FWD("Tua tới 10 giây"),
    SEEK_BACK("Tua lui 10 giây"),
    SHOW_HUD("Hiện thanh điều khiển"),
}

/** Nơi lưu phím đã gán lại (SharedPreferences trên máy thật, Map trong test). */
interface KeyStoreLike {
    fun all(): Map<Int, String>
    fun put(keyCode: Int, action: String)
    fun clear()
}

/**
 * Gán phím remote -> hành động trên màn hình hát.
 * Khi thanh điều khiển (HUD) đang hiện, các phím mũi tên để dành cho việc di chuyển giữa các nút.
 */
class KeyBindings(private val store: KeyStoreLike) {

    fun actionFor(keyCode: Int, hudVisible: Boolean): PlayerAction? {
        if (hudVisible && keyCode in DPAD_KEYS) return null
        store.all()[keyCode]?.let { name -> PlayerAction.values().firstOrNull { it.name == name }?.let { return it } }
        return DEFAULTS[keyCode]
    }

    fun bind(keyCode: Int, action: PlayerAction) = store.put(keyCode, action.name)

    fun reset() = store.clear()

    /** Danh sách phím đang dùng cho từng hành động (để hiển thị). */
    fun describe(): Map<PlayerAction, List<Int>> {
        val merged = HashMap<Int, PlayerAction>(DEFAULTS)
        for ((k, v) in store.all()) PlayerAction.values().firstOrNull { it.name == v }?.let { merged[k] = it }
        return PlayerAction.values().associateWith { a -> merged.filterValues { it == a }.keys.sorted() }
    }

    companion object {
        val DPAD_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        )

        val DEFAULTS: Map<Int, PlayerAction> = mapOf(
            KeyEvent.KEYCODE_DPAD_UP to PlayerAction.TONE_UP,
            KeyEvent.KEYCODE_CHANNEL_UP to PlayerAction.TONE_UP,
            KeyEvent.KEYCODE_PAGE_UP to PlayerAction.TONE_UP,
            KeyEvent.KEYCODE_DPAD_DOWN to PlayerAction.TONE_DOWN,
            KeyEvent.KEYCODE_CHANNEL_DOWN to PlayerAction.TONE_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN to PlayerAction.TONE_DOWN,
            // Khi thanh điều khiển đang ẩn: OK / ◄ / ► chỉ để gọi thanh điều khiển ra.
            KeyEvent.KEYCODE_DPAD_CENTER to PlayerAction.SHOW_HUD,
            KeyEvent.KEYCODE_ENTER to PlayerAction.SHOW_HUD,
            KeyEvent.KEYCODE_DPAD_LEFT to PlayerAction.SHOW_HUD,
            KeyEvent.KEYCODE_DPAD_RIGHT to PlayerAction.SHOW_HUD,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to PlayerAction.TOGGLE_PAUSE,
            KeyEvent.KEYCODE_MEDIA_NEXT to PlayerAction.NEXT_SONG,
            KeyEvent.KEYCODE_MENU to PlayerAction.SHOW_QUEUE,
            KeyEvent.KEYCODE_0 to PlayerAction.TONE_RESET,
            KeyEvent.KEYCODE_5 to PlayerAction.TOGGLE_VOCAL_CUT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to PlayerAction.SEEK_FWD,
            KeyEvent.KEYCODE_MEDIA_REWIND to PlayerAction.SEEK_BACK,
        )
    }
}
