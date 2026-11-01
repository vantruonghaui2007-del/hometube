package vn.hometube.app.core

import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun sharedLinkAndTimestamp() {
        val x = Links.parse("Xem video này: https://youtu.be/dQw4w9WgXcQ?si=test&t=1m30s")!!
        assertEquals(LinkKind.VIDEO, x.kind); assertEquals("dQw4w9WgXcQ", x.id); assertEquals(90, x.startSec)
    }
    @Test fun mobileMusicShortsAndEmbed() {
        for (s in listOf("https://m.youtube.com/watch?v=dQw4w9WgXcQ", "https://music.youtube.com/watch?v=dQw4w9WgXcQ", "https://youtube.com/shorts/dQw4w9WgXcQ", "https://youtube.com/embed/dQw4w9WgXcQ"))
            assertEquals("dQw4w9WgXcQ", Links.parse(s)?.id)
    }
    @Test fun rejectsSpoofedHostsAndMalformedIds() {
        for (s in listOf("https://evil.com/watch?v=dQw4w9WgXcQ", "https://youtube.com.evil.org/watch?v=dQw4w9WgXcQ", "https://youtube.com@evil.org/watch?v=dQw4w9WgXcQ", "https://youtu.be/short", "javascript:alert(1)", "https://youtube.com/watch?v=dQw4w9WgXcQextra")) assertNull(s, Links.parse(s))
    }
    @Test fun playlistsAndMix() {
        assertEquals(LinkKind.PLAYLIST, Links.parse("https://youtube.com/playlist?list=PLabcdefghijk")?.kind)
        assertEquals(LinkKind.VIDEO, Links.parse("https://youtube.com/watch?v=dQw4w9WgXcQ&list=RDtest")?.kind)
        assertNull(Links.parse("https://youtube.com/playlist?list=RDtest"))
        assertEquals(LinkKind.CHANNEL, Links.parse("https://youtube.com/@VTV24")?.kind)
    }
    @Test fun resumeNearEndAndUnknownDuration() {
        assertEquals(0L, Resume.start(298000,300000)); assertEquals(120000L, Resume.start(120000,300000))
        assertEquals(0L, Resume.start(9000,10000)); assertEquals(4000L, Resume.start(4000,10000))
        assertEquals(60000L, Resume.start(60000,0)); assertEquals(0L, Resume.start(-3,100))
    }
    @Test fun badSponsorJsonAndFiltering() {
        assertTrue(Sponsors.parse("oops").isEmpty())
        assertEquals(listOf(Segment(10000,20000)), Sponsors.parse("""[{"segment":[10,20],"actionType":"skip"},{"segment":[5,2]},{"segment":[30,40],"actionType":"mute"}]"""))
    }
    @Test fun explicitSeekIntoSponsorRespected() {
        val s = Skipper(listOf(Segment(10000,20000)))
        assertEquals(20000L,s.check(11000)); assertNull(s.check(12000))
        s.userSeek(15000); assertNull(s.check(16000))
    }
    @Test fun invalidSegmentsCannotLoop() {
        assertTrue(Sponsors.parse("""[{"segment":[-1,10]},{"segment":[0,1e99]}]""").isEmpty())
    }
    @Test fun localRecommendationDeduplicatesAndAvoidsWatched() {
        val a=Video("aaaaaaaaaaa","A");val b=Video("bbbbbbbbbbb","B");val c=Video("ccccccccccc","C")
        assertEquals(listOf(b,c), Feeds.recommend(listOf(a,b),listOf(b,c),setOf(a.id)))
    }
    @Test fun channelFailureDoesNotLoseSuccessfulFeed() {
        val r=Feeds.load(listOf("ok","fail"), { if(it=="fail") error("rss") else listOf(Video("aaaaaaaaaaa","A")) }, {error("tab")})
        assertEquals(1,r.videos.size);assertEquals(listOf("fail"),r.failed)
    }
    @Test fun rssFailureFallsBackToChannel() {
        val r=Feeds.load(listOf("x"),{error("rss")},{listOf(Video("aaaaaaaaaaa","A"))})
        assertEquals(1,r.videos.size);assertTrue(r.failed.isEmpty())
    }
    @Test fun displayFormatting() {
        assertEquals("4:05", Format.duration(245));assertEquals("1:01:01",Format.duration(3661))
        assertEquals("1,2 Tr lượt xem",Format.views(1250000));assertEquals("",Format.views(-1))
    }
    @Test fun keyboardLimitsAndSpaces() {
        val k=Keyboard();assertFalse(k.type(" "));k.type("a");k.type(" ");assertFalse(k.type(" "))
        k.set("x".repeat(80));assertEquals(60,k.text.length);k.backspace();assertEquals(59,k.text.length)
    }
}
