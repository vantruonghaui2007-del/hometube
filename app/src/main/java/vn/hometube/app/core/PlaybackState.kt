package vn.hometube.app.core

class LoadReadiness {
    private var notified=false
    fun loading(){notified=false}
    fun ready():Boolean {if(notified)return false;notified=true;return true}
}
class MediaOwnership {
    private var owner:String?=null
    fun opening(){owner=null}
    fun ready(id:String){owner=id}
    fun canSave(id:String)=owner!=null&&owner==id
}
class FeedCache {
    private var key:List<String>?=null
    private var at=0L
    private var value:Feeds.Result?=null
    @Synchronized fun load(channels:List<String>,now:Long=System.currentTimeMillis(),fetch:()->Feeds.Result):Feeds.Result {
        val old=value
        if(old!=null&&key==channels&&now-at in 0 until 1800000)return old
        return fetch().also {value=it;key=channels.toList();at=now}
    }
}
data class QueueWindow(val videos:List<Video>,val index:Int) {
    companion object {fun of(videos:List<Video>,selected:Int):QueueWindow {
        if(videos.isEmpty())return QueueWindow(emptyList(),0)
        val i=selected.coerceIn(videos.indices);val start=(i-20).coerceAtLeast(0)
        return QueueWindow(videos.drop(start).take(100),i-start)
    }}
}
class PlaylistQueue(val url:String,var videos:List<Video>,var next:Any?) {
    val hasMore get()=next!=null
    fun append(page:Page){videos=(videos+page.items.filterIsInstance<Entry.Clip>().map {it.video}).distinctBy {it.id};next=page.next}
}
