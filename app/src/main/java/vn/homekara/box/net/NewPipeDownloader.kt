package vn.homekara.box.net

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

/** Cầu nối NewPipeExtractor -> HttpURLConnection (đi qua TLS Conscrypt của app). */
class NewPipeDownloader : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val c = Http.open(request.url(), connectMs = 15_000, readMs = 30_000)
        try {
            c.requestMethod = request.httpMethod()
            for ((name, values) in request.headers()) {
                if (values.isEmpty()) continue
                c.setRequestProperty(name, values[0])
                for (i in 1 until values.size) c.addRequestProperty(name, values[i])
            }
            val body = request.dataToSend()
            if (body != null) {
                c.doOutput = true
                c.setFixedLengthStreamingMode(body.size)
                c.outputStream.use { it.write(body) }
            }
            val code = c.responseCode
            if (code == 429) throw ReCaptchaException("reCaptcha Challenge requested", request.url())

            val stream = if (code >= 400) c.errorStream else c.inputStream
            val text = stream?.use { String(Http.readAll(it, 16 * 1024 * 1024), Charsets.UTF_8) }
            val headers = HashMap<String, List<String>>()
            for ((k, v) in c.headerFields) if (k != null) headers[k] = v
            return Response(code, c.responseMessage ?: "", headers, text, c.url.toString())
        } finally {
            c.disconnect()
        }
    }
}
