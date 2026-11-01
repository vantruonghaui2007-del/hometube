package vn.hometube.app.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.widget.*
import org.json.JSONObject
import vn.homekara.box.domain.Song
import vn.homekara.box.player.KaraokePlayer
import vn.hometube.app.TubeApp
import vn.hometube.app.core.*
import vn.hometube.app.data.SponsorClient

class PlayerActivity:BaseActivity(),TubeApp.PlaybackControls {
    private var videos:List<Video> = emptyList()
    private var index=0
    private var video=Video("")
    private var related:List<Video> = emptyList()
    private var playlistQueue:PlaylistQueue?=null
    private var recoverQueue=false
    private var loadingQueue=false
    private var emptyQueuePages=0
    private val ownership=MediaOwnership()
    private var player:KaraokePlayer?=null
    private lateinit var surface:SurfaceView
    private lateinit var hud:LinearLayout
    private lateinit var title:TextView
    private lateinit var meta:TextView
    private lateinit var time:TextView
    private lateinit var timeline:ProgressBar
    private lateinit var notice:TextView
    private lateinit var playButton:TextView
    private lateinit var relatedRow:LinearLayout
    private var token=0
    private var ticks=0
    private var lastInteraction=0L
    private var resumeAt:Long?=null
    private var initialStart=-1
    private var failed=false
    private var ended=false
    private var countdown=-1
    private var pendingNext:Video?=null
    private var skipper:Skipper?=null
    private val seen=HashSet<String>()
    private val audio by lazy {getSystemService(Context.AUDIO_SERVICE) as AudioManager}
    private val audioListener=AudioManager.OnAudioFocusChangeListener {change->
        if(change==AudioManager.AUDIOFOCUS_LOSS){save();dispose();notice.text="Ứng dụng khác đang phát\nBấm OK để tiếp tục HomeTube";notice.visibility=View.VISIBLE;showHud()}
        else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){player?.exo?.playWhenReady=false;showHud()}
        else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){player?.exo?.volume=0.2f}
        else if(change==AudioManager.AUDIOFOCUS_GAIN){player?.exo?.volume=1f}
    }
    private val ticker=object:Runnable {override fun run(){
        player?.let {p->
            val pos=p.exo.currentPosition.coerceAtLeast(0);val dur=p.exo.duration.coerceAtLeast(0)
            timeline.progress=if(dur>0)(pos*1000/dur).toInt()else 0;time.text="${Format.duration((pos/1000).toInt()).ifBlank {"0:00"}}   /   ${Format.duration((dur/1000).toInt()).ifBlank {"--:--"}}"
            playButton.text=if(p.isPaused)"▶ Tiếp tục" else "❚❚ Tạm dừng"
            if(!p.isPaused)skipper?.check(pos)?.let {p.exo.seekTo(it);toast("Đã bỏ qua đoạn tài trợ")}
            if(++ticks%20==0)save()
            if(!p.isPaused&&!failed&&!ended&&SystemClock.elapsedRealtime()-lastInteraction>6000)hud.visibility=View.GONE
        }
        app.main.postDelayed(this,500)
    }}
    private val autoplayTick=object:Runnable{override fun run(){
        if(countdown<0)return
        if(countdown==0){val next=pendingNext;cancelCountdown();if(next!=null)playNext(next);return}
        notice.text="Tiếp theo sau $countdown giây\n${pendingNext?.title}\n\nBack để huỷ";notice.visibility=View.VISIBLE;countdown--;app.main.postDelayed(this,1000)
    }}
    override fun onCreate(s:Bundle?){super.onCreate(s);read(intent);build()}
    @Suppress("DEPRECATION","UNCHECKED_CAST")
    private fun read(i:Intent){
        videos=(i.getSerializableExtra("videos") as? ArrayList<Video>).orEmpty();index=i.getIntExtra("index",0).coerceIn(0,(videos.size-1).coerceAtLeast(0));initialStart=i.getIntExtra("start",-1)
        playlistQueue=app.queues.remove(i.getStringExtra("queue"));recoverQueue=false
        playlistQueue?.let {q->videos=q.videos;index=videos.indexOfFirst {it.id==i.getStringExtra("selectedId")}.coerceAtLeast(0)}
        if(playlistQueue==null&&!i.getStringExtra("playlistUrl").isNullOrBlank()){playlistQueue=PlaylistQueue(i.getStringExtra("playlistUrl")!!,videos,null);recoverQueue=true}
    }
    override fun onNewIntent(i:Intent){super.onNewIntent(i);save();setIntent(i);read(i);resumeAt=null;open(index,initialStart)}
    override fun onResume(){super.onResume();app.controls=this;app.main.removeCallbacks(ticker);app.main.post(ticker);if(videos.isEmpty()){toast("Không có video để phát");finish()}else if(player==null)open(index,initialStart)}
    override fun onPause(){save();token++;cancelCountdown();app.main.removeCallbacks(ticker);if(app.controls===this)app.controls=null;dispose();super.onPause()}
    override fun onDestroy(){token++;app.main.removeCallbacks(ticker);app.main.removeCallbacks(autoplayTick);dispose();super.onDestroy()}
    private fun build(){
        val root=FrameLayout(this).apply {setBackgroundColor(android.graphics.Color.BLACK)};setContentView(root)
        surface=SurfaceView(this);root.addView(surface,FrameLayout.LayoutParams(-1,-1,Gravity.CENTER))
        notice=text("Đang mở video…",22f).apply {gravity=Gravity.CENTER;background=shape(0xD910141A.toInt(),0);setPadding(dp(40),dp(30),dp(40),dp(30))};root.addView(notice,FrameLayout.LayoutParams(-1,-1))
        hud=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(44),dp(22),dp(44),dp(24));background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xE812151C.toInt(),0xFF0C1016.toInt()))}
        title=hud.label("",24f,Palette.text,true);title.maxLines=2;meta=hud.label("",13f,Palette.muted);hud.space(12)
        val progress=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL};timeline=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {max=1000};progress.addView(timeline,LinearLayout.LayoutParams(0,dp(8),1f));time=text("0:00 / --:--",13f,Palette.muted).apply {setPadding(dp(16),0,0,0)};progress.addView(time);hud.addView(progress);hud.space(14)
        val buttons=LinearLayout(this);fun add(label:String,primary:Boolean=false,action:()->Unit):TextView=button(label,primary){lastInteraction=SystemClock.elapsedRealtime();action()}.apply {textSize=14f;setPadding(dp(13),dp(12),dp(13),dp(12));buttons.addView(this,LinearLayout.LayoutParams(-2,dp(44)).apply {rightMargin=dp(8)})}
        playButton=add("❚❚ Tạm dừng",true){toggle()};add("◄ 10s"){seek(-10000)};add("10s ►"){seek(10000)}
        add("Chất lượng"){choices("Chất lượng tối đa",listOf("360p","480p","720p","1080p")){app.quality=listOf(360,480,720,1080)[it];resumeAt=player?.exo?.currentPosition;open(index,-1)}}
        add("Xem sau"){work({val saved=app.store.saved(video.id);app.store.setSaved(video,!saved);!saved}){toast(if(it)"Đã lưu vào Xem sau" else "Đã bỏ khỏi Xem sau")}}
        add("Kênh"){if(video.channelUrl.isNotBlank())Nav.collection(this,video.channelUrl)else toast("Chưa có thông tin kênh")}
        add("Thêm…"){choices(video.title,listOf("Đăng ký / bỏ đăng ký kênh","Hát karaoke bài này","Video liên quan","Video kế tiếp")){when(it){0->if(video.channelUrl.isNotBlank())work({val yes=app.store.subscribed(video.channelUrl);app.store.subscribe(Channel(video.channelUrl,video.channel),!yes);!yes}){toast(if(it)"Đã đăng ký kênh" else "Đã bỏ đăng ký")};1->videoMenu(video);2->showRelated();3->advance(false)}}}
        hud.addView(HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;addView(buttons)})
        relatedRow=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=View.GONE};hud.addView(relatedRow);hud.space(9)
        val bottom=LinearLayout(this);bottom.addView(text("▲ Điều khiển   ▼ Liên quan   Back Quay lại",11f,Palette.muted),LinearLayout.LayoutParams(0,-2,1f));bottom.addView(text("© 2026 Vân Trường",11f,Palette.gold));hud.addView(bottom)
        root.addView(hud,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));showHud()
    }
    private fun ensurePlayer():Boolean {
        if(player!=null)return true
        if(audio.requestAudioFocus(audioListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){notice.text="Chưa nhận được quyền phát âm thanh. Thử lại sau.";notice.visibility=View.VISIBLE;return false}
        player=KaraokePlayer(this,{app.source},{app.quality}).also {p->
            p.attach(surface);p.vocalCut.enabled=false
            p.exo.addListener(object:androidx.media3.common.Player.Listener {
                override fun onVideoSizeChanged(size:androidx.media3.common.VideoSize){surface.post {val parent=surface.parent as? View?:return@post;if(size.width>0&&size.height>0){val ratio=size.width*size.pixelWidthHeightRatio/size.height;val w=minOf(parent.width,(parent.height*ratio).toInt());val h=(w/ratio).toInt();surface.layoutParams=FrameLayout.LayoutParams(w,h,Gravity.CENTER)}}}
            })
            p.listener=object:KaraokePlayer.Listener {
                override fun onPreparing(song:Song){notice.text="Đang tải video…\n${video.title}";notice.visibility=View.VISIBLE}
                override fun onStarted(song:Song){if(song.videoId!=video.id)return;ownership.ready(song.videoId);notice.visibility=View.GONE;failed=false;showHud()}
                override fun onUnplayable(song:Song,reason:String){if(song.videoId!=video.id)return;failed=true;notice.text="$reason\n\nKiểm tra mạng, bấm OK để thử lại\nNếu đang hát, hãy dừng HomeKara trước.";notice.visibility=View.VISIBLE;showHud()}
                override fun onEnded(song:Song){if(song.videoId!=video.id)return;ended=true;save(true);advance(true)}
            }
        };return true
    }
    private fun open(i:Int,start:Int=-1){
        if(i !in videos.indices)return
        val t=++token;ownership.opening();player?.stop();loadingQueue=false;cancelCountdown();failed=false;ended=false;skipper=null;related=emptyList();relatedRow.visibility=View.GONE;index=i;video=videos[i];seen+=video.id;notice.text="Đang mở…\n${video.title}";notice.visibility=View.VISIBLE;showHud()
        if(!ensurePlayer())return
        val cachedPosition=resumeAt;resumeAt=null
        val requestedVideo=video
        work({
            val d=runCatching {app.source.detail(requestedVideo.id)}.getOrNull()
            val pos=cachedPosition?:if(start>=0)start*1000L else app.store.position(requestedVideo.id)
            d to pos
        },error={if(t==token){notice.text="Không mở được video. Bấm OK để thử lại.";failed=true}}){(detail,pos)->
            if(t!=token||player==null)return@work
            if(detail!=null){video=detail.video;related=detail.related}
            title.text=video.title.ifBlank {"Video YouTube"};meta.text=Format.meta(video);initialStart=-1
            val openedVideo=video
            app.writer.execute {app.store.savePosition(openedVideo,pos,openedVideo.duration*1000L)}
            player?.play(Song(video.id,video.title,video.channel,video.duration,video.thumb),0,pos)
            if(app.sponsor)work({SponsorClient.fetch(openedVideo.id)}){if(t==token)skipper=Skipper(it)}
        }
    }
    private fun save(complete:Boolean=false){
        val p=player?:return;if(video.id.isBlank()||!ownership.canSave(video.id))return
        val v=video;val duration=p.exo.duration.coerceAtLeast(0).takeIf {it>0}?:v.duration*1000L
        val position=if(complete)duration else p.exo.currentPosition.coerceAtLeast(0)
        resumeAt=if(complete)0 else position
        app.writer.execute {runCatching {app.store.savePosition(v,position,duration)}}
    }
    private fun dispose(){player?.release();player=null;audio.abandonAudioFocus(audioListener)}
    private fun showHud(){hud.visibility=View.VISIBLE;lastInteraction=SystemClock.elapsedRealtime()}
    private fun toggle(){cancelCountdown();if(player==null||failed||ended){resumeAt=if(ended)0 else resumeAt;open(index,-1)}else player?.togglePause();showHud()}
    private fun seek(delta:Long){cancelCountdown();player?.let {it.seekBy(delta);skipper?.userSeek(it.exo.currentPosition)};showHud()}
    private fun advance(automatic:Boolean){
        val queued=videos.getOrNull(index+1)
        val q=playlistQueue
        if(queued==null&&q!=null&&(q.hasMore||recoverQueue)) {loadQueue(automatic);return}
        val next=queued?:related.firstOrNull {it.id !in seen}
        if(next==null||(automatic&&queued==null&&!app.autoplay)){notice.text="Đã xem xong\nBấm Back để quay lại";notice.visibility=View.VISIBLE;showHud();return}
        if(automatic){pendingNext=next;countdown=8;autoplayTick.run()}else playNext(next)
    }
    private fun loadQueue(automatic:Boolean){
        val q=playlistQueue?:return;if(loadingQueue)return;loadingQueue=true;val t=token;val id=video.id;val cursor=q.next;val recovering=recoverQueue
        notice.text="Đang tải phần tiếp theo của danh sách phát…";notice.visibility=View.VISIBLE;showHud()
        work({
            if(!recovering)app.source.more(cursor!!) else {
                var page=app.source.playlist(q.url).page;var scanned=0;var found:Page?=null
                while(scanned++<100){
                    val clips=page.items.filterIsInstance<Entry.Clip>();val at=clips.indexOfFirst {it.video.id==id}
                    if(at>=0){found=Page(clips.drop(at+1),page.next);break}
                    val n=page.next?:break;page=app.source.more(n)
                }
                found?:throw IllegalArgumentException("Không tìm thấy vị trí trong danh sách phát")
            }
        },error={if(t==token){loadingQueue=false;notice.text="Chưa tải được phần tiếp theo.\nBấm Next để thử lại hoặc Back để quay lại.";notice.visibility=View.VISIBLE;showHud()}}){page->
            if(t!=token)return@work
            loadingQueue=false;recoverQueue=false;val oldSize=videos.size;q.videos=videos;q.append(page);videos=q.videos
            if(videos.size==oldSize&&q.hasMore){if(++emptyQueuePages>5){notice.text="Danh sách chưa có video tiếp theo. Thử lại sau.";return@work}}else emptyQueuePages=0
            advance(automatic)
        }
    }
    private fun playNext(v:Video){save();resumeAt=null;val i=videos.indexOfFirst {it.id==v.id};if(i>=0)open(i,0)else {playlistQueue=null;recoverQueue=false;videos=videos.take(index+1)+v;open(videos.lastIndex,0)}}
    private fun cancelCountdown(){countdown=-1;pendingNext=null;app.main.removeCallbacks(autoplayTick)}
    private fun showRelated(){
        showHud();relatedRow.removeAllViews();relatedRow.visibility=View.VISIBLE;relatedRow.space(14);relatedRow.label("VIDEO LIÊN QUAN",11f,Palette.gold,true)
        val row=LinearLayout(this);related.take(10).forEach {v->row.addView(button(v.title){playNext(v)}.apply {textSize=14f;maxLines=2;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(9),dp(12),dp(9))},LinearLayout.LayoutParams(dp(220),dp(65)).apply {rightMargin=dp(10)})};relatedRow.addView(HorizontalScrollView(this).apply {addView(row)},LinearLayout.LayoutParams(-1,dp(75)));row.getChildAt(0)?.requestFocus();if(related.isEmpty())toast("Chưa có video liên quan")
    }
    override fun dispatchKeyEvent(e:KeyEvent):Boolean {
        if(e.action!=KeyEvent.ACTION_DOWN)return super.dispatchKeyEvent(e)
        when(e.keyCode){
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_MEDIA_PLAY,KeyEvent.KEYCODE_MEDIA_PAUSE->{toggle();return true}
            KeyEvent.KEYCODE_MEDIA_NEXT,KeyEvent.KEYCODE_CHANNEL_UP->{advance(false);return true}
            KeyEvent.KEYCODE_DPAD_DOWN->if(hud.visibility==View.GONE){showRelated();return true}
            KeyEvent.KEYCODE_DPAD_UP->if(hud.visibility==View.GONE){showHud();playButton.requestFocus();return true}
            KeyEvent.KEYCODE_DPAD_LEFT->if(hud.visibility==View.GONE){seek(if(e.repeatCount>3)-30000 else -10000);return true}
            KeyEvent.KEYCODE_DPAD_RIGHT->if(hud.visibility==View.GONE){seek(if(e.repeatCount>3)30000 else 10000);return true}
            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER->if(hud.visibility==View.GONE||failed||player==null||ended){toggle();return true}
        }
        lastInteraction=SystemClock.elapsedRealtime();return super.dispatchKeyEvent(e)
    }
    override fun onBackPressed(){if(countdown>=0){cancelCountdown();notice.text="Đã huỷ tự phát video tiếp theo";showHud()}else if(hud.visibility==View.VISIBLE){hud.visibility=View.GONE}else super.onBackPressed()}
    override fun control(action:String):Boolean=when(action){"pause"->{player?.exo?.playWhenReady=false;showHud();true};"play"->{if(player==null||failed)toggle()else player?.exo?.playWhenReady=true;showHud();true};"toggle"->{toggle();true};"back"->{seek(-10000);true};"forward"->{seek(10000);true};"next"->{advance(false);true};else->false}
    override fun state():JSONObject=JSONObject().put("title",video.title).put("id",video.id).put("paused",player?.isPaused?:true).put("position",player?.exo?.currentPosition?:0).put("duration",player?.exo?.duration?.coerceAtLeast(0)?:0)
}
