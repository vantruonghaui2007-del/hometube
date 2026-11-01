package vn.hometube.app.core

import java.io.Serializable
import java.util.Locale

data class Video(val id:String,val title:String="",val channel:String="",val channelUrl:String="",val duration:Int=0,
    val thumb:String="https://i.ytimg.com/vi/$id/mqdefault.jpg",val views:Long=-1,val uploaded:String="") : Serializable
data class Channel(val url:String,val name:String,val avatar:String="",val subscribers:Long=-1):Serializable
sealed class Entry:Serializable {
    data class Clip(val video:Video,val progress:Float=0f):Entry()
    data class Creator(val channel:Channel):Entry()
    data class Playlist(val url:String,val name:String,val owner:String,val thumb:String,val count:Long):Entry()
}
data class Page(val items:List<Entry>,val next:Any?=null,val note:String="")
data class Detail(val video:Video,val description:String,val related:List<Video>)
data class CollectionPage(val title:String,val subtitle:String,val avatar:String,val page:Page,val channel:Channel?=null)

object Format {
    fun duration(s:Int):String=if(s<=0) "" else if(s>=3600) String.format(Locale.US,"%d:%02d:%02d",s/3600,s/60%60,s%60) else String.format(Locale.US,"%d:%02d",s/60,s%60)
    private fun number(n:Long):String=when { n>=1000000 -> String.format(Locale.US,"%.1f",(n/100000)/10.0).replace('.',',')+" Tr";n>=1000 -> String.format(Locale.US,"%.1f",(n/100)/10.0).replace('.',',')+" N";else -> n.toString() }
    fun views(n:Long):String=if(n<0) "" else number(n)+" lượt xem"
    fun subscribers(n:Long):String=if(n<0) "" else number(n)+" người đăng ký"
    fun meta(v:Video)=listOf(v.channel,views(v.views),v.uploaded).filter {it.isNotBlank()}.joinToString(" · ")
}
