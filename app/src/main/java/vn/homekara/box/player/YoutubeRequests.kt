package vn.homekara.box.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import vn.homekara.box.net.Http
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sửa yêu cầu tải video tới máy chủ googlevideo theo cách của ứng dụng NewPipe
 * (YoutubeHttpDataSource): gửi POST với thân "x\0", đưa vị trí byte vào tham số &range=
 * và đánh số yêu cầu &rn=. Cách này tránh bị YouTube bóp băng thông / trả lỗi 403.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object YoutubeRequests {
    private val POST_BODY = byteArrayOf(0x78, 0)
    private val requestNumber = AtomicInteger(0)

    fun isYoutubePlayback(uri: Uri): Boolean =
        (uri.host ?: "").endsWith("googlevideo.com") && (uri.path ?: "").startsWith("/videoplayback")

    fun transform(spec: DataSpec): DataSpec {
        if (!isYoutubePlayback(spec.uri)) return spec
        val url = spec.uri.toString()
        val sb = StringBuilder(url)
        if (!url.contains("&rn=")) sb.append("&rn=").append(requestNumber.incrementAndGet())
        if (spec.position != 0L || spec.length != C.LENGTH_UNSET.toLong()) {
            sb.append("&range=").append(spec.position).append('-')
            if (spec.length != C.LENGTH_UNSET.toLong()) sb.append(spec.position + spec.length - 1)
        }
        val headers = HashMap(spec.httpRequestHeaders)
        headers["User-Agent"] = userAgentFor(url)
        if (isWeb(url)) {
            headers["Origin"] = "https://www.youtube.com"
            headers["Referer"] = "https://www.youtube.com/"
            headers["Sec-Fetch-Dest"] = "empty"
            headers["Sec-Fetch-Mode"] = "cors"
            headers["Sec-Fetch-Site"] = "cross-site"
        }
        headers["TE"] = "trailers"
        return spec.buildUpon()
            .setUri(Uri.parse(sb.toString()))
            .setHttpMethod(DataSpec.HTTP_METHOD_POST)
            .setHttpBody(POST_BODY)
            .setPosition(0)
            .setLength(C.LENGTH_UNSET.toLong())
            .setHttpRequestHeaders(headers)
            .build()
    }

    private fun isWeb(url: String) = try { YoutubeParsingHelper.isWebStreamingUrl(url) } catch (_: Throwable) { false }

    private fun userAgentFor(url: String): String = try {
        when {
            YoutubeParsingHelper.isVisionOsStreamingUrl(url) -> YoutubeParsingHelper.getVisionOsUserAgent(null)
            YoutubeParsingHelper.isAndroidStreamingUrl(url) -> YoutubeParsingHelper.getAndroidUserAgent(null)
            YoutubeParsingHelper.isIosStreamingUrl(url) -> YoutubeParsingHelper.getIosUserAgent(null)
            else -> Http.DESKTOP_UA
        }
    } catch (_: Throwable) {
        Http.DESKTOP_UA
    }
}
