package vn.homekara.box.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import vn.homekara.box.App
import vn.homekara.box.BuildConfig
import vn.homekara.box.net.Http
import java.io.File

data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

/**
 * Tự cập nhật: đọc file JSON {"versionCode":2,"versionName":"1.0.1","apkUrl":"https://...apk","notes":"..."}.
 * Khi YouTube thay đổi làm app hỏng, chỉ cần đưa bản APK mới + sửa file JSON là box tự cài.
 */
object UpdateChecker {
    private const val TAG = "HomeKara/Update"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun manifestUrl(context: Context): String =
        App.instance.prefs.updateManifestUrl.ifEmpty { BuildConfig.UPDATE_MANIFEST_URL }

    /** Phân tích JSON; trả null nếu hỏng hoặc không mới hơn. Hàm thuần để dễ kiểm thử. */
    fun parse(json: String, currentVersionCode: Int): UpdateInfo? = try {
        val o = JSONObject(json)
        val info = UpdateInfo(o.getInt("versionCode"), o.optString("versionName"), o.getString("apkUrl"), o.optString("notes"))
        if (info.versionCode > currentVersionCode && info.apkUrl.startsWith("http")) info else null
    } catch (e: Exception) {
        null
    }

    fun maybeCheck(activity: Activity) {
        val prefs = App.instance.prefs
        if (manifestUrl(activity).isEmpty()) return
        if (System.currentTimeMillis() - prefs.lastUpdateCheck < DAY_MS) return
        checkNow(activity, manual = false)
    }

    fun checkNow(activity: Activity, manual: Boolean) {
        val url = manifestUrl(activity)
        val app = App.instance
        if (url.isEmpty()) {
            if (manual) say(activity, "Chưa đặt địa chỉ kiểm tra cập nhật (xem hướng dẫn cài đặt).")
            return
        }
        app.io.execute {
            val info = try { parse(Http.getString(url), BuildConfig.VERSION_CODE) } catch (e: Exception) {
                Log.w(TAG, "Kiểm tra cập nhật lỗi", e)
                if (manual) activity.runOnUiThread { say(activity, "Không kiểm tra được: ${e.message}") }
                return@execute
            }
            app.prefs.lastUpdateCheck = System.currentTimeMillis()
            activity.runOnUiThread {
                if (activity.isFinishing) return@runOnUiThread
                if (info == null) { if (manual) say(activity, "Bạn đang dùng bản mới nhất (${BuildConfig.VERSION_NAME}).") }
                else AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
                    .setTitle("Có bản mới ${info.versionName}")
                    .setMessage(info.notes.ifEmpty { "Cập nhật để app chạy ổn định với YouTube." })
                    .setPositiveButton("Cập nhật") { _, _ -> download(activity, info) }
                    .setNegativeButton("Để sau", null)
                    .show()
            }
        }
    }

    private fun download(activity: Activity, info: UpdateInfo) {
        val dialog = AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK)
            .setTitle("Đang tải bản mới…").setMessage("0 MB").setCancelable(false).show()
        App.instance.io.execute {
            try {
                // Bộ nhớ ngoài để trình cài đặt của Android 4.4 đọc được file.
                val dir = activity.getExternalFilesDir(null) ?: Environment.getExternalStorageDirectory()
                val f = File(dir, "HomeKara-update.apk")
                Http.download(info.apkUrl, f) { bytes ->
                    if (bytes % (512 * 1024) < 64 * 1024) activity.runOnUiThread { dialog.setMessage("%.1f MB".format(bytes / 1048576.0)) }
                }
                f.setReadable(true, false)
                activity.runOnUiThread {
                    dialog.dismiss()
                    val i = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(i)
                }
            } catch (e: Exception) {
                activity.runOnUiThread { dialog.dismiss(); say(activity, "Tải bản mới lỗi: ${e.message}") }
            }
        }
    }

    private fun say(activity: Activity, msg: String) {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_DARK).setMessage(msg).setPositiveButton("OK", null).show()
    }
}
