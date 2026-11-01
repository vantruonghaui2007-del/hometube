package vn.homekara.box.search

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.homekara.box.domain.SearchFilter

class QueryBuilderTest {
    @Test fun appendsKaraoke() = assertEquals("noi nay co anh karaoke", QueryBuilder.build("  Noi nay co anh ", SearchFilter.ALL))
    @Test fun doesNotDuplicateKaraoke() = assertEquals("noi nay co anh karaoke", QueryBuilder.build("noi nay co anh Karaoke", SearchFilter.ALL))
    @Test fun appendsFilterKeyword() = assertEquals("dem lanh karaoke tone nữ", QueryBuilder.build("dem lanh", SearchFilter.FEMALE))
    @Test fun doesNotDuplicateFilterKeyword() = assertEquals("dem lanh tone nu karaoke", QueryBuilder.build("dem lanh tone nu", SearchFilter.FEMALE))
    @Test fun blankStaysBlank() = assertEquals("", QueryBuilder.build("   ", SearchFilter.BEAT))
    @Test fun keepsVietnameseDiacritics() = assertEquals("đêm lạnh karaoke beat", QueryBuilder.build("Đêm  Lạnh", SearchFilter.BEAT))
}
