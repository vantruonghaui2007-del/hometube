package vn.hometube.app.core

import org.junit.Test
import org.junit.Assert.*

class PlaybackStateTest {
    @Test fun boundedQueuePreservesSelectionAfterHundredthItem(){
        val list=(0..149).map {Video(it.toString().padStart(11,'0'),"Video $it")}
        val q=QueueWindow.of(list,125);assertTrue(q.videos.size<=100);assertEquals(list[125],q.videos[q.index])
    }
    @Test fun playlistKeepsCursorUntilFollowingPageArrives(){
        val a=Video("aaaaaaaaaaa","A");val b=Video("bbbbbbbbbbb","B")
        val q=PlaylistQueue("https://youtube.com/playlist?list=PLtest",listOf(a),"page2")
        assertTrue(q.hasMore);q.append(Page(listOf(Entry.Clip(a),Entry.Clip(b)),null));assertFalse(q.hasMore);assertEquals(listOf(a,b),q.videos)
    }
    @Test fun retriedLoadNotifiesReadyAgain(){
        val state=LoadReadiness();state.loading();assertTrue(state.ready());assertFalse(state.ready())
        state.loading();assertTrue("A 403 reload must dismiss loading overlay when ready",state.ready())
    }
    @Test fun switchingBeforeMediaReadyCannotWriteOldPositionToNewVideo(){
        val state=MediaOwnership();state.ready("old");assertTrue(state.canSave("old"));state.opening()
        assertFalse(state.canSave("new"));assertFalse(state.canSave("old"));state.ready("new");assertTrue(state.canSave("new"));assertFalse(state.canSave("old"))
    }
    @Test fun subscriptionCacheExpiresAndInvalidatesWhenChannelChanges(){
        val cache=FeedCache();var calls=0
        fun fetch()=Feeds.Result(listOf(Video("aaaaaaaaaaa","A")),emptyList()).also {calls++}
        cache.load(listOf("a"),0,::fetch);cache.load(listOf("a"),1000,::fetch);assertEquals(1,calls)
        cache.load(listOf("a"),1800000,::fetch);assertEquals(2,calls)
        cache.load(listOf("b"),1800001,::fetch);assertEquals(3,calls)
    }
}
