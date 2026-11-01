package vn.homekara.box.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {
    private val json = """{"versionCode":5,"versionName":"1.0.4","apkUrl":"https://example.com/HomeKara.apk","notes":"Sửa lỗi"}"""

    @Test fun newerVersionReturned() = assertEquals(5, UpdateChecker.parse(json, 4)!!.versionCode)
    @Test fun sameVersionReturnsNull() = assertNull(UpdateChecker.parse(json, 5))
    @Test fun malformedJsonReturnsNull() = assertNull(UpdateChecker.parse("<html>", 1))
    @Test fun nonHttpUrlRejected() = assertNull(UpdateChecker.parse("""{"versionCode":9,"apkUrl":"file:///x.apk"}""", 1))
}
