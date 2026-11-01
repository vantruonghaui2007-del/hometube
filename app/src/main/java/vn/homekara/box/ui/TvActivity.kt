package vn.homekara.box.ui

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager

/**
 * Lớp nền cho mọi màn hình của HomeKara:
 *  - Toàn màn hình thật: ẩn thanh trạng thái và thanh điều hướng (immersive), kể cả trên máy giả lập.
 *  - Giao diện được thiết kế trên khung 1280×720 dp. Mỗi box báo mật độ điểm ảnh khác nhau
 *    (cùng là TV 1080p nhưng có box báo 960×540 dp, có box 1920×1080 dp), nên ở đây ta tự tính lại
 *    mật độ để khung thiết kế luôn vừa khít màn hình, không bị cắt hay thừa viền.
 */
abstract class TvActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        TvScale.apply(resources)
        TvScale.apply(applicationContext.resources)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        immersive()
    }

    override fun onResume() {
        TvScale.apply(resources)
        super.onResume()
        immersive()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        TvScale.apply(resources)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= 19) flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = flags
    }
}

object TvScale {
    const val DESIGN_W = 1280f
    const val DESIGN_H = 720f

    /** Đặt lại mật độ sao cho màn hình chứa trọn khung 1280×720 dp (giữ tỉ lệ, cạnh dư sẽ giãn ra). */
    fun apply(res: Resources) {
        val dm: DisplayMetrics = res.displayMetrics
        val w = maxOf(dm.widthPixels, dm.heightPixels).toFloat()   // luôn tính theo chiều ngang
        val h = minOf(dm.widthPixels, dm.heightPixels).toFloat()
        if (w <= 0f || h <= 0f) return
        val density = minOf(w / DESIGN_W, h / DESIGN_H)
        val fontScale = res.configuration.fontScale.takeIf { it > 0f } ?: 1f
        dm.density = density
        dm.scaledDensity = density * fontScale
        dm.densityDpi = (density * DisplayMetrics.DENSITY_DEFAULT).toInt()
        val c = res.configuration
        c.densityDpi = dm.densityDpi
        c.screenWidthDp = (w / density).toInt()
        c.screenHeightDp = (h / density).toInt()
    }
}
