package vn.hometube.app.core

object Resume {
    fun start(position:Long,duration:Long):Long {
        if(position<=0)return 0
        if(duration<=0)return position
        val margin=minOf(30000L,duration/10)
        return if(position>=duration-margin)0 else position.coerceAtMost(duration)
    }
    fun progress(p:Long,d:Long):Float=if(d>0)(p.toFloat()/d).coerceIn(0f,1f)else 0f
}
class Keyboard {
    var text="";private set
    fun type(s:String):Boolean {if(text.length+s.length>60 || (s==" "&&(text.isEmpty()||text.endsWith(' '))))return false;text+=s;return true}
    fun backspace(){text=text.dropLast(1)}
    fun set(s:String){text=s.take(60)}
    fun clear(){text=""}
}
object Feeds {
    data class Result(val videos:List<Video>,val failed:List<String>)
    fun recommend(related:List<Video>,subscriptions:List<Video>,watched:Set<String>):List<Video> = (subscriptions+related).distinctBy {it.id}.filter {it.id !in watched}.take(80)
    fun load(urls:List<String>,rss:(String)->List<Video>,tab:(String)->List<Video>):Result {
        val failed=mutableListOf<String>();val all=urls.flatMap {url->try {rss(url)}catch(e:Exception){try {tab(url)}catch(e:Exception){failed+=url;emptyList()}}}
        return Result(all.distinctBy {it.id},failed)
    }
}
