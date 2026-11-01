package vn.hometube.app.core

import org.junit.Test
import org.junit.Assert.*
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import vn.hometube.app.data.TubeSource

class MapperTest {
    @Test fun dropsLiveButKeepsShortVideos(){
        val v=StreamInfoItem(0,"https://youtube.com/watch?v=aaaaaaaaaaa","Short",StreamType.VIDEO_STREAM).apply {duration=25}
        val live=StreamInfoItem(0,"https://youtube.com/watch?v=bbbbbbbbbbb","Live",StreamType.LIVE_STREAM)
        val mapped=TubeSource.map(listOf(v,live,v));assertEquals(1,mapped.size);assertEquals(25,(mapped[0] as Entry.Clip).video.duration)
    }
    @Test fun choosesThumbnailAtMost360(){
        assertEquals("small",TubeSource.thumb(listOf(Image("large",720,1280,Image.ResolutionLevel.HIGH),Image("small",360,640,Image.ResolutionLevel.MEDIUM))))
        assertEquals("",TubeSource.thumb(emptyList()))
    }
}
