package vn.homekara.box.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import vn.homekara.box.R

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** Tạo một "chip" bấm được bằng remote. */
fun Context.chip(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    setBackgroundResource(R.drawable.bg_chip)
    setTextColor(resources.getColorStateList(R.color.chip_text))
    textSize = 16f
    setTypeface(typeface, Typeface.BOLD)
    gravity = Gravity.CENTER
    minHeight = dp(40)
    setPadding(dp(16), 0, dp(16), 0)
    isFocusable = true
    isClickable = true
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply { rightMargin = dp(8) }
}

/** Hộp thoại danh sách lựa chọn, chọn bằng mũi tên + OK. */
fun Activity.choose(title: String, options: List<String>, onPick: (Int) -> Unit) {
    if (isFinishing) return
    AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
        .setTitle(title)
        .setItems(options.toTypedArray()) { d, which -> d.dismiss(); onPick(which) }
        .setNegativeButton("Đóng", null)
        .show()
}

fun Activity.confirm(title: String, message: String, yes: String, onYes: () -> Unit) {
    if (isFinishing) return
    AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(yes) { _, _ -> onYes() }
        .setNegativeButton("Không", null)
        .show()
}

/** Thông báo ngắn kiểu viên thuốc ở đáy màn hình. */
class Toaster(private val view: TextView) {
    private val hide = Runnable { view.visibility = View.GONE }
    fun show(text: String, ms: Long = 2200) {
        view.text = text
        view.visibility = View.VISIBLE
        view.removeCallbacks(hide)
        view.postDelayed(hide, ms)
    }
}
