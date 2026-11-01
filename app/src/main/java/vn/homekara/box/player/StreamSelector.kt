package vn.homekara.box.player

import vn.homekara.box.youtube.PlayableStreams
import vn.homekara.box.youtube.StreamCandidate
import vn.homekara.box.youtube.StreamCandidate.Kind

/**
 * Chọn luồng hợp với Amlogic S805: chỉ H.264 (avc1) — VP9/AV1 không có giải mã phần cứng —
 * và âm thanh AAC (mp4a).
 */
object StreamSelector {
    fun select(candidates: List<StreamCandidate>, maxHeight: Int): PlayableStreams? {
        val video = candidates
            .filter { it.kind == Kind.VIDEO_ONLY && isAvc(it.codec) && it.height in 1..maxHeight }
            .maxWithOrNull(compareBy<StreamCandidate> { it.height }.thenBy { it.bitrate })
        val audio = candidates
            .filter { it.kind == Kind.AUDIO_ONLY && it.codec.startsWith("mp4a", ignoreCase = true) }
            .maxByOrNull { it.bitrate }
        if (video != null && audio != null) return PlayableStreams.Split(video.url, audio.url)

        val progressive = candidates
            .filter { it.kind == Kind.PROGRESSIVE && isAvc(it.codec) && it.height <= maxHeight }
            .maxWithOrNull(compareBy<StreamCandidate> { it.height }.thenBy { it.bitrate })
        return progressive?.let { PlayableStreams.Progressive(it.url) }
    }

    private fun isAvc(codec: String) = codec.startsWith("avc1", ignoreCase = true)
}
