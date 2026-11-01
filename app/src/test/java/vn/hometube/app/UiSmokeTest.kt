package vn.hometube.app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import vn.hometube.app.ui.HomeActivity
import vn.hometube.app.ui.Cards
import vn.hometube.app.core.Entry
import vn.hometube.app.core.Video

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[19],application=TubeApp::class,qualifiers="w1280dp-h720dp-land-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class UiSmokeTest {
    private fun texts(v:View):List<TextView> = (if(v is TextView)listOf(v)else emptyList())+(if(v is ViewGroup)(0 until v.childCount).flatMap {texts(v.getChildAt(it))}else emptyList())
    @Test fun tvHomeAndSearchStartOnApi19(){
        val controller=Robolectric.buildActivity(HomeActivity::class.java).create().start().resume().visible()
        try {
            val a=controller.get();val root=a.window.decorView
            assertTrue(texts(root).any {it.text.toString()=="HomeTube"})
            assertTrue(texts(root).any {it.text.toString().contains("Vân Trường")})
            val search=texts(root).first {it.isFocusable&&it.text.toString().contains("Tìm kiếm")}
            assertTrue(search.requestFocus());assertTrue(search.performClick())
            assertTrue(texts(root).any {it.text.toString()=="BÀN PHÍM TV"})
            val key=texts(root).first {it.isFocusable&&it.text.toString()=="a"};assertTrue(key.performClick())
            assertTrue(texts(root).any {it is android.widget.EditText&&it.text.toString()=="a"})
            val adapter=Cards(a);adapter.items=listOf(Entry.Clip(Video("aaaaaaaaaaa","A",thumb="")))
            val parent=android.widget.FrameLayout(a);val card=adapter.getView(0,null,parent)
            assertSame("Image loader tag changes must not break recycled cards",card,adapter.getView(0,card,parent))
        }finally{controller.pause().stop().destroy();TubeApp.instance.server?.stop();TubeApp.instance.io.shutdownNow();TubeApp.instance.writer.shutdownNow()}
    }
}
