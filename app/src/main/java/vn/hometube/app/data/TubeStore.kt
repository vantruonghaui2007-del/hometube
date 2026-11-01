package vn.hometube.app.data

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import vn.hometube.app.core.*

class TubeStore(context:Context) {
    private val db=object:SQLiteOpenHelper(context,"hometube.db",null,1){
        override fun onCreate(d:SQLiteDatabase){
            d.execSQL("CREATE TABLE videos(id TEXT PRIMARY KEY, json TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0, duration INTEGER NOT NULL DEFAULT 0, seen INTEGER NOT NULL DEFAULT 0, saved INTEGER NOT NULL DEFAULT 0)")
            d.execSQL("CREATE TABLE channels(url TEXT PRIMARY KEY, json TEXT NOT NULL)")
            d.execSQL("CREATE INDEX video_seen ON videos(seen)")
        }
        override fun onUpgrade(d:SQLiteDatabase,old:Int,new:Int){}
    }
    fun savePosition(v:Video,position:Long,duration:Long){
        val d=db.writableDatabase;d.beginTransaction()
        try {ensure(d,v);d.update("videos",ContentValues().apply {put("json",encode(v).toString());put("position",position.coerceAtLeast(0));put("duration",duration.coerceAtLeast(0));put("seen",System.currentTimeMillis())},"id=?",arrayOf(v.id));d.setTransactionSuccessful()}finally{d.endTransaction()}
    }
    fun position(id:String):Long=db.readableDatabase.rawQuery("SELECT position,duration FROM videos WHERE id=?",arrayOf(id)).use {if(it.moveToFirst())Resume.start(it.getLong(0),it.getLong(1))else 0}
    fun saved(id:String):Boolean=db.readableDatabase.rawQuery("SELECT saved FROM videos WHERE id=?",arrayOf(id)).use {it.moveToFirst()&&it.getLong(0)>0}
    fun setSaved(v:Video,yes:Boolean){val d=db.writableDatabase;ensure(d,v);d.update("videos",ContentValues().apply {put("saved",if(yes)System.currentTimeMillis()else 0)},"id=?",arrayOf(v.id))}
    fun entries(section:String):List<Entry> {
        val clause=when(section){"later"->"saved>0 ORDER BY saved DESC";"continue"->"seen>0 AND position>0 AND (duration<=0 OR position<duration-MIN(30000,duration/10)) ORDER BY seen DESC";else->"seen>0 ORDER BY seen DESC"}
        return db.readableDatabase.rawQuery("SELECT json,position,duration FROM videos WHERE $clause LIMIT 200",null).use {c->buildList {while(c.moveToNext())runCatching {add(Entry.Clip(decode(JSONObject(c.getString(0))),Resume.progress(c.getLong(1),c.getLong(2))))}}}
    }
    fun watched():Set<String> = db.readableDatabase.rawQuery("SELECT id FROM videos WHERE seen>0",null).use {c->buildSet {while(c.moveToNext())add(c.getString(0))}}
    fun subscriptions():List<Channel> = db.readableDatabase.rawQuery("SELECT json FROM channels ORDER BY url",null).use {c->buildList {while(c.moveToNext())runCatching {val j=JSONObject(c.getString(0));add(Channel(j.getString("url"),j.getString("name"),j.optString("avatar"),j.optLong("subscribers",-1)))}}}
    fun subscribed(url:String):Boolean=db.readableDatabase.rawQuery("SELECT 1 FROM channels WHERE url=?",arrayOf(url)).use {it.moveToFirst()}
    fun subscribe(ch:Channel,yes:Boolean){
        if(yes)db.writableDatabase.insertWithOnConflict("channels",null,ContentValues().apply {put("url",ch.url);put("json",JSONObject().put("url",ch.url).put("name",ch.name).put("avatar",ch.avatar).put("subscribers",ch.subscribers).toString())},SQLiteDatabase.CONFLICT_REPLACE)
        else db.writableDatabase.delete("channels","url=?",arrayOf(ch.url))
    }
    fun clearHistory(){db.writableDatabase.execSQL("UPDATE videos SET seen=0,position=0,duration=0");db.writableDatabase.delete("videos","saved=0",null)}
    fun check():Boolean = db.readableDatabase.rawQuery("PRAGMA quick_check",null).use {it.moveToFirst()&&it.getString(0)=="ok"}
    private fun ensure(d:SQLiteDatabase,v:Video){d.insertWithOnConflict("videos",null,ContentValues().apply {put("id",v.id);put("json",encode(v).toString())},SQLiteDatabase.CONFLICT_IGNORE)}
    companion object {
        fun encode(v:Video):JSONObject=JSONObject().put("id",v.id).put("title",v.title).put("channel",v.channel).put("channelUrl",v.channelUrl).put("duration",v.duration).put("thumb",v.thumb).put("views",v.views).put("uploaded",v.uploaded)
        fun decode(j:JSONObject)=Video(j.getString("id"),j.optString("title"),j.optString("channel"),j.optString("channelUrl"),j.optInt("duration"),j.optString("thumb"),j.optLong("views",-1),j.optString("uploaded"))
    }
}
