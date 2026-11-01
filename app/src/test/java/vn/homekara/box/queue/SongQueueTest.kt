package vn.homekara.box.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.homekara.box.domain.Song
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SongQueueTest {
    private fun item(id: String) = QueueItem(Song(id, "Bài $id", "Kênh", 200, ""), "TV")
    private fun ids(q: SongQueue) = q.snapshot().map { it.song.videoId }

    @Test fun fifoOrder() {
        val q = SongQueue(); q.add(item("A")); q.add(item("B"))
        assertEquals("A", q.popNext()!!.song.videoId); assertEquals("B", q.popNext()!!.song.videoId); assertNull(q.popNext())
    }
    @Test fun addDuplicateIsRejected() {
        val q = SongQueue(); q.add(item("A"))
        assertEquals(AddResult.DUPLICATE, q.add(item("A"))); assertEquals(1, q.size)
    }
    @Test fun addNextJumpsToFront() {
        val q = SongQueue(); q.add(item("A")); q.add(item("B")); q.addNext(item("C"))
        assertEquals(listOf("C", "A", "B"), ids(q))
    }
    @Test fun addNextMovesExistingToFront() {
        val q = SongQueue(); q.add(item("A")); q.add(item("B")); q.add(item("C"))
        assertEquals(AddResult.ADDED, q.addNext(item("C"))); assertEquals(listOf("C", "A", "B"), ids(q))
    }
    @Test fun moveAtEdgesIsNoOp() {
        val q = SongQueue(); q.add(item("A")); q.add(item("B"))
        q.moveUp("A"); q.moveDown("B"); assertEquals(listOf("A", "B"), ids(q))
        q.moveDown("A"); assertEquals(listOf("B", "A"), ids(q))
    }
    @Test fun onChangedCalledOnEveryMutation() {
        var n = 0
        val q = SongQueue(onChanged = { n++ })
        q.add(item("A")); q.addNext(item("B")); q.remove("A"); q.popNext()
        assertEquals(4, n)
    }
    @Test fun lastSnapshotPersistedMatchesFinalState() {
        var last: List<QueueItem> = emptyList()
        val q = SongQueue(onChanged = { last = it })
        q.add(item("A")); q.add(item("B")); q.moveUp("B")
        assertEquals(listOf("B", "A"), last.map { it.song.videoId })
    }
    @Test fun concurrentAddsAllLand() {
        val q = SongQueue()
        val pool = Executors.newFixedThreadPool(8)
        val done = CountDownLatch(8)
        for (t in 0 until 8) pool.execute { for (i in 0 until 50) q.add(item("t$t-$i")); done.countDown() }
        done.await(10, TimeUnit.SECONDS); pool.shutdown()
        assertEquals(400, q.size)
    }
}
