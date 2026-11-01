package vn.homekara.box.data

import android.content.Context
import android.content.SharedPreferences
import vn.homekara.box.ui.KeyStoreLike

/** Cài đặt người dùng. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("homekara", Context.MODE_PRIVATE)

    var maxHeight: Int
        get() = sp.getInt("max_height", 720)
        set(v) = sp.edit().putInt("max_height", v).apply()

    var vocalCutDefault: Boolean
        get() = sp.getBoolean("vocal_cut_default", false)
        set(v) = sp.edit().putBoolean("vocal_cut_default", v).apply()

    var defaultTone: Int
        get() = sp.getInt("default_tone", 0)
        set(v) = sp.edit().putInt("default_tone", v).apply()

    /** Để trống = box tự lấy dữ liệu YouTube. */
    var resolverServer: String
        get() = sp.getString("resolver_server", "") ?: ""
        set(v) = sp.edit().putString("resolver_server", v.trim()).apply()

    var updateManifestUrl: String
        get() = sp.getString("update_url", "") ?: ""
        set(v) = sp.edit().putString("update_url", v.trim()).apply()

    var lastUpdateCheck: Long
        get() = sp.getLong("last_update_check", 0L)
        set(v) = sp.edit().putLong("last_update_check", v).apply()

    val keyStore: KeyStoreLike = object : KeyStoreLike {
        private val keys = context.getSharedPreferences("homekara_keys", Context.MODE_PRIVATE)
        override fun all(): Map<Int, String> =
            keys.all.mapNotNull { (k, v) -> k.toIntOrNull()?.let { code -> (v as? String)?.let { code to it } } }.toMap()
        override fun put(keyCode: Int, action: String) { keys.edit().putString(keyCode.toString(), action).apply() }
        override fun clear() { keys.edit().clear().apply() }
    }
}
