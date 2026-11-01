package vn.homekara.box.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import vn.homekara.box.R
import vn.homekara.box.domain.Song
import vn.homekara.box.domain.ToneTag
import vn.homekara.box.queue.QueueItem
import vn.homekara.box.search.ToneTagParser

class SongGridAdapter(private val inflater: LayoutInflater) : BaseAdapter() {
    var songs: List<Song> = emptyList()
        set(v) { field = v; notifyDataSetChanged() }
    var favorites: Set<String> = emptySet()
        set(v) { field = v; notifyDataSetChanged() }

    override fun getCount() = songs.size
    override fun getItem(position: Int): Song = songs[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: inflater.inflate(R.layout.item_song, parent, false)
        val s = songs[position]
        v.findViewById<TextView>(R.id.title).text = s.title
        v.findViewById<TextView>(R.id.channel).text = s.channel
        v.findViewById<TextView>(R.id.duration).apply {
            text = s.durationText
            visibility = if (s.durationText.isEmpty()) View.GONE else View.VISIBLE
        }
        v.findViewById<View>(R.id.fav).visibility = if (s.videoId in favorites) View.VISIBLE else View.GONE
        val tags = ToneTagParser.parse(s.title)
        v.findViewById<TextView>(R.id.tags).apply {
            if (tags.isEmpty()) visibility = View.GONE
            else {
                visibility = View.VISIBLE
                text = tags.joinToString(" · ") { it.label }
                setBackgroundColor(tagBg(tags.first()))
            }
        }
        Thumbs.load(v.findViewById<ImageView>(R.id.thumb), s.thumbnailUrl)
        return v
    }

    private fun tagBg(t: ToneTag): Int = when (t) {
        ToneTag.MALE -> 0xE61F3A5C.toInt()
        ToneTag.FEMALE -> 0xE65A2340.toInt()
        ToneTag.DUET -> 0xE63B2F66.toInt()
        ToneTag.BEAT -> 0xE61E4A35.toInt()
    }
}

class QueueAdapter(private val inflater: LayoutInflater) : BaseAdapter() {
    var items: List<QueueItem> = emptyList()
        set(v) { field = v; notifyDataSetChanged() }

    override fun getCount() = items.size
    override fun getItem(position: Int): QueueItem = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: inflater.inflate(R.layout.item_queue, parent, false)
        val q = items[position]
        v.findViewById<TextView>(R.id.num).text = (position + 1).toString()
        v.findViewById<TextView>(R.id.title).text = q.song.title
        v.findViewById<TextView>(R.id.by).text = "Chọn bởi: ${q.addedBy}"
        return v
    }
}
