package vn.hometube.app.core

import java.net.URI
import java.net.URLDecoder

enum class LinkKind { VIDEO,PLAYLIST,CHANNEL }
data class TubeLink(val kind:LinkKind,val id:String,val startSec:Int=0,val firstVideo:String="") {
    val url:String get()=when(kind) {LinkKind.VIDEO->"https://www.youtube.com/watch?v=$id";LinkKind.PLAYLIST->"https://www.youtube.com/playlist?list=$id";LinkKind.CHANNEL->"https://www.youtube.com/$id"}
}
object Links {
    private val videoId=Regex("[A-Za-z0-9_-]{11}")
    private val hosts=setOf("youtube.com","www.youtube.com","m.youtube.com","music.youtube.com","youtu.be","www.youtu.be")
    fun parse(text:String):TubeLink? {
        for(m in Regex("https?://[^\\s<>]+",RegexOption.IGNORE_CASE).findAll(text.take(4096))) {
            val result=runCatching { parseUrl(m.value.trimEnd('.',',',')',']','!',';','"')) }.getOrNull()
            if(result!=null)return result
        }
        return null
    }
    private fun parseUrl(raw:String):TubeLink? {
        val u=URI(raw);val host=u.host?.lowercase()?:return null
        if(host !in hosts || u.rawUserInfo!=null || u.port !in listOf(-1,80,443))return null
        val q=(u.rawQuery?:"").split('&').mapNotNull { val x=it.split('=',limit=2);if(x.size==2) URLDecoder.decode(x[0],"UTF-8") to URLDecoder.decode(x[1],"UTF-8") else null }.toMap()
        val path=u.path.trim('/')
        val id=when {host.endsWith("youtu.be")->path;path=="watch"->q["v"];path.startsWith("shorts/")||path.startsWith("embed/")||path.startsWith("v/")->path.substringAfter('/');else->null}?.takeIf {videoId.matches(it)}
        val start=time(q["t"]?:q["start"]?:u.fragment?.removePrefix("t=")?:"")
        val list=q["list"]?.takeIf { it.length in 10..100 && Regex("[A-Za-z0-9_-]+").matches(it) && !it.startsWith("RD") }
        if(list!=null)return TubeLink(LinkKind.PLAYLIST,list,start,id?:"")
        if(id!=null)return TubeLink(LinkKind.VIDEO,id,start)
        if((path.startsWith('@')||path.startsWith("channel/")||path.startsWith("c/")||path.startsWith("user/")) && path.length in 2..200 && !path.contains("..")) {
            val base=path.split('/').let { if(it[0].startsWith('@'))it[0] else it.take(2).joinToString("/") }
            return TubeLink(LinkKind.CHANNEL,base)
        }
        return null
    }
    fun time(s:String):Int {
        s.toLongOrNull()?.let {return it.coerceIn(0,86400).toInt()}
        val m=Regex("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$").matchEntire(s)?:return 0
        return ((m.groupValues[1].toLongOrNull()?:0)*3600+(m.groupValues[2].toLongOrNull()?:0)*60+(m.groupValues[3].toLongOrNull()?:0)).coerceIn(0,86400).toInt()
    }
}
