package vn.homekara.box.search

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.homekara.box.domain.ToneTag

class ToneTagParserTest {
    @Test fun male() = assertEquals(setOf(ToneTag.MALE), ToneTagParser.parse("Đêm Lạnh Karaoke Tone Nam | Nhạc Sống"))
    @Test fun femaleWithoutDiacritics() = assertEquals(setOf(ToneTag.FEMALE), ToneTagParser.parse("Dem Lanh Karaoke TONE NU"))
    @Test fun femaleWithDiacritics() = assertEquals(setOf(ToneTag.FEMALE), ToneTagParser.parse("Hạ Trắng Karaoke Tone Nữ"))
    @Test fun duetAndBeat() = assertEquals(setOf(ToneTag.DUET, ToneTag.BEAT), ToneTagParser.parse("Song Ca - Beat Chuẩn"))
    @Test fun noTag() = assertEquals(emptySet<ToneTag>(), ToneTagParser.parse("Nơi Này Có Anh Official MV"))
    @Test fun nuInsideWordIsNotFemale() = assertEquals(emptySet<ToneTag>(), ToneTagParser.parse("Karaoke Nụ Cười"))
    @Test fun beatInsideWordIsNotBeat() = assertEquals(emptySet<ToneTag>(), ToneTagParser.parse("Beatles Karaoke"))
}
