package vn.homekara.box.player

import org.junit.Assert.assertEquals
import org.junit.Test

class ToneTest {
    @Test fun zeroIsUnity() = assertEquals(1f, Tone.pitchFactor(0), 1e-6f)
    @Test fun octaveDoubles() = assertEquals(2f, Tone.pitchFactor(12), 1e-5f)
    @Test fun plusThree() = assertEquals(1.189207f, Tone.pitchFactor(3), 1e-5f)
    @Test fun minusTwo() = assertEquals(0.890899f, Tone.pitchFactor(-2), 1e-5f)
    @Test fun clampRange() { assertEquals(6, Tone.clamp(9)); assertEquals(-6, Tone.clamp(-7)); assertEquals(2, Tone.clamp(2)) }
    @Test fun labels() { assertEquals("+2", Tone.label(2)); assertEquals("0", Tone.label(0)); assertEquals("−3", Tone.label(-3)) }
}
