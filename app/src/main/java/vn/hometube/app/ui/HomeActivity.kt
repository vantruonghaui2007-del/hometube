package vn.hometube.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import vn.hometube.app.core.*

class HomeActivity:BaseActivity() {
    private data class Section(val key:String,val label:String,val symbol:String,val kiosk:String="",val subtitle:String="")
    private val sections=listOf(
        Section("search","Tìm kiếm","⌕"),Section("foryou","Dành cho bạn","★"),
        Section("music","Âm nhạc","♪","trending_music","Giai điệu hay, khoảnh khắc trọn vẹn."),
        Section("gaming","Trò chơi","▶","trending_gaming","Khám phá thế giới game trên màn hình lớn."),
        Section("movies","Phim & trailer","▶","trending_movies_and_shows","Những câu chuyện đáng để ngồi lại."),
        Section("podcasts","Podcast","●","trending_podcasts_episodes","Lắng nghe một góc nhìn mới."),
        Section("subscriptions","Kênh đăng ký","✔"),Section("continue","Xem tiếp","▶"),Section("later","Xem sau","★"),Section("history","Đã xem","◄"))
    private var selected="music"
    private lateinit var sidebar:LinearLayout
    private lateinit var body:LinearLayout
    private lateinit var grid:GridView
    private lateinit var cards:Cards
    private lateinit var status:TextView
    private lateinit var heading:TextView
    private lateinit var count:TextView
    private lateinit var more:TextView
    private var next:Any?=null
    private var token=0
    private var loading=false
    private var query=""
    private var filter="videos"
    private var searchField:EditText?=null
    private var suggestTask:Runnable?=null

    override fun onCreate(state:Bundle?){super.onCreate(state);selected=state?.getString("section")?:"music";query=state?.getString("query")?:"";render();select(selected)}
    override fun onSaveInstanceState(out:Bundle){out.putString("section",selected);out.putString("query",query);super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();if(::cards.isInitialized&&selected in listOf("continue","history","later","subscriptions","foryou"))select(selected)}
    override fun onDestroy(){token++;suggestTask?.let {app.main.removeCallbacks(it)};super.onDestroy()}

    private fun render(){
        val root=page();val row=LinearLayout(this);root.addView(row,LinearLayout.LayoutParams(-1,0,1f))
        sidebar=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(0,0,dp(22),0)}
        row.addView(sidebar,LinearLayout.LayoutParams(dp(211),-1))
        val brand=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        brand.addView(text("▶",26f,Palette.gold,true).apply {gravity=Gravity.CENTER;background=shape(Palette.raised,12,Palette.gold)},LinearLayout.LayoutParams(dp(46),dp(40)))
        brand.addView(text("HomeTube",23f,Palette.text,true).apply {setPadding(dp(11),0,0,0)})
        sidebar.addView(brand);sidebar.space(23)
        drawSidebar()
        body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL};row.addView(body,LinearLayout.LayoutParams(0,-1,1f))
        val bottom=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(8),0,0)}
        bottom.addView(footer(),LinearLayout.LayoutParams(0,-2,1f));bottom.addView(text("▲▼◄► Di chuyển     OK Chọn     Menu Tuỳ chọn",12f,Palette.muted));root.addView(bottom)
    }
    private fun drawSidebar(){
        while(sidebar.childCount>2)sidebar.removeViewAt(2)
        sections.forEachIndexed {i,s->
            if(i==6){sidebar.space(12);sidebar.addView(text("THƯ VIỆN CỦA BẠN",10f,Palette.muted,true).apply {setPadding(dp(13),0,0,dp(5))})}
            val b=button("${s.symbol}   ${s.label}"){select(s.key)}.apply {gravity=Gravity.CENTER_VERTICAL;setPadding(dp(13),0,dp(6),0);textSize=15f;background=focusBg(selected==s.key);if(selected==s.key)setTextColor(Palette.gold)}
            sidebar.addView(b,LinearLayout.LayoutParams(-1,dp(37)).apply {bottomMargin=dp(3)})
        }
        sidebar.addView(View(this),LinearLayout.LayoutParams(1,0,1f))
        val util=LinearLayout(this)
        util.addView(button("HomeKara"){openKaraoke()}.apply {textSize=13f;setPadding(dp(6),0,dp(6),0)},LinearLayout.LayoutParams(0,dp(40),1f))
        util.addView(button("Cài đặt"){startActivity(Intent(this,SettingsActivity::class.java))}.apply {textSize=14f;setPadding(dp(9),0,dp(9),0)},LinearLayout.LayoutParams(0,dp(40),1f).apply {leftMargin=dp(7)})
        sidebar.addView(util)
    }
    private fun select(key:String){
        selected=key;token++;next=null;loading=false;drawSidebar();body.removeAllViews();searchField=null
        val s=sections.first {it.key==key}
        val top=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        val titles=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        titles.addView(text("KHÁM PHÁ THEO CÁCH CỦA BẠN",10f,Palette.gold,true));titles.space(7)
        heading=text(s.label,30f,Palette.text,true);titles.addView(heading)
        top.addView(titles,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(button("Điện thoại → TV"){startActivity(Intent(this,SettingsActivity::class.java).putExtra("phone",true))})
        body.addView(top);body.space(16)
        if(key=="search")buildSearch() else {
            val hero=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(0xFF29303B.toInt(),Palette.panel)).apply {cornerRadius=dp(14).toFloat()};setPadding(dp(24),dp(17),dp(22),dp(17))}
            hero.label(if(key=="music")"Bật nhạc lên. Thư giãn thôi." else when(key){"continue"->"Câu chuyện của bạn vẫn còn tiếp.";"later"->"Để dành những điều bạn thích.";"history"->"Những khoảnh khắc đã xem.";"subscriptions"->"Gần hơn với những kênh yêu thích.";"foryou"->"Một màn hình, nhiều điều mới.";else->s.label},23f,Palette.text,true)
            hero.space(9);hero.label(s.subtitle.ifBlank {when(key){"subscriptions"->"Video mới từ kênh bạn đăng ký · không cần tài khoản Google";"continue"->"Tiếp tục ngay từ chỗ bạn đã dừng";"later"->"Nhấn Menu trên một video để lưu vào bộ sưu tập";"history"->"Lịch sử được lưu riêng trên box của bạn";else->"Gợi ý từ lịch sử xem và kênh bạn đăng ký"}},14f,Palette.muted)
            body.addView(hero);body.space(14)
        }
        count=text("",13f,Palette.muted);body.addView(count);body.space(9)
        val content=LinearLayout(this)
        if(key=="search")content.addView(keyboard(),LinearLayout.LayoutParams(dp(270),-1).apply {rightMargin=dp(14)})
        val stack=FrameLayout(this)
        grid=GridView(this).apply {numColumns=if(key=="search")3 else 4;horizontalSpacing=dp(12);verticalSpacing=dp(12);stretchMode=GridView.STRETCH_COLUMN_WIDTH;clipToPadding=false;selector=shape(android.graphics.Color.TRANSPARENT,10,Palette.gold);setDrawSelectorOnTop(true);isFocusable=true}
        cards=Cards(this);grid.adapter=cards
        grid.setOnItemClickListener {_,_,p,_->open(cards.items[p])}
        grid.setOnItemLongClickListener {_,_,p,_->(cards.items[p] as? Entry.Clip)?.let {videoMenu(it.video){select(selected)}};true}
        grid.setOnScrollListener(object:AbsListView.OnScrollListener{override fun onScrollStateChanged(v:AbsListView,s:Int){};override fun onScroll(v:AbsListView,f:Int,visible:Int,total:Int){if(total>0&&f+visible>=total-4)loadMore()}})
        status=text("Đang tải…",18f,Palette.muted).apply {gravity=Gravity.CENTER;setPadding(dp(20),dp(20),dp(20),dp(20))}
        stack.addView(grid,FrameLayout.LayoutParams(-1,-1));stack.addView(status,FrameLayout.LayoutParams(-1,-1));content.addView(stack,LinearLayout.LayoutParams(0,-1,1f));body.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        more=button("Thử lại / tải thêm"){if(next!=null)loadMore()else load()}.apply {textSize=13f;setPadding(dp(10),dp(7),dp(10),dp(7));visibility=View.GONE};body.addView(more)
        load()
    }
    private fun buildSearch(){
        val row=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        val field=EditText(this).apply {setSingleLine();textSize=18f;setTextColor(Palette.text);setHintTextColor(Palette.muted);hint="Tìm video, kênh, danh sách phát…";background=shape(Palette.raised,10,Palette.line);setPadding(dp(15),dp(8),dp(15),dp(8));setText(query);imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH;setOnEditorActionListener {_,_,_->submit();true}}
        searchField=field;row.addView(field,LinearLayout.LayoutParams(0,dp(45),1f));row.addView(button("Tìm",true){submit()},LinearLayout.LayoutParams(dp(85),dp(45)).apply {leftMargin=dp(9)});body.addView(row);body.space(8)
        val types=LinearLayout(this);listOf("Video" to "videos","Kênh" to "channels","Danh sách phát" to "playlists").forEach {(label,value)->types.addView(button(label){filter=value;submit()}.apply {textSize=13f;setPadding(dp(13),dp(7),dp(13),dp(7));background=focusBg(filter==value)},LinearLayout.LayoutParams(-2,dp(35)).apply {rightMargin=dp(8)})};body.addView(types)
        val suggestions=LinearLayout(this);body.addView(HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;addView(suggestions)},LinearLayout.LayoutParams(-1,dp(38)))
        field.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){
                suggestTask?.let {app.main.removeCallbacks(it)}
                val prefix=s.toString().trim();suggestions.removeAllViews()
                if(prefix.isNotBlank()) {val t=token;suggestTask=Runnable {work({app.source.suggest(prefix)}){list->if(t==token&&field.text.toString().trim()==prefix){suggestions.removeAllViews();list.take(5).forEach {q->suggestions.addView(button(q){field.setText(q);submit()}.apply {textSize=12f;setPadding(dp(10),dp(6),dp(10),dp(6))},LinearLayout.LayoutParams(-2,dp(31)).apply {rightMargin=dp(6)})}}}};app.main.postDelayed(suggestTask!!,450)}
            }
            override fun afterTextChanged(s:Editable?){}
        })
    }
    private fun keyboard():LinearLayout=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL;background=shape(Palette.panel,12);setPadding(dp(9),dp(9),dp(9),dp(9))
        label("BÀN PHÍM TV",11f,Palette.muted,true);space(8)
        for(chars in "abcdefghijklmnopqrstuvwxyz0123456789".chunked(6)) {
            val row=LinearLayout(this@HomeActivity);chars.forEach {c->row.addView(button(c.toString()){val f=searchField?:return@button;if(f.text.length<60)f.append(c.toString())}.apply {textSize=17f;setPadding(0,0,0,0)},LinearLayout.LayoutParams(0,dp(38),1f).apply {setMargins(dp(2),dp(2),dp(2),dp(2))})};addView(row)
        }
        space(5)
        val row=LinearLayout(this@HomeActivity)
        listOf("Cách" to {searchField?.append(" ")},"Xoá" to {searchField?.let {it.setText(it.text.toString().dropLast(1))}},"Xoá hết" to {searchField?.setText("")}).forEach {(s,a)->row.addView(button(s){a()}.apply {textSize=12f;setPadding(0,0,0,0)},LinearLayout.LayoutParams(0,dp(38),1f).apply {setMargins(dp(2),0,dp(2),0)})};addView(row)
    }
    private fun submit(){query=searchField?.text?.toString()?.trim()?.take(120)?:query;if(query.isBlank()){toast("Nhập từ khoá để tìm kiếm");return};load(true)}
    private fun load(focusResults:Boolean=false){
        val t=++token;val section=selected;loading=true;next=null;cards.items=emptyList();status.visibility=View.VISIBLE;status.text="Đang tải…";more.visibility=View.GONE;count.text=""
        if(section=="search"&&query.isBlank()){loading=false;status.text="Bạn muốn xem gì hôm nay?\nNhập từ khoá hoặc chọn từ bàn phím TV.";return}
        work({
            when(section){
                "search"->app.source.search(query,filter)
                "continue","history","later"->Page(app.store.entries(section))
                "subscriptions"-> {
                    val channels=app.store.subscriptions();val result=app.feed(channels)
                    val clips=result.videos.map {Entry.Clip(it)}
                    FeedPage(Page(clips+channels.map {Entry.Creator(it)}),result.failed.size)
                }
                "foryou"->{
                    val history=app.store.entries("history").filterIsInstance<Entry.Clip>().take(3)
                    val related=history.flatMap {runCatching {app.source.detail(it.video.id).related}.getOrDefault(emptyList())}
                    val subs=app.store.subscriptions();val feed=app.feed(subs)
                    Page(Feeds.recommend(related,feed.videos,app.store.watched()).map {Entry.Clip(it)})
                }
                else->app.source.shelf(sections.first {it.key==section}.kiosk)
            }
        },error={e->if(t==token){loading=false;status.text="Chưa tải được nội dung\nKiểm tra kết nối mạng và thử lại.\n${e.javaClass.simpleName}";more.visibility=View.VISIBLE}}){result->
            if(t!=token)return@work
            val p=if(result is FeedPage)result.page else result as Page
            loading=false;next=p.next;cards.items=p.items;status.visibility=if(p.items.isEmpty())View.VISIBLE else View.GONE
            status.text=when(section){"continue"->"Chưa có video đang xem dở.\nChọn một video để bắt đầu.";"history"->"Lịch sử xem của bạn sẽ xuất hiện ở đây.";"later"->"Bộ sưu tập đang trống.\nBấm Menu trên video rồi chọn Lưu vào Xem sau.";"subscriptions"->"Chưa đăng ký kênh nào.\nTìm kiếm một kênh rồi chọn Đăng ký.";"foryou"->"Gợi ý sẽ xuất hiện khi bạn xem video hoặc đăng ký kênh.\nKhám phá mục Âm nhạc hoặc Tìm kiếm.";else->"Chưa có video ở mục này — thử Tìm kiếm."}
            count.text=if(p.items.isEmpty())"" else (if(p.note.isNotBlank())p.note+" · " else "")+"${p.items.size} nội dung"+(if(result is FeedPage&&result.failed>0)" · ${result.failed} kênh tạm thời không tải được" else "")
            more.visibility=if(next!=null)View.VISIBLE else View.GONE
            if(focusResults&&p.items.isNotEmpty()){grid.requestFocus();grid.setSelection(0)}
        }
    }
    private data class FeedPage(val page:Page,val failed:Int)
    private fun loadMore(){val cursor=next?:return;if(loading)return;loading=true;val t=token
        work({app.source.more(cursor)},error={if(t==token){loading=false;toast("Không tải được trang tiếp. Bấm tải thêm để thử lại.")}}){p->if(t==token){loading=false;next=p.next;cards.items=(cards.items+p.items).distinctBy {when(it){is Entry.Clip->it.video.id;is Entry.Creator->it.channel.url;is Entry.Playlist->it.url}};more.visibility=if(next!=null)View.VISIBLE else View.GONE}}
    }
    private fun open(e:Entry){when(e){is Entry.Clip->Nav.play(this,e.video);is Entry.Creator->Nav.collection(this,e.channel.url);is Entry.Playlist->Nav.collection(this,e.url,true)}}
    override fun dispatchKeyEvent(e:KeyEvent):Boolean {if(e.action==KeyEvent.ACTION_DOWN&&e.keyCode==KeyEvent.KEYCODE_MENU&&::grid.isInitialized&&grid.hasFocus()){(cards.items.getOrNull(grid.selectedItemPosition) as? Entry.Clip)?.let {videoMenu(it.video){select(selected)}};return true};return super.dispatchKeyEvent(e)}
    override fun onBackPressed(){if(selected!="music")select("music")else super.onBackPressed()}
}
