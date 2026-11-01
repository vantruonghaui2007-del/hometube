package vn.hometube.app.data

import org.json.JSONArray
import vn.homekara.box.net.Http
import vn.hometube.app.core.*
import java.net.URLEncoder
import java.security.MessageDigest

object SponsorClient {
    fun fetch(videoId:String):List<Segment> = try {
        val hash=MessageDigest.getInstance("SHA-256").digest(videoId.toByteArray(Charsets.UTF_8)).take(2).joinToString(""){"%02x".format(it.toInt() and 255)}
        val categories=URLEncoder.encode("[\"sponsor\",\"selfpromo\",\"interaction\",\"music_offtopic\"]","UTF-8")
        val action=URLEncoder.encode("[\"skip\"]","UTF-8")
        val c=Http.open("https://sponsor.ajay.app/api/skipSegments/$hash?categories=$categories&actionTypes=$action",4000,4000)
        try {if(c.responseCode!=200)emptyList() else {
            val a=JSONArray(c.inputStream.use {String(Http.readAll(it,1024*1024),Charsets.UTF_8)})
            (0 until a.length()).mapNotNull {a.optJSONObject(it)}.firstOrNull {it.optString("videoID")==videoId}?.optJSONArray("segments")?.let {Sponsors.parse(it.toString())}?:emptyList()
        }}finally{c.disconnect()}
    }catch(e:Exception){emptyList()}
}
