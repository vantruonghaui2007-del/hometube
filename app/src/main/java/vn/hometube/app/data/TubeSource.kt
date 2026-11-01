package vn.hometube.app.data

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.Page as NPage
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.feed.FeedInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import vn.homekara.box.youtube.NewPipeYoutube
import vn.homekara.box.youtube.StreamCandidate
import vn.homekara.box.youtube.StreamResolver
import vn.homekara.box.youtube.UnplayableException
import vn.hometube.app.core.*
import java.util.LinkedHashMap

class TubeSource : StreamResolver {
    private val service get()=ServiceList.YouTube
    private val base=NewPipeYoutube()
    private data class Cursor(val kind:String,val url:String,val filter:String,val page:NPage,val tab:ListLinkHandler?=null)
    private val details=object:LinkedHashMap<String,Pair<Long,Detail>>(8,0.75f,true) {override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Pair<Long,Detail>>?)=size>4}
    private val streams=object:LinkedHashMap<String,Pair<Long,List<StreamCandidate>>>(8,0.75f,true) {override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Pair<Long,List<StreamCandidate>>>?)=size>4}
    fun search(query:String,filter:String="videos"):Page {
        val handler=service.searchQHFactory.fromQuery(query.trim().take(120),listOf(filter),"")
        val info=SearchInfo.getInfo(service,handler)
        return Page(map(info.relatedItems),cursor("search",query,filter,info.nextPage))
    }
    fun shelf(id:String):Page {
        val result=runCatching {
            if(id !in service.kioskList.availableKiosks)return@runCatching Page(emptyList())
            val info=KioskInfo.getInfo(service.kioskList.getExtractorById(id,null))
            Page(map(info.relatedItems),cursor("shelf",info.url,"",info.nextPage))
        }.getOrNull()
        if(result!=null&&result.items.isNotEmpty())return result
        val query=when(id){"trending_music"->"nhạc Việt Nam";"trending_gaming"->"game Việt Nam";"trending_movies_and_shows"->"trailer phim";"trending_podcasts_episodes"->"podcast tiếng Việt";else->return Page(emptyList())}
        return search(query).copy(note="Gợi ý qua tìm kiếm · chưa có bảng xếp hạng cho khu vực")
    }
    fun more(next:Any):Page {
        val c=next as Cursor
        val p=when(c.kind) {
            "search" -> SearchInfo.getMoreItems(service,service.searchQHFactory.fromQuery(c.url,listOf(c.filter),""),c.page)
            "playlist" -> PlaylistInfo.getMoreItems(service,c.url,c.page)
            "channel" -> ChannelTabInfo.getMoreItems(service,c.tab!!,c.page)
            else -> KioskInfo.getMoreItems(service,c.url,c.page)
        }
        return Page(map(p.items),cursor(c.kind,c.url,c.filter,p.nextPage,c.tab))
    }
    fun channel(url:String):CollectionPage {
        val c=ChannelInfo.getInfo(service,url)
        val tab=c.tabs.firstOrNull { it.contentFilters.any {f->f.equals("videos",true)} }?:c.tabs.firstOrNull()
        val info=tab?.let {ChannelTabInfo.getInfo(service,it)}
        return CollectionPage(c.name,c.description?:"",thumb(c.avatars),
            Page(map(info?.relatedItems?:emptyList()),cursor("channel",c.url,"",info?.nextPage,tab)),Channel(c.url,c.name,thumb(c.avatars),c.subscriberCount))
    }
    fun playlist(url:String):CollectionPage {
        val p=PlaylistInfo.getInfo(service,url)
        return CollectionPage(p.name,"${p.uploaderName} · ${p.streamCount} video",thumb(p.thumbnails),Page(map(p.relatedItems),cursor("playlist",p.url,"",p.nextPage)))
    }
    fun latest(url:String):List<Video> = map(FeedInfo.getInfo(service,url).relatedItems).filterIsInstance<Entry.Clip>().map {it.video}.take(15)
    fun channelVideos(url:String):List<Video> = channel(url).page.items.filterIsInstance<Entry.Clip>().map {it.video}.take(15)
    fun suggest(prefix:String):List<String> = base.suggest(prefix)
    fun detail(id:String):Detail {
        synchronized(details){details[id]?.takeIf {System.currentTimeMillis()-it.first<240000}?.let {return it.second}}
        val i=StreamInfo.getInfo(service,"https://www.youtube.com/watch?v=$id")
        if(live(i.streamType))throw UnplayableException("HomeTube chưa hỗ trợ video trực tiếp")
        val d=Detail(Video(id,i.name,i.uploaderName?:"",i.uploaderUrl?:"",i.duration.toInt().coerceAtLeast(0),thumb(i.thumbnails).ifEmpty {Video(id).thumb},i.viewCount,i.textualUploadDate?:""),i.description?.content?:"",map(i.relatedItems).filterIsInstance<Entry.Clip>().map {it.video}.filter {it.id!=id})
        synchronized(details){details[id]=System.currentTimeMillis() to d};return d
    }
    override fun resolve(videoId:String):List<StreamCandidate> {
        synchronized(streams){streams[videoId]?.takeIf {System.currentTimeMillis()-it.first<240000}?.let {return it.second}}
        return base.resolve(videoId).also {synchronized(streams){streams[videoId]=System.currentTimeMillis() to it}}
    }
    override fun invalidate(videoId:String){synchronized(streams){streams.remove(videoId)}}
    private fun cursor(kind:String,url:String,filter:String,page:NPage?,tab:ListLinkHandler?=null):Any? = if(page!=null&&NPage.isValid(page))Cursor(kind,url,filter,page,tab)else null
    companion object {
        fun thumb(images:List<Image>):String = images.filter {it.height in 1..360}.maxByOrNull {it.height}?.url?:images.firstOrNull()?.url?:""
        private fun live(t:StreamType)=t==StreamType.LIVE_STREAM||t==StreamType.AUDIO_LIVE_STREAM
        fun map(items:List<InfoItem>):List<Entry> {
            val seen=HashSet<String>()
            return items.mapNotNull {i->when(i){
                is StreamInfoItem -> {
                    val id=Links.parse(i.url)?.takeIf {it.kind==LinkKind.VIDEO}?.id
                    if(id==null||live(i.streamType)||!seen.add("v$id"))null else Entry.Clip(Video(id,i.name?:"",i.uploaderName?:"",i.uploaderUrl?:"",i.duration.toInt().coerceAtLeast(0),thumb(i.thumbnails).ifEmpty {Video(id).thumb},i.viewCount,i.textualUploadDate?:""))
                }
                is ChannelInfoItem -> if(seen.add(i.url))Entry.Creator(Channel(i.url,i.name?:"",thumb(i.thumbnails),i.subscriberCount))else null
                is PlaylistInfoItem -> if(seen.add(i.url))Entry.Playlist(i.url,i.name?:"",i.uploaderName?:"",thumb(i.thumbnails),i.streamCount)else null
                else -> null
            }}
        }
    }
}
