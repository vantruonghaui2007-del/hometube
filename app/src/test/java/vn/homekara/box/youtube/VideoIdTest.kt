package vn.homekara.box.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoIdTest {
    @Test fun watchUrl() = assertEquals("dQw4w9WgXcQ", NewPipeYoutube.videoIdFromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=3"))
    @Test fun shortUrl() = assertEquals("dQw4w9WgXcQ", NewPipeYoutube.videoIdFromUrl("https://youtu.be/dQw4w9WgXcQ"))
    @Test fun shorts() = assertEquals("dQw4w9WgXcQ", NewPipeYoutube.videoIdFromUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    @Test fun garbage() = assertNull(NewPipeYoutube.videoIdFromUrl("https://example.com/"))
}
