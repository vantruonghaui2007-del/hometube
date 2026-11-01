package vn.homekara.box.net

import android.content.Context
import android.util.Log
import org.conscrypt.Conscrypt
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Android 4.4 chỉ có TLS 1.0 bật sẵn và kho chứng chỉ gốc từ 2013.
 * Ở đây: cài Conscrypt (TLS 1.2/1.3), gộp chứng chỉ gốc hệ thống với các chứng chỉ gốc
 * đóng gói trong res/raw/ca_*.pem, rồi đặt làm mặc định cho mọi HttpsURLConnection
 * (NewPipeExtractor, ExoPlayer, tải ảnh, cập nhật đều đi qua đây).
 */
object Tls {
    private const val TAG = "HomeKara/Tls"

    @Volatile lateinit var socketFactory: SSLSocketFactory
        private set
    @Volatile lateinit var trustManager: X509TrustManager
        private set

    fun install(context: Context) {
        val provider = Conscrypt.newProvider()
        Security.insertProviderAt(provider, 1)

        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        var n = 0
        try {
            val system = KeyStore.getInstance("AndroidCAStore").apply { load(null, null) }
            for (alias in system.aliases()) {
                val cert = system.getCertificate(alias) ?: continue
                ks.setCertificateEntry("sys_${n++}", cert)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Không đọc được kho chứng chỉ hệ thống", e)
        }
        val cf = CertificateFactory.getInstance("X.509")
        val res = context.resources
        for (field in vn.homekara.box.R.raw::class.java.fields) {
            if (!field.name.startsWith("ca_")) continue
            try {
                res.openRawResource(field.getInt(null)).use { input ->
                    for (c in cf.generateCertificates(input)) {
                        ks.setCertificateEntry("bundled_${field.name}_${n++}", c as X509Certificate)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi đọc ${field.name}", e)
            }
        }

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(ks)
        trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()

        val ctx = SSLContext.getInstance("TLS", provider)
        ctx.init(null, arrayOf(trustManager), null)
        socketFactory = ctx.socketFactory
        HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory)
        try { SSLContext.setDefault(ctx) } catch (_: Exception) { }
        Log.i(TAG, "TLS sẵn sàng: $n chứng chỉ gốc")
    }
}
