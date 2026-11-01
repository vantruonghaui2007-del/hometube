package vn.hometube.app

import android.app.Application
import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import vn.hometube.app.core.*
import vn.hometube.app.data.TubeStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[19],application=Application::class)
class AndroidIntegrationTest {
    private fun fresh():TubeStore {val c=RuntimeEnvironment.getApplication() as Context;c.deleteDatabase("hometube.db");return TubeStore(c)}
    @Test fun historyAndWatchLaterSurviveReopen(){
        val store=fresh();val v=Video("aaaaaaaaaaa","Một ngày bình yên","Vân Trường",duration=300)
        store.savePosition(v,120000,300000);store.setSaved(v,true)
        val reopened=TubeStore(RuntimeEnvironment.getApplication())
        assertEquals(120000L,reopened.position(v.id));assertTrue(reopened.saved(v.id));assertEquals(v,(reopened.entries("continue")[0] as Entry.Clip).video)
        assertTrue(reopened.check())
    }
    @Test fun completedVideoIsNotContinueWatching(){
        val store=fresh();val v=Video("bbbbbbbbbbb","B",duration=300)
        store.savePosition(v,298000,300000);assertTrue(store.entries("continue").isEmpty());assertEquals(0L,store.position(v.id));assertEquals(1,store.entries("history").size)
    }
    @Test fun clearingHistoryPreservesSavedAndSubscriptions(){
        val store=fresh();val v=Video("ccccccccccc","C");val c=Channel("https://youtube.com/@test","Kênh thử")
        store.savePosition(v,3000,0);store.setSaved(v,true);store.subscribe(c,true);store.clearHistory()
        assertTrue(store.entries("history").isEmpty());assertTrue(store.saved(v.id));assertEquals(listOf(c),store.subscriptions());assertEquals(0L,store.position(v.id))
    }
}
