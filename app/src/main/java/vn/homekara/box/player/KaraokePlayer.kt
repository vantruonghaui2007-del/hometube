package vn.homekara.box.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import vn.homekara.box.domain.Song
import vn.homekara.box.net.Http
import vn.homekara.box.youtube.PlayableStreams
import vn.homekara.box.youtube.StreamResolver
import vn.homekara.box.youtube.UnplayableException
import java.util.concurrent.Executors

/**
 * Bọc ExoPlayer cho karaoke: lấy luồng ngay trước khi phát, đổi tone (đổi cao độ, giữ tốc độ),
 * tắt lời ca sĩ, tự lấy lại link khi gặp HTTP 403 (link hết hạn) đúng một lần.
 * Mọi hàm public gọi trên main thread.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class KaraokePlayer(
    context: Context,
    private val resolverProvider: () -> StreamResolver,
    private val maxHeight: () -> Int,
) {
    interface Listener {
        fun onPreparing(song: Song) {}
        fun onStarted(song: Song) {}
        fun onEnded(song: Song) {}
        fun onUnplayable(song: Song, reason: String) {}
        fun onPauseChanged(paused: Boolean) {}
    }

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    val vocalCut = VocalCutAudioProcessor()

    private val renderers = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(vocalCut))
            .build()
    }.setEnableDecoderFallback(true)

    val exo: ExoPlayer = ExoPlayer.Builder(context, renderers)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 2_500, 5_000)
                .build())
        .build()

    private val dataSourceFactory = ResolvingDataSource.Factory(
        DefaultHttpDataSource.Factory()
            .setUserAgent(Http.DESKTOP_UA)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
    ) { spec -> YoutubeRequests.transform(spec) }

    var listener: Listener? = null
    var current: Song? = null
        private set

    var semitones: Int = 0
        set(value) {
            field = Tone.clamp(value)
            exo.playbackParameters = PlaybackParameters(1f, Tone.pitchFactor(field))
        }

    private var requestId = 0
    private var retriedThisSong = false
    private val readiness = vn.hometube.app.core.LoadReadiness()

    init {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val song = current ?: return
                if (state == Player.STATE_READY && readiness.ready()) {
                    listener?.onStarted(song)
                }
                if (state == Player.STATE_ENDED) {
                    current = null
                    listener?.onEnded(song)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener?.onPauseChanged(!exo.playWhenReady)
            }

            override fun onPlayerError(error: PlaybackException) {
                val song = current ?: return
                Log.w(TAG, "Lỗi phát ${song.videoId}", error)
                if (is403(error) && !retriedThisSong) {
                    retriedThisSong = true
                    resolverProvider().invalidate(song.videoId)
                    val pos = exo.currentPosition
                    load(song, pos, keepRetryFlag = true)
                } else {
                    current = null
                    listener?.onUnplayable(song, if (is403(error)) "Không tải được video (403)" else "Lỗi phát video")
                }
            }
        })
    }

    fun attach(surface: SurfaceView) = exo.setVideoSurfaceView(surface)

    /** Phát bài; [startSemitones] là tone áp ngay từ đầu. */
    fun play(song: Song, startSemitones: Int, startPositionMs: Long = 0L) {
        semitones = startSemitones
        retriedThisSong = false
        load(song, startPositionMs, keepRetryFlag = false)
    }

    private fun load(song: Song, positionMs: Long, keepRetryFlag: Boolean) {
        val id = ++requestId
        current = song
        readiness.loading()
        exo.stop()
        listener?.onPreparing(song)
        worker.execute {
            val result: Any = try {
                val candidates = resolverProvider().resolve(song.videoId)
                StreamSelector.select(candidates, maxHeight())
                    ?: UnplayableException("Video không có định dạng phù hợp với box")
            } catch (e: UnplayableException) {
                e
            } catch (e: Exception) {
                Log.w(TAG, "Không lấy được luồng ${song.videoId}", e)
                UnplayableException("Không lấy được video từ YouTube", e)
            }
            main.post {
                if (id != requestId) return@post   // đã có yêu cầu mới hơn
                when (result) {
                    is PlayableStreams -> start(result, positionMs)
                    is UnplayableException -> { current = null; listener?.onUnplayable(song, result.reason) }
                }
            }
        }
    }

    private fun start(streams: PlayableStreams, positionMs: Long) {
        val source: MediaSource = when (streams) {
            is PlayableStreams.Split -> MergingMediaSource(
                progressive(streams.videoUrl), progressive(streams.audioUrl))
            is PlayableStreams.Progressive -> progressive(streams.url)
        }
        exo.setMediaSource(source, positionMs)
        exo.playbackParameters = PlaybackParameters(1f, Tone.pitchFactor(semitones))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun progressive(url: String): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(url))

    fun toneUp(): Int { semitones += 1; return semitones }
    fun toneDown(): Int { semitones -= 1; return semitones }
    fun resetTone(): Int { semitones = 0; return semitones }

    fun togglePause(): Boolean {
        exo.playWhenReady = !exo.playWhenReady
        return !exo.playWhenReady
    }

    val isPaused: Boolean get() = !exo.playWhenReady

    fun seekBy(deltaMs: Long) {
        val d = exo.duration
        var p = exo.currentPosition + deltaMs
        if (p < 0) p = 0
        if (d > 0 && p > d - 1000) p = d - 1000
        exo.seekTo(p)
    }

    fun stop() {
        requestId++
        current = null
        exo.stop()
    }

    fun release() {
        requestId++
        current = null
        exo.release()
        worker.shutdownNow()
    }

    private fun is403(error: Throwable): Boolean {
        var t: Throwable? = error
        while (t != null) {
            if (t is HttpDataSource.InvalidResponseCodeException && t.responseCode == 403) return true
            t = t.cause
        }
        return false
    }

    companion object { private const val TAG = "HomeKara/Player" }
}
