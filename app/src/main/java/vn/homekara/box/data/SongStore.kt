package vn.homekara.box.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import vn.homekara.box.domain.Song
import vn.homekara.box.queue.QueueItem

class KaraokeDb(context: Context, name: String = "homekara.db") :
    SQLiteOpenHelper(context, name, null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE songs(video_id TEXT PRIMARY KEY, title TEXT, channel TEXT, duration_sec INTEGER, thumb TEXT)")
        db.execSQL("CREATE TABLE favorites(video_id TEXT PRIMARY KEY, added_at INTEGER)")
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, video_id TEXT, played_at INTEGER)")
        db.execSQL("CREATE INDEX history_vid ON history(video_id)")
        db.execSQL("CREATE TABLE tone_prefs(video_id TEXT PRIMARY KEY, semitones INTEGER)")
        db.execSQL("CREATE TABLE queue(position INTEGER, video_id TEXT, added_by TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

/** Lưu yêu thích, lịch sử, tone theo bài và hàng chờ (còn nguyên khi rút điện). */
class SongStore(private val helper: KaraokeDb) {

    private val db: SQLiteDatabase get() = helper.writableDatabase

    private fun upsertSong(d: SQLiteDatabase, s: Song) {
        val v = ContentValues().apply {
            put("video_id", s.videoId); put("title", s.title); put("channel", s.channel)
            put("duration_sec", s.durationSec); put("thumb", s.thumbnailUrl)
        }
        d.insertWithOnConflict("songs", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun songs(sql: String, vararg args: String): List<Song> {
        val out = ArrayList<Song>()
        db.rawQuery(sql, args).use { c -> while (c.moveToNext()) out += c.song() }
        return out
    }

    private fun Cursor.song() = Song(getString(0), getString(1) ?: "", getString(2) ?: "", getInt(3), getString(4) ?: "")

    @Synchronized fun setFavorite(song: Song, fav: Boolean) {
        val d = db
        d.beginTransaction()
        try {
            if (fav) {
                upsertSong(d, song)
                d.insertWithOnConflict("favorites", null,
                    ContentValues().apply { put("video_id", song.videoId); put("added_at", System.currentTimeMillis()) },
                    SQLiteDatabase.CONFLICT_IGNORE)
            } else {
                d.delete("favorites", "video_id=?", arrayOf(song.videoId))
            }
            d.setTransactionSuccessful()
        } finally { d.endTransaction() }
    }

    fun isFavorite(videoId: String): Boolean =
        db.rawQuery("SELECT 1 FROM favorites WHERE video_id=?", arrayOf(videoId)).use { it.moveToFirst() }

    fun favoriteIds(): Set<String> {
        val out = HashSet<String>()
        db.rawQuery("SELECT video_id FROM favorites", null).use { c -> while (c.moveToNext()) out += c.getString(0) }
        return out
    }

    fun favorites(): List<Song> = songs(
        "SELECT s.video_id,s.title,s.channel,s.duration_sec,s.thumb FROM favorites f JOIN songs s USING(video_id) ORDER BY f.added_at DESC")

    @Synchronized fun recordPlayed(song: Song) {
        val d = db
        d.beginTransaction()
        try {
            upsertSong(d, song)
            d.insert("history", null, ContentValues().apply {
                put("video_id", song.videoId); put("played_at", System.currentTimeMillis())
            })
            d.setTransactionSuccessful()
        } finally { d.endTransaction() }
    }

    fun history(limit: Int = 50): List<Song> = songs(
        "SELECT s.video_id,s.title,s.channel,s.duration_sec,s.thumb FROM " +
            "(SELECT video_id, MAX(id) AS last FROM history GROUP BY video_id) h JOIN songs s USING(video_id) " +
            "ORDER BY h.last DESC LIMIT $limit")

    fun mostPlayed(limit: Int = 20): List<Song> = songs(
        "SELECT s.video_id,s.title,s.channel,s.duration_sec,s.thumb FROM " +
            "(SELECT video_id, COUNT(*) AS n, MAX(id) AS last FROM history GROUP BY video_id) h JOIN songs s USING(video_id) " +
            "ORDER BY h.n DESC, h.last DESC LIMIT $limit")

    fun savedTone(videoId: String): Int? =
        db.rawQuery("SELECT semitones FROM tone_prefs WHERE video_id=?", arrayOf(videoId)).use {
            if (it.moveToFirst()) it.getInt(0) else null
        }

    @Synchronized fun saveTone(videoId: String, semitones: Int) {
        if (semitones == 0) db.delete("tone_prefs", "video_id=?", arrayOf(videoId))
        else db.insertWithOnConflict("tone_prefs", null,
            ContentValues().apply { put("video_id", videoId); put("semitones", semitones) },
            SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized fun saveQueue(items: List<QueueItem>) {
        val d = db
        d.beginTransaction()
        try {
            d.delete("queue", null, null)
            items.forEachIndexed { i, q ->
                upsertSong(d, q.song)
                d.insert("queue", null, ContentValues().apply {
                    put("position", i); put("video_id", q.song.videoId); put("added_by", q.addedBy)
                })
            }
            d.setTransactionSuccessful()
        } finally { d.endTransaction() }
    }

    fun loadQueue(): List<QueueItem> {
        val out = ArrayList<QueueItem>()
        db.rawQuery(
            "SELECT s.video_id,s.title,s.channel,s.duration_sec,s.thumb,q.added_by FROM queue q JOIN songs s USING(video_id) ORDER BY q.position",
            null).use { c -> while (c.moveToNext()) out += QueueItem(c.song(), c.getString(5) ?: "TV") }
        return out
    }
}
