package vn.hometube.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.multidex.MultiDexApplication
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import vn.homekara.box.net.NewPipeDownloader
import vn.homekara.box.net.Tls
import vn.hometube.app.data.*
import vn.hometube.app.remote.PhoneServer
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import vn.hometube.app.core.*

class TubeApp:MultiDexApplication() {
    val io=Executors.newFixedThreadPool(4)
    val writer=Executors.newSingleThreadExecutor()
    val main=Handler(Looper.getMainLooper())
    val source by lazy {TubeSource()}
    val store by lazy {TubeStore(this)}
    val queues=LinkedHashMap<String,PlaylistQueue>()
    private val feedCache=FeedCache()
    fun feed(channels:List<Channel>):Feeds.Result = feedCache.load(channels.map {it.url}) {
        val pool=Executors.newFixedThreadPool(3)
        try {
            val futures=pool.invokeAll(channels.map {ch->Callable {Feeds.load(listOf(ch.url),source::latest,source::channelVideos)}},60,TimeUnit.SECONDS)
            val videos=mutableListOf<Video>();val failed=mutableListOf<String>()
            futures.forEachIndexed {i,f->try {val r=f.get();videos+=r.videos;if(r.failed.isNotEmpty())failed+=channels[i].name}catch(e:Exception){failed+=channels[i].name}}
            Feeds.Result(videos.distinctBy {it.id},failed)
        }finally{pool.shutdownNow()}
    }
    val prefs by lazy {getSharedPreferences("hometube",MODE_PRIVATE)}
    var server:PhoneServer?=null;private set
    var serverError:String?=null;private set
    var controls:PlaybackControls?=null
    var quality:Int get()=prefs.getInt("quality",720);set(v){prefs.edit().putInt("quality",v).apply()}
    var autoplay:Boolean get()=prefs.getBoolean("autoplay",true);set(v){prefs.edit().putBoolean("autoplay",v).apply()}
    var sponsor:Boolean get()=prefs.getBoolean("sponsor",true);set(v){prefs.edit().putBoolean("sponsor",v).apply()}
    override fun onCreate(){
        super.onCreate();instance=this
        try {Tls.install(this)}catch(e:Throwable){Log.e("HomeTube","TLS",e)}
        NewPipe.init(NewPipeDownloader(),Localization("vi","VN"),ContentCountry("VN"))
        var visible=0
        val stop=Runnable {if(visible==0){server?.stop();server=null}}
        registerActivityLifecycleCallbacks(object:Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(a:Activity){
                visible++;main.removeCallbacks(stop)
                if(server==null)try{server=PhoneServer(this@TubeApp).also {it.start(5000,false)};serverError=null}catch(e:Exception){server=null;serverError="Không mở được cổng 8687";Log.w("HomeTube",serverError,e)}
            }
            override fun onActivityStopped(a:Activity){visible--;if(visible==0)main.postDelayed(stop,700)}
            override fun onActivityCreated(a:Activity,s:Bundle?){}
            override fun onActivityResumed(a:Activity){}
            override fun onActivityPaused(a:Activity){}
            override fun onActivitySaveInstanceState(a:Activity,b:Bundle){}
            override fun onActivityDestroyed(a:Activity){}
        })
    }
    interface PlaybackControls {fun control(action:String):Boolean;fun state():org.json.JSONObject}
    companion object {lateinit var instance:TubeApp;private set}
}
