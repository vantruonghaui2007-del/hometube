package vn.homekara.box.remote

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.Inet4Address
import java.net.NetworkInterface

object QrCode {
    fun bitmap(text: String, sizePx: Int): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(m.width * m.height)
        for (y in 0 until m.height) for (x in 0 until m.width) {
            pixels[y * m.width + x] = if (m.get(x, y)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.RGB_565)
    }
}

object NetworkInfo {
    /** IPv4 trong mạng nhà, ưu tiên cáp (eth0) rồi Wi‑Fi (wlan0). */
    fun lanIpv4(): String? = try {
        val all = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
        val ordered = all.sortedBy { when { it.name.startsWith("eth") -> 0; it.name.startsWith("wlan") -> 1; else -> 2 } }
        ordered.asSequence()
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }

    fun connectionLabel(): String {
        val ifs = try { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() } catch (e: Exception) { emptyList() }
        val up = ifs.filter { it.isUp && !it.isLoopback && it.inetAddresses.toList().any { a -> a is Inet4Address } }
        return when {
            up.any { it.name.startsWith("eth") } -> "Mạng dây"
            up.any { it.name.startsWith("wlan") } -> "Wi‑Fi"
            else -> "Chưa có mạng"
        }
    }
}
