package vn.homekara.box.net

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Gọi HTTP đơn giản bằng HttpURLConnection (dùng TLS đã cài trong [Tls]). */
object Http {
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    class HttpException(val code: Int, message: String) : IOException(message)

    fun open(url: String, connectMs: Int = 10_000, readMs: Int = 20_000): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs
        c.readTimeout = readMs
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", DESKTOP_UA)
        return c
    }

    fun getBytes(url: String, maxBytes: Int = 8 * 1024 * 1024): ByteArray {
        val c = open(url)
        try {
            val code = c.responseCode
            if (code !in 200..299) throw HttpException(code, "HTTP $code cho $url")
            return c.inputStream.use { readAll(it, maxBytes) }
        } finally {
            c.disconnect()
        }
    }

    fun getString(url: String): String = String(getBytes(url), Charsets.UTF_8)

    fun download(url: String, dest: File, onProgress: (Long) -> Unit = {}) {
        val c = open(url, readMs = 60_000)
        try {
            val code = c.responseCode
            if (code !in 200..299) throw HttpException(code, "HTTP $code cho $url")
            dest.parentFile?.mkdirs()
            c.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val r = input.read(buf)
                        if (r < 0) break
                        out.write(buf, 0, r)
                        total += r
                        onProgress(total)
                    }
                }
            }
        } finally {
            c.disconnect()
        }
    }

    fun readAll(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (true) {
            val r = input.read(buf)
            if (r < 0) break
            out.write(buf, 0, r)
            if (out.size() > maxBytes) throw IOException("Dữ liệu quá lớn")
        }
        return out.toByteArray()
    }
}
