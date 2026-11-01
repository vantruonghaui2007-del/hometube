package vn.hometube.app.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import vn.homekara.box.remote.NetworkInfo
import vn.homekara.box.remote.QrCode
import vn.homekara.box.net.Http
import vn.homekara.box.player.StreamSelector
import vn.homekara.box.R

class SettingsActivity:BaseActivity() {
    private lateinit var rows:LinearLayout
    override fun onCreate(s:Bundle?){super.onCreate(s);render()}
    private fun render(){
        val root=page();val top=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        top.addView(text("Cài đặt",30f,Palette.text,true),LinearLayout.LayoutParams(0,-2,1f));top.addView(button("◄ Quay lại"){finish()});root.addView(top);root.space(18)
        val scroll=ScrollView(this);rows=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL};scroll.addView(rows);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        rows.label("DÀNH RIÊNG CHO TRẢI NGHIỆM CỦA BẠN",11f,Palette.gold,true);rows.space(12)
        row("Chất lượng video","Tối đa ${app.quality}p · H.264 phù hợp với box") {choices("Chất lượng tối đa",listOf("360p","480p","720p","1080p")){app.quality=listOf(360,480,720,1080)[it];render()}}
        row("Tự phát video tiếp theo",if(app.autoplay)"Đang bật · có đếm ngược để huỷ" else "Đang tắt"){app.autoplay=!app.autoplay;render()}
        row("Bỏ qua đoạn tài trợ",if(app.sponsor)"SponsorBlock đang bật · dựa trên dữ liệu cộng đồng" else "SponsorBlock đang tắt"){app.sponsor=!app.sponsor;render()}
        row("Lề an toàn cho TV","${app.prefs.getInt("inset",5)}% · tránh nội dung bị cắt ở mép TV"){choices("Lề an toàn",(0..10).map {"$it%"}){app.prefs.edit().putInt("inset",it).apply();render()}}
        row("Xoá lịch sử xem","Xem sau và kênh đăng ký vẫn được giữ lại"){confirm("Xoá lịch sử?","Xoá lịch sử và vị trí xem trên box này."){work({app.store.clearHistory()}){toast("Đã xoá lịch sử")}}}
        row("Kiểm tra hệ thống","Kết nối, cơ sở dữ liệu, YouTube và luồng H.264"){diagnostics()}
        rows.space(14);rows.label("ĐIỆN THOẠI → MÀN HÌNH LỚN",11f,Palette.gold,true);rows.space(12)
        val phone=LinearLayout(this).apply {background=shape(Palette.panel,14);setPadding(dp(18),dp(18),dp(18),dp(18));gravity=Gravity.CENTER_VERTICAL}
        val qr=ImageView(this);phone.addView(qr,LinearLayout.LayoutParams(dp(126),dp(126)))
        val info=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,0,0)};info.label("Chọn video bằng điện thoại",21f,Palette.text,true);info.space(9)
        val address=info.label("Đang đọc kết nối…",14f,Palette.muted);info.space(8);info.label("Cùng Wi-Fi với box · mở HomeTube trước khi kết nối",13f,Palette.muted);phone.addView(info,LinearLayout.LayoutParams(0,-2,1f));rows.addView(phone)
        app.main.postDelayed({if(!isFinishing)work({NetworkInfo.lanIpv4()}){ip->val server=app.server
            if(ip==null||server==null){address.text=app.serverError?:"Chưa có mạng LAN/Wi-Fi hoặc máy chủ chưa sẵn sàng"}
            else {val url="http://$ip:8687/?k=${server.token}";qr.setImageBitmap(QrCode.bitmap(url,dp(126)));address.text="Quét QR để ghép điện thoại\n$ip:8687 · giữ mã QR riêng tư"}
        }},350)
        rows.space(18);row("Mở từ HomeBox Lite","Cài đặt HomeBox Lite → Ô YouTube → chọn HomeTube"){choices("Gán ô YouTube trong HomeBox Lite",listOf("1. Cài HomeTube.apk lên box","2. Mở Cài đặt của HomeBox Lite","3. Chọn Ô YouTube / Đổi ứng dụng","4. Chọn HomeTube rồi bấm OK")) {}}
        row("Giới thiệu HomeTube","Thiết kế & phát triển bởi Vân Trường · 1.0.0") {
            android.app.AlertDialog.Builder(this).setTitle("HomeTube · Vân Trường").setMessage("© 2026 Vân Trường\n\nXem video theo cách của bạn.\n\nNền tảng phát/TLS kế thừa dự án HomeKara. NewPipeExtractor: Team NewPipe, GPL-3.0-or-later; AndroidX Media3: Apache-2.0; Conscrypt: Apache-2.0; NanoHTTPD: BSD; ZXing: Apache-2.0. Mã nguồn và thông báo giấy phép được cung cấp cùng APK.\n\nHomeTube không liên kết với Google/YouTube. Dành cho sử dụng cá nhân. Đoạn tài trợ trong nội dung chỉ được bỏ qua khi có dữ liệu SponsorBlock.").setPositiveButton("Đóng",null).show()
        }
        root.addView(footer())
        if(intent.getBooleanExtra("phone",false))scroll.post {scroll.smoothScrollTo(0,phone.top)}
    }
    private fun row(title:String,subtitle:String,click:()->Unit){
        val box=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;isFocusable=true;isClickable=true;background=focusBg();setPadding(dp(18),dp(13),dp(18),dp(13));setOnClickListener {click()}}
        box.label(title,18f,Palette.text,true);box.space(5);box.label(subtitle,13f,Palette.muted);rows.addView(box,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(9)})
    }
    private fun diagnostics(){
        val msg=TextView(this).apply {text="Đang kiểm tra, vui lòng đợi…";textSize=16f;setPadding(dp(24),dp(18),dp(24),dp(18))}
        val dialog=android.app.AlertDialog.Builder(this).setTitle("Kiểm tra hệ thống").setView(msg).setPositiveButton("Đóng",null).show()
        work({
            val lines=mutableListOf("Android ${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}","Máy: ${android.os.Build.MODEL}")
            fun check(name:String,test:()->Boolean){lines+="$name: "+try{if(test())"✔" else "✘"}catch(e:Exception){"✘ (${e.javaClass.simpleName})"}}
            check("Cơ sở dữ liệu"){app.store.check()};check("Internet / TLS"){Http.getBytes("https://www.youtube.com/robots.txt",1024*1024).isNotEmpty()};check("Tìm kiếm YouTube"){app.source.search("VTV24").items.isNotEmpty()};check("Trích xuất H.264"){StreamSelector.select(app.source.resolve("dQw4w9WgXcQ"),720)!=null}
            lines+="Điện thoại 8687: "+if(app.server!=null)"✔" else "✘";lines+="Phát thực tế và remote cần kiểm tra trực tiếp trên box.";lines.joinToString("\n\n")
        }){if(dialog.isShowing)msg.text=it}
    }
}
