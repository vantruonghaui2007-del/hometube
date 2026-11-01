package vn.homekara.box.player

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VocalCutTest {
    private fun run(vararg s: Short): ShortArray = s.copyOf().also { VocalCut.processInterleavedStereo(it) }

    @Test fun centeredSignalIsCancelled() = assertArrayEquals(shortArrayOf(0, 0, 0, 0), run(1000, 1000, -500, -500))
    @Test fun sideSignalIsKeptOnBothChannels() = assertArrayEquals(shortArrayOf(500, 500), run(1000, 0))
    @Test fun noClippingAtExtremes() = assertArrayEquals(shortArrayOf(32767, 32767), run(Short.MAX_VALUE, Short.MIN_VALUE))
    @Test fun oppositeExtremeNegative() = assertArrayEquals(shortArrayOf(-32767, -32767), run(Short.MIN_VALUE, Short.MAX_VALUE))
}
