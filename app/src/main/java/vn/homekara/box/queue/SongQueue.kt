package vn.homekara.box.queue

import vn.homekara.box.domain.Song
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

data class QueueItem(val song: Song, val addedBy: String)

enum class AddResult { ADDED, DUPLICATE }

/**
 * Hàng chờ dùng chung cho remote TV và điện thoại; an toàn khi nhiều luồng cùng gọi.
 * [onChanged] và listener được gọi (vẫn trong khoá) sau mỗi thay đổi, theo đúng thứ tự thay đổi.
 */
class SongQueue(
    initial: List<QueueItem> = emptyList(),
    private val onChanged: (List<QueueItem>) -> Unit = {},
) {
    private val lock = Any()
    private val items = ArrayList(initial)
    private val listeners = CopyOnWriteArrayList<(List<QueueItem>) -> Unit>()

    fun addListener(l: (List<QueueItem>) -> Unit) { listeners.add(l) }
    fun removeListener(l: (List<QueueItem>) -> Unit) { listeners.remove(l) }

    fun add(item: QueueItem): AddResult = synchronized(lock) {
        if (indexOf(item.song.videoId) >= 0) return AddResult.DUPLICATE
        items.add(item)
        changed()
        AddResult.ADDED
    }

    /** Ưu tiên: đưa lên đầu (nếu đã có trong hàng thì chuyển lên đầu). */
    fun addNext(item: QueueItem): AddResult = synchronized(lock) {
        val i = indexOf(item.song.videoId)
        items.add(0, if (i >= 0) items.removeAt(i) else item)
        changed()
        AddResult.ADDED
    }

    fun remove(videoId: String): Unit = synchronized(lock) {
        val i = indexOf(videoId)
        if (i >= 0) { items.removeAt(i); changed() }
    }

    fun moveUp(videoId: String): Unit = synchronized(lock) {
        val i = indexOf(videoId)
        if (i > 0) { Collections.swap(items, i, i - 1); changed() }
    }

    fun moveDown(videoId: String): Unit = synchronized(lock) {
        val i = indexOf(videoId)
        if (i >= 0 && i < items.size - 1) { Collections.swap(items, i, i + 1); changed() }
    }

    fun popNext(): QueueItem? = synchronized(lock) {
        if (items.isEmpty()) return null
        val head = items.removeAt(0)
        changed()
        head
    }

    fun peek(): QueueItem? = synchronized(lock) { items.firstOrNull() }

    fun clear(): Unit = synchronized(lock) {
        if (items.isNotEmpty()) { items.clear(); changed() }
    }

    fun shuffle(random: Random = Random.Default): Unit = synchronized(lock) {
        if (items.size > 1) { items.shuffle(random); changed() }
    }

    fun snapshot(): List<QueueItem> = synchronized(lock) { ArrayList(items) }

    val size: Int get() = synchronized(lock) { items.size }

    fun contains(videoId: String): Boolean = synchronized(lock) { indexOf(videoId) >= 0 }

    private fun indexOf(videoId: String) = items.indexOfFirst { it.song.videoId == videoId }

    private fun changed() {
        val snap: List<QueueItem> = ArrayList(items)
        onChanged(snap)
        for (l in listeners) l(snap)
    }
}
