package vn.homekara.box.ui

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyBindingsTest {
    private class MapStore : KeyStoreLike {
        val m = HashMap<Int, String>()
        override fun all(): Map<Int, String> = m
        override fun put(keyCode: Int, action: String) { m[keyCode] = action }
        override fun clear() = m.clear()
    }

    @Test fun defaultsWhenHudHidden() {
        val k = KeyBindings(MapStore())
        assertEquals(PlayerAction.TONE_UP, k.actionFor(KeyEvent.KEYCODE_DPAD_UP, false))
        assertEquals(PlayerAction.NEXT_SONG, k.actionFor(KeyEvent.KEYCODE_MEDIA_NEXT, false))
    }
    @Test fun okShowsControlsWhenHudHidden() {
        val k = KeyBindings(MapStore())
        assertEquals(PlayerAction.SHOW_HUD, k.actionFor(KeyEvent.KEYCODE_DPAD_CENTER, false))
        assertEquals(PlayerAction.SHOW_HUD, k.actionFor(KeyEvent.KEYCODE_DPAD_RIGHT, false))
        assertEquals(PlayerAction.TOGGLE_PAUSE, k.actionFor(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false))
    }
    @Test fun dpadFreeWhenHudVisible() = assertNull(KeyBindings(MapStore()).actionFor(KeyEvent.KEYCODE_DPAD_UP, true))
    @Test fun customBindingOverridesDefault() {
        val k = KeyBindings(MapStore()); k.bind(KeyEvent.KEYCODE_PROG_RED, PlayerAction.TONE_UP)
        assertEquals(PlayerAction.TONE_UP, k.actionFor(KeyEvent.KEYCODE_PROG_RED, false))
        k.bind(KeyEvent.KEYCODE_DPAD_UP, PlayerAction.SEEK_FWD)
        assertEquals(PlayerAction.SEEK_FWD, k.actionFor(KeyEvent.KEYCODE_DPAD_UP, false))
    }
    @Test fun resetRestoresDefaults() {
        val k = KeyBindings(MapStore()); k.bind(KeyEvent.KEYCODE_DPAD_UP, PlayerAction.SEEK_FWD); k.reset()
        assertEquals(PlayerAction.TONE_UP, k.actionFor(KeyEvent.KEYCODE_DPAD_UP, false))
    }
}
