package vn.hometube.app.remote

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import org.json.JSONArray
import vn.hometube.app.TubeApp
import vn.hometube.app.core.*
import vn.hometube.app.data.TubeStore
import vn.hometube.app.ui.Nav
import java.security.SecureRandom
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class PhoneServer(private val app:TubeApp):NanoHTTPD(8687) {
    val token=ByteArray(24).also {SecureRandom().nextBytes(it)}.joinToString(""){"%02x".format(it.toInt() and 255)}
    private val created=System.currentTimeMillis()
    private fun <T> main(action:()->T):T {val f=FutureTask(action);app.main.post(f);return f.get(3,TimeUnit.SECONDS)}
    override fun serve(s:IHTTPSession):Response = try {
        if(s.uri=="/"&&s.method==Method.GET) {
            newFixedLengthResponse(Response.Status.OK,"text/html; charset=utf-8",app.assets.open("phone/index.html").bufferedReader().use {it.readText()}).apply {
                addHeader("Cache-Control","no-store");addHeader("X-Content-Type-Options","nosniff");addHeader("Referrer-Policy","no-referrer");addHeader("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src https: data:; connect-src 'self'; frame-ancestors 'none'")
            }
        } else {
            val write=s.method!=Method.GET
            val check=RemotePolicy.check(token,s.headers["x-hometube-token"],s.headers["origin"],s.headers["host"]?:"",write,s.headers["content-length"]?.toLongOrNull()?:-1)
            when {
                check!=200->json(check,JSONObject().put("error",if(check==403)"Hãy mở HomeTube và quét lại QR trên TV để ghép điện thoại." else "Kích thước yêu cầu không hợp lệ"))
                System.currentTimeMillis()-created>12*3600000L->json(403,JSONObject().put("error","Phiên ghép đã hết hạn. Đóng rồi mở lại HomeTube và quét QR mới."))
                s.method==Method.GET&&s.uri=="/api/state"->json(200,main {app.controls?.state()?:JSONObject().put("paused",true).put("title","Chưa phát video")})
                s.method==Method.GET&&s.uri=="/api/search"->{
                    val q=s.parms["q"].orEmpty().trim().take(120)
                    if(q.isBlank())json(400,JSONObject().put("error","Nhập từ khoá tìm kiếm")) else {
                        val page=app.source.search(q)
                        json(200,JSONObject().put("videos",JSONArray().apply {page.items.filterIsInstance<Entry.Clip>().forEach {put(TubeStore.encode(it.video))}}))
                    }
                }
                s.method==Method.POST&&s.uri=="/api/play"->{
                    val link=Links.parse(body(s).optString("url"))
                    if(link==null)json(400,JSONObject().put("error","Dán link YouTube hợp lệ: video, kênh hoặc danh sách phát"))else {
                        when(link.kind){
                            LinkKind.CHANNEL->main {Nav.collection(app,link.url)}
                            LinkKind.VIDEO->main {Nav.play(app,Video(link.id),startSec=link.startSec)}
                            LinkKind.PLAYLIST->{
                                val page=app.source.playlist(link.url).page
                                val list=page.items.filterIsInstance<Entry.Clip>().map {it.video}
                                if(list.isEmpty())throw IllegalArgumentException("Danh sách phát chưa có video xem được")
                                main {Nav.play(app,list.firstOrNull {it.id==link.firstVideo}?:list[0],list,link.startSec,playlistUrl=link.url,nextPage=page.next)}
                            }
                        }
                        json(200,JSONObject().put("message","Đang mở trên TV"))
                    }
                }
                s.method==Method.POST&&s.uri=="/api/control"->{
                    val action=body(s).optString("action")
                    val yes=main {app.controls?.control(action)?:false}
                    json(if(yes)200 else 409,JSONObject().put("message",if(yes)"Đã gửi lệnh" else "TV chưa phát video").put("error",if(yes)JSONObject.NULL else "TV chưa phát video hoặc lệnh không hợp lệ"))
                }
                else->json(404,JSONObject().put("error","Không tìm thấy"))
            }
        }
    }catch(e:Exception){json(if(e is IllegalArgumentException)400 else 503,JSONObject().put("error",if(e is IllegalArgumentException)e.message else "Chưa xử lý được. Kiểm tra mạng của box rồi thử lại."))}
    private fun body(s:IHTTPSession):JSONObject {
        if(!s.headers["content-type"].orEmpty().startsWith("application/json"))throw IllegalArgumentException("Yêu cầu phải là JSON")
        val files=HashMap<String,String>();s.parseBody(files);return JSONObject(files["postData"]?:"{}")
    }
    private fun json(code:Int,value:JSONObject)=newFixedLengthResponse(Response.Status.lookup(code),"application/json; charset=utf-8",value.toString()).apply {addHeader("Cache-Control","no-store");addHeader("X-Content-Type-Options","nosniff");addHeader("Referrer-Policy","no-referrer")}
}
