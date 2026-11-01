package vn.hometube.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import vn.hometube.app.ui.*
import java.io.File
import java.util.concurrent.TimeUnit

/** Native-rendered actual Views with live search fixtures; this is not a screenshot from the box. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=TubeApp::class,qualifiers="w1280dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class RenderPreviewTest {
    private fun texts(v:View):List<TextView> = (if(v is TextView)listOf(v)else emptyList())+(if(v is ViewGroup)(0 until v.childCount).flatMap {texts(v.getChildAt(it))}else emptyList())
    private fun images(v:View):List<android.widget.ImageView> = (if(v is android.widget.ImageView)listOf(v)else emptyList())+(if(v is ViewGroup)(0 until v.childCount).flatMap {images(v.getChildAt(it))}else emptyList())
    @Test fun renderHomeAndSearch(){
        assumeTrue(System.getenv("HOMETUBE_RENDER")=="1")
        val controller=Robolectric.buildActivity(HomeActivity::class.java).create().start().resume().visible()
        val a=controller.get();val root=a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val out=File("build/outputs/previews").apply {mkdirs()}
        fun snapshot(name:String){
            root.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY));root.layout(0,0,1280,720)
            val deadline=System.currentTimeMillis()+12000
            do {ShadowLooper.idleMainLooper();Thread.sleep(100)}while(System.currentTimeMillis()<deadline&&images(root).any {it.drawable==null})
            root.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY));root.layout(0,0,1280,720)
            val bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);root.draw(Canvas(bitmap));File(out,name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
        try {
            val page=TubeApp.instance.io.submit<vn.hometube.app.core.Page>{TubeApp.instance.source.search("nhạc Việt Nam")}.get(45,TimeUnit.SECONDS)
            ShadowLooper.idleMainLooper()
            val token=HomeActivity::class.java.getDeclaredField("token").apply {isAccessible=true};token.setInt(a,token.getInt(a)+1)
            val adapter=HomeActivity::class.java.getDeclaredField("cards").apply {isAccessible=true}.get(a) as Cards;adapter.items=page.items
            (HomeActivity::class.java.getDeclaredField("status").apply {isAccessible=true}.get(a) as View).visibility=View.GONE
            (HomeActivity::class.java.getDeclaredField("count").apply {isAccessible=true}.get(a) as TextView).text="Gợi ý qua tìm kiếm · ${page.items.size} video"
            snapshot("hometube-home.png")
            texts(root).first {it.isFocusable&&it.text.toString().contains("Tìm kiếm")}.performClick()
            snapshot("hometube-search.png")
        }finally{controller.pause().stop().destroy();TubeApp.instance.server?.stop();TubeApp.instance.io.shutdownNow();TubeApp.instance.writer.shutdownNow()}
    }
}
