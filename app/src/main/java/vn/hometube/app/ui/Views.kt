@file:Suppress("DEPRECATION")
package vn.hometube.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import vn.homekara.box.ui.TvActivity
import vn.homekara.box.ui.Thumbs
import vn.hometube.app.TubeApp
import vn.hometube.app.core.*

object Palette {val bg=Color.rgb(13,16,21);val panel=Color.rgb(23,28,36);val raised=Color.rgb(33,40,51);val line=Color.rgb(50,59,72);val gold=Color.rgb(255,181,71);val text=Color.rgb(241,244,248);val muted=Color.rgb(151,163,182)}
fun Context.dp(v:Int)=(v*resources.displayMetrics.density+0.5f).toInt()
fun Context.text(label:String,size:Float=16f,color:Int=Palette.text,bold:Boolean=false)=TextView(this).apply {text=label;textSize=size;setTextColor(color);if(bold)typeface=Typeface.create("sans-serif-medium",Typeface.BOLD);includeFontPadding=false}
fun Context.shape(color:Int,radius:Int=10,stroke:Int=0)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat();if(stroke!=0)setStroke(dp(3),stroke)}
fun Context.focusBg(active:Boolean=false,primary:Boolean=false)=StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_focused),shape(if(primary)Palette.gold else Palette.raised,10,if(primary)Palette.text else Palette.gold))
    addState(intArrayOf(android.R.attr.state_pressed),shape(Palette.gold,10))
    addState(intArrayOf(),shape(if(primary)Palette.gold else if(active)Palette.raised else Palette.panel,10,if(active)Palette.gold else 0))
}
fun Context.button(label:String,primary:Boolean=false,action:()->Unit)=text(label,16f,if(primary)Palette.bg else Palette.text,true).apply {
    gravity=Gravity.CENTER;isFocusable=true;isClickable=true;background=focusBg(primary=primary);setPadding(dp(18),dp(12),dp(18),dp(12));setOnClickListener {action()}
}
fun LinearLayout.space(h:Int){addView(View(context),LinearLayout.LayoutParams(1,context.dp(h)))}
fun LinearLayout.label(s:String,size:Float=16f,color:Int=Palette.text,bold:Boolean=false):TextView=context.text(s,size,color,bold).also {addView(it)}

abstract class BaseActivity:TvActivity() {
    val app get()=TubeApp.instance
    override fun onResume(){super.onResume();val root=findViewById<ViewGroup>(android.R.id.content).getChildAt(0);if(root is LinearLayout){val inset=app.prefs.getInt("inset",5);root.setPadding(dp(1280*inset/100),dp(720*inset/100),dp(1280*inset/100),dp(720*inset/100))}}
    fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
    fun <T> work(task:()->T,error:(Throwable)->Unit={toast("Không tải được. Kiểm tra mạng rồi thử lại.")},done:(T)->Unit) {
        app.io.execute {val result=runCatching(task);app.main.post {if(!isFinishing&&!isDestroyed)result.fold(done,error)}}
    }
    fun choices(title:String,items:List<String>,action:(Int)->Unit){AlertDialog.Builder(this).setTitle(title).setItems(items.toTypedArray()){_,i->action(i)}.setNegativeButton("Đóng",null).show()}
    fun confirm(title:String,body:String,action:()->Unit){AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Đồng ý"){_,_->action()}.setNegativeButton("Huỷ",null).show()}
    fun page():LinearLayout=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL;setBackgroundColor(Palette.bg)
        val inset=app.prefs.getInt("inset",5);setPadding(dp(1280*inset/100),dp(720*inset/100),dp(1280*inset/100),dp(720*inset/100));setContentView(this)
    }
    fun footer():TextView=text("© 2026  Vân Trường",13f,Palette.gold).apply {setPadding(0,dp(8),0,0)}
    fun openKaraoke(){
        val i=packageManager.getLaunchIntentForPackage("vn.homekara.box")
        if(i==null)toast("Chưa cài HomeKara trên box") else try{startActivity(i)}catch(e:Exception){toast("Không mở được HomeKara")}
    }
}
object Nav {
    fun play(context:Context,video:Video,list:List<Video> = listOf(video),startSec:Int=-1,playlistUrl:String="",nextPage:Any?=null){
        val all=list.ifEmpty {listOf(video)};val index=all.indexOfFirst {it.id==video.id}.coerceAtLeast(0)
        val window=QueueWindow.of(all,index)
        val key=if(playlistUrl.isNotBlank()||all.size>100)java.util.UUID.randomUUID().toString() else ""
        if(key.isNotEmpty()){
            val queues=TubeApp.instance.queues;queues[key]=PlaylistQueue(playlistUrl,all,nextPage)
            while(queues.size>3)queues.remove(queues.keys.first())
        }
        context.startActivity(Intent(context,PlayerActivity::class.java).putExtra("videos",ArrayList(window.videos)).putExtra("index",window.index).putExtra("selectedId",video.id).putExtra("queue",key).putExtra("playlistUrl",playlistUrl).putExtra("start",startSec).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun collection(context:Context,url:String,playlist:Boolean=false){context.startActivity(Intent(context,BrowserActivity::class.java).putExtra("url",url).putExtra("playlist",playlist).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
}
class Cards(private val activity:BaseActivity):BaseAdapter() {
    private data class Holder(val image:ImageView,val title:TextView,val meta:TextView,val badge:TextView,val progress:ProgressBar)
    var items:List<Entry> = emptyList();set(v){field=v;notifyDataSetChanged()}
    override fun getCount()=items.size
    override fun getItem(p:Int)=items[p]
    override fun getItemId(p:Int)=p.toLong()
    override fun getView(p:Int,convert:View?,parent:ViewGroup):View {
        val card=convert as? LinearLayout?:build()
        val e=items[p];val holder=card.tag as Holder;val image=holder.image;val title=holder.title;val meta=holder.meta;val badge=holder.badge;val progress=holder.progress
        val thumb:String
        when(e){
            is Entry.Clip->{val v=e.video;title.text=v.title.ifBlank {"Video YouTube"};meta.text=listOf(v.channel,Format.views(v.views)).filter {it.isNotBlank()}.joinToString(" · ");badge.text=Format.duration(v.duration);thumb=v.thumb;progress.progress=(e.progress*1000).toInt()}
            is Entry.Creator->{title.text=e.channel.name;meta.text=Format.subscribers(e.channel.subscribers);badge.text="KÊNH";thumb=e.channel.avatar;progress.progress=0}
            is Entry.Playlist->{title.text=e.name;meta.text=e.owner;badge.text="▶ ${e.count.coerceAtLeast(0)} video";thumb=e.thumb;progress.progress=0}
        }
        image.scaleType=if(e is Entry.Creator)ImageView.ScaleType.CENTER_INSIDE else ImageView.ScaleType.CENTER_CROP
        Thumbs.load(image,thumb);badge.visibility=if(badge.text.isBlank())View.GONE else View.VISIBLE
        progress.visibility=if(progress.progress>0)View.VISIBLE else View.GONE
        return card
    }
    private fun build():LinearLayout=LinearLayout(activity).apply {
        orientation=LinearLayout.VERTICAL;descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS;background=activity.focusBg();setPadding(activity.dp(4),activity.dp(4),activity.dp(4),activity.dp(9));layoutParams=AbsListView.LayoutParams(-1,activity.dp(202))
        val frame=FrameLayout(activity)
        frame.addView(ImageView(activity).apply {tag="image";setBackgroundColor(Palette.raised)},FrameLayout.LayoutParams(-1,-1))
        frame.addView(activity.text("",12f).apply {tag="badge";background=activity.shape(0xDD080B10.toInt(),4);setPadding(activity.dp(6),activity.dp(3),activity.dp(6),activity.dp(3))},FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM or Gravity.RIGHT).apply {setMargins(0,0,activity.dp(6),activity.dp(6))})
        frame.addView(ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal).apply {tag="progress";max=1000;progressDrawable=android.graphics.drawable.ClipDrawable(activity.shape(Palette.gold,0),Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL)},FrameLayout.LayoutParams(-1,activity.dp(3),Gravity.BOTTOM))
        addView(frame,LinearLayout.LayoutParams(-1,activity.dp(125)))
        addView(activity.text("",16f,Palette.text,true).apply {tag="title";maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(activity.dp(7),activity.dp(9),activity.dp(7),0)},LinearLayout.LayoutParams(-1,activity.dp(49)))
        addView(activity.text("",13f,Palette.muted).apply {tag="meta";maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(activity.dp(7),activity.dp(4),activity.dp(7),0)})
        tag=Holder(findViewWithTag("image"),findViewWithTag("title"),findViewWithTag("meta"),findViewWithTag("badge"),findViewWithTag("progress"))
    }
}
fun BaseActivity.videoMenu(v:Video,onChanged:()->Unit={}) {
    work({app.store.saved(v.id)}) {saved->
        choices(v.title,listOf("▶ Phát video",if(saved)"Bỏ khỏi Xem sau" else "Lưu vào Xem sau","Mở kênh","Hát karaoke bài này")){i->when(i){
            0->Nav.play(this,v)
            1->work({app.store.setSaved(v,!saved)}){toast(if(saved)"Đã bỏ khỏi Xem sau" else "Đã lưu vào Xem sau");onChanged()}
            2->if(v.channelUrl.isNotEmpty())Nav.collection(this,v.channelUrl)else toast("Video chưa có thông tin kênh")
            3->confirm("Mở HomeKara","Mở HomeKara rồi tìm bài “${v.title}”. Phiên bản APK riêng chưa thể thêm trực tiếp vào hàng chờ của HomeKara."){openKaraoke()}
        }}
    }
}
