package vn.hometube.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import vn.hometube.app.core.*

class BrowserActivity:BaseActivity() {
    private lateinit var cards:Cards
    private lateinit var grid:GridView
    private lateinit var status:TextView
    private lateinit var more:TextView
    private var next:Any?=null
    private var loading=false
    private var token=0
    private val playlist get()=intent.getBooleanExtra("playlist",false)
    private val url get()=intent.getStringExtra("url")?:""
    override fun onCreate(s:Bundle?){super.onCreate(s);load()}
    private fun load(){
        val root=page();val header=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        header.addView(button("◄ Quay lại"){finish()});header.addView(text(if(playlist)"DANH SÁCH PHÁT" else "KÊNH YÊU THÍCH",12f,Palette.gold,true).apply {setPadding(dp(18),0,0,0)});root.addView(header);root.space(20)
        val title=root.label("Đang tải…",30f,Palette.text,true);val sub=root.label("",14f,Palette.muted);root.space(15)
        val actions=LinearLayout(this);root.addView(actions);root.space(15)
        status=root.label("",17f,Palette.muted)
        grid=GridView(this).apply {numColumns=4;horizontalSpacing=dp(14);verticalSpacing=dp(14);selector=shape(android.graphics.Color.TRANSPARENT,10,Palette.gold);setDrawSelectorOnTop(true)};cards=Cards(this);grid.adapter=cards
        grid.setOnItemClickListener {_,_,p,_->when(val e=cards.items[p]){is Entry.Clip->Nav.play(this,e.video,if(playlist)cards.items.filterIsInstance<Entry.Clip>().map {it.video}else listOf(e.video),playlistUrl=if(playlist)url else "",nextPage=next);is Entry.Creator->Nav.collection(this,e.channel.url);is Entry.Playlist->Nav.collection(this,e.url,true)}}
        grid.setOnItemLongClickListener {_,_,p,_->(cards.items[p] as? Entry.Clip)?.let {videoMenu(it.video)};true}
        root.addView(grid,LinearLayout.LayoutParams(-1,0,1f));more=button("Tải thêm video"){loadMore()};more.visibility=View.GONE;root.addView(more);root.addView(footer())
        val t=++token
        work({if(playlist)app.source.playlist(url)else app.source.channel(url)},error={if(t==token){title.text="Không tải được trang";status.text="Kiểm tra mạng hoặc thử lại sau.";actions.addView(button("Thử lại"){load()})}}){p->
            if(t!=token)return@work
            title.text=p.title;sub.text=(p.channel?.let {Format.subscribers(it.subscribers)}?:p.subtitle).take(220);cards.items=p.page.items;next=p.page.next;more.visibility=if(next!=null)View.VISIBLE else View.GONE
            if(cards.count==0)status.text="Chưa có video ở trang này."
            if(playlist)actions.addView(button("▶ Phát tất cả",true){cards.items.filterIsInstance<Entry.Clip>().map {it.video}.let {if(it.isNotEmpty())Nav.play(this,it[0],it,playlistUrl=url,nextPage=next)}})
            p.channel?.let {channel->val b=button("Đăng ký kênh",true){};actions.addView(b)
                fun refresh(){work({app.store.subscribed(channel.url)}){yes->b.text=if(yes)"✔ Đã đăng ký" else "Đăng ký kênh";b.setOnClickListener {work({app.store.subscribe(channel,!yes)}){refresh()}}}}
                refresh()
            }
            if(cards.count>0)grid.requestFocus()
        }
    }
    private fun loadMore(){val n=next?:return;if(loading)return;loading=true;val t=token
        work({app.source.more(n)},error={loading=false;toast("Chưa tải được trang tiếp. Thử lại nhé.")}){p->if(t==token){loading=false;next=p.next;cards.items=(cards.items+p.items).distinctBy {when(it){is Entry.Clip->it.video.id;is Entry.Creator->it.channel.url;is Entry.Playlist->it.url}};more.visibility=if(next!=null)View.VISIBLE else View.GONE}}
    }
    override fun dispatchKeyEvent(e:KeyEvent):Boolean {if(e.action==KeyEvent.ACTION_DOWN&&e.keyCode==KeyEvent.KEYCODE_MENU&&grid.hasFocus()){(cards.items.getOrNull(grid.selectedItemPosition) as? Entry.Clip)?.let {videoMenu(it.video)};return true};return super.dispatchKeyEvent(e)}
    override fun onDestroy(){token++;super.onDestroy()}
}
