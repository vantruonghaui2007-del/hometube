package vn.homekara.box.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import vn.homekara.box.net.Http
import java.util.concurrent.Executors

/** Tải ảnh nhỏ (≤ 320×180) cho thẻ bài, có bộ nhớ đệm ~12 MB để hợp với box RAM 1 GB. */
object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.rowBytes * value.height
    }
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun load(view: ImageView, url: String) {
        view.tag = url
        val hit = cache.get(url)
        if (hit != null) { view.setImageBitmap(hit); return }
        view.setImageDrawable(null)
        if (url.isBlank()) return
        pool.execute {
            val bmp = try { decode(Http.getBytes(url, 2 * 1024 * 1024)) } catch (e: Exception) { null }
            if (bmp != null) cache.put(url, bmp)
            main.post { if (view.tag == url && bmp != null) view.setImageBitmap(bmp) }
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 320) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
