package vn.hometube.app.core

import org.json.JSONArray

data class Segment(val start:Long,val end:Long)
object Sponsors {
    fun parse(json:String):List<Segment> = try {
        val a=JSONArray(json);(0 until a.length()).mapNotNull {i->
            val o=a.optJSONObject(i)?:return@mapNotNull null
            if(o.optString("actionType","skip")!="skip")return@mapNotNull null
            val p=o.optJSONArray("segment")?:return@mapNotNull null
            val s=p.optDouble(0,Double.NaN);val e=p.optDouble(1,Double.NaN)
            if(!s.isFinite()||!e.isFinite()||s<0||e<=s||e>86400*7)null else Segment((s*1000).toLong(),(e*1000).toLong())
        }.sortedBy {it.start}
    }catch(e:Exception){emptyList()}
}
class Skipper(private val segments:List<Segment>) {
    private val skipped=HashSet<Segment>()
    fun check(position:Long):Long? {
        val s=segments.firstOrNull {position>=it.start&&position<it.end&&it !in skipped}?:return null
        skipped+=s;return s.end
    }
    fun userSeek(position:Long){segments.filter {position>=it.start&&position<it.end}.forEach {skipped+=it}}
}
