package vn.homekara.box.youtube

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import vn.homekara.box.domain.SearchFilter
import vn.homekara.box.domain.Song
import vn.homekara.box.search.QueryBuilder

/** Tìm kiếm + lấy luồng YouTube ngay trên box bằng NewPipeExtractor. */
class NewPipeYoutube : SongSearch, StreamResolver {

    private val service get() = ServiceList.YouTube

    private class Cursor(val query: String, val page: Page)

    override fun search(userText: String, filter: SearchFilter, next: Any?): SearchPage {
        val cursor = next as? Cursor
        val query = cursor?.query ?: QueryBuilder.build(userText, filter)
        if (query.isEmpty()) return SearchPage(emptyList(), null)
        val handler = service.searchQHFactory.fromQuery(
            query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
        val items: List<InfoItem>
        val nextPage: Page?
        if (cursor == null) {
            val info = SearchInfo.getInfo(service, handler)
            items = info.relatedItems
            nextPage = info.nextPage
        } else {
            val more = SearchInfo.getMoreItems(service, handler, cursor.page)
            items = more.items
            nextPage = more.nextPage
        }
        val songs = items.mapNotNull { toSong(it) }
        val hasNext = nextPage != null && Page.isValid(nextPage)
        return SearchPage(songs, if (hasNext) Cursor(query, nextPage!!) else null)
    }

    fun suggest(prefix: String): List<String> = try {
        if (prefix.isBlank()) emptyList()
        else service.suggestionExtractor.suggestionList(prefix.trim()).take(8)
    } catch (e: Exception) {
        emptyList()
    }

    override fun resolve(videoId: String): List<StreamCandidate> {
        val info = try {
            StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")
        } catch (e: AgeRestrictedContentException) {
            throw UnplayableException("Video giới hạn độ tuổi", e)
        } catch (e: GeographicRestrictionException) {
            throw UnplayableException("Video bị chặn tại Việt Nam", e)
        } catch (e: ContentNotAvailableException) {
            throw UnplayableException("Video không còn tồn tại hoặc không xem được", e)
        }
        if (info.streamType == StreamType.LIVE_STREAM || info.streamType == StreamType.AUDIO_LIVE_STREAM) {
            throw UnplayableException("Không hát được video đang phát trực tiếp")
        }
        val out = ArrayList<StreamCandidate>()
        for (s in info.videoOnlyStreams) if (usable(s)) {
            out += StreamCandidate(s.content, StreamCandidate.Kind.VIDEO_ONLY, s.codec ?: "", s.height, s.bitrate)
        }
        for (s in info.audioStreams) if (usable(s)) {
            val br = if (s.averageBitrate > 0) s.averageBitrate * 1000 else s.bitrate
            out += StreamCandidate(s.content, StreamCandidate.Kind.AUDIO_ONLY, s.codec ?: "", 0, br)
        }
        for (s in info.videoStreams) if (usable(s)) {
            out += StreamCandidate(s.content, StreamCandidate.Kind.PROGRESSIVE, s.codec ?: guessCodec(s), s.height, s.bitrate)
        }
        return out
    }

    private fun usable(s: Stream) = s.isUrl && s.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP

    /** Luồng có cả hình lẫn tiếng (itag 18 = MP4 H.264 360p) đôi khi không ghi codec. */
    private fun guessCodec(s: Stream): String =
        if (s.format?.mimeType?.contains("mp4") == true) "avc1" else ""

    private fun toSong(item: InfoItem): Song? {
        if (item !is StreamInfoItem) return null
        if (item.streamType == StreamType.LIVE_STREAM || item.streamType == StreamType.AUDIO_LIVE_STREAM) return null
        val duration = item.duration.toInt()
        if (duration in 0 until 60) return null   // bỏ Shorts / video quá ngắn
        val id = videoIdFromUrl(item.url) ?: return null
        val thumb = item.thumbnails
            .filter { it.height in 1..360 }
            .maxByOrNull { it.height }?.url
            ?: item.thumbnails.firstOrNull()?.url
            ?: "https://i.ytimg.com/vi/$id/mqdefault.jpg"
        return Song(id, item.name ?: "", item.uploaderName ?: "", duration.coerceAtLeast(0), thumb)
    }

    companion object {
        private val ID = Regex("(?:v=|youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})")
        fun videoIdFromUrl(url: String?): String? = url?.let { ID.find(it)?.groupValues?.get(1) }
    }
}
