package vn.homekara.box.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.homekara.box.youtube.PlayableStreams.Progressive
import vn.homekara.box.youtube.PlayableStreams.Split
import vn.homekara.box.youtube.StreamCandidate
import vn.homekara.box.youtube.StreamCandidate.Kind.AUDIO_ONLY
import vn.homekara.box.youtube.StreamCandidate.Kind.PROGRESSIVE
import vn.homekara.box.youtube.StreamCandidate.Kind.VIDEO_ONLY

class StreamSelectorTest {
    private fun v(h: Int, codec: String = "avc1.4d401f", br: Int = h * 1000) = StreamCandidate("v$h$codec", VIDEO_ONLY, codec, h, br)
    private fun a(codec: String, br: Int) = StreamCandidate("a$codec$br", AUDIO_ONLY, codec, 0, br)

    @Test fun picksHighestAvcAtOrBelowMax() = assertEquals(
        Split("v720avc1.4d401f", "amp4a.40.2128000"),
        StreamSelector.select(listOf(v(1080), v(720), v(480), v(1440, "vp9"), a("mp4a.40.2", 128000), a("opus", 160000)), 720))

    @Test fun ignoresVp9AndAv1EvenIfHigher() = assertEquals(
        "v480avc1.4d401f",
        (StreamSelector.select(listOf(v(720, "vp9"), v(720, "av01.0.05M.08"), v(480), a("mp4a.40.2", 128000)), 720) as Split).videoUrl)

    @Test fun prefersAacOverOpus() = assertTrue(
        (StreamSelector.select(listOf(v(720), a("opus", 160000), a("mp4a.40.2", 48000)), 720) as Split).audioUrl.startsWith("amp4a"))

    @Test fun fallsBackToProgressiveWhenNoSplit() = assertEquals(
        Progressive("p"),
        StreamSelector.select(listOf(StreamCandidate("p", PROGRESSIVE, "avc1.42001E", 360, 500000), v(720, "vp9")), 720))

    @Test fun selectReturnsNullWhenNoAvcAndNoProgressive() =
        assertNull(StreamSelector.select(listOf(v(720, "vp9"), a("opus", 160000)), 720))

    @Test fun maxHeight480Respected() = assertEquals(
        "v480avc1.4d401f",
        (StreamSelector.select(listOf(v(1080), v(720), v(480), a("mp4a.40.2", 128000)), 480) as Split).videoUrl)
}
