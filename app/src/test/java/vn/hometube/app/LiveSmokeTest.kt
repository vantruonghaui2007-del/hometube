package vn.hometube.app

import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry
import vn.homekara.box.net.NewPipeDownloader
import vn.homekara.box.youtube.NewPipeYoutube
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.player.StreamSelector
import vn.homekara.box.youtube.PlayableStreams
import vn.homekara.box.net.Http
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper

class LiveSmokeTest {
    @Test fun publicVideoExtractionHasH264() {
        assumeTrue(System.getenv("HOMETUBE_LIVE_TEST") == "1")
        NewPipe.init(NewPipeDownloader(),Localization("vi","VN"),ContentCountry("VN"))
        val np=NewPipeYoutube()
        val results=np.search("VTV24",SearchFilter.ALL,null)
        println("LIVE_SEARCH_COUNT=${results.songs.size}")
        assertTrue(results.songs.isNotEmpty())
        val streams=np.resolve("dQw4w9WgXcQ")
        println("LIVE_STREAM_COUNT=${streams.size}")
        val selected=StreamSelector.select(streams,720)
        assertNotNull(selected)
        val urls=when(selected){is PlayableStreams.Split->listOf(selected.videoUrl,selected.audioUrl);is PlayableStreams.Progressive->listOf(selected.url);else->emptyList()}
        urls.forEachIndexed {index,url->
            val c=Http.open(url+"&rn=1&range=0-1023")
            try {
                c.requestMethod="POST";c.doOutput=true
                val ua=when {
                    YoutubeParsingHelper.isVisionOsStreamingUrl(url)->YoutubeParsingHelper.getVisionOsUserAgent(null)
                    YoutubeParsingHelper.isAndroidStreamingUrl(url)->YoutubeParsingHelper.getAndroidUserAgent(null)
                    YoutubeParsingHelper.isIosStreamingUrl(url)->YoutubeParsingHelper.getIosUserAgent(null)
                    else->Http.DESKTOP_UA
                }
                c.setRequestProperty("User-Agent",ua)
                if(YoutubeParsingHelper.isWebStreamingUrl(url)){c.setRequestProperty("Origin","https://www.youtube.com");c.setRequestProperty("Referer","https://www.youtube.com/")}
                c.setFixedLengthStreamingMode(2);c.outputStream.use {it.write(byteArrayOf(0x78,0))}
                println("LIVE_MEDIA_${index}_HTTP=${c.responseCode}")
                assertTrue("Media HTTP ${c.responseCode}",c.responseCode in 200..299)
                val read=c.inputStream.use {it.read(ByteArray(1024))};assertTrue("Media response must contain bytes",read>0)
            }finally{c.disconnect()}
        }
    }
}
