package vn.homekara.box

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.multidex.MultiDexApplication
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import vn.homekara.box.data.KaraokeDb
import vn.homekara.box.data.Prefs
import vn.homekara.box.data.SongStore
import vn.homekara.box.net.NewPipeDownloader
import vn.homekara.box.net.Tls
import vn.homekara.box.player.KaraokeSession
import vn.homekara.box.queue.SongQueue
import vn.homekara.box.remote.PhoneRemoteServer
import vn.homekara.box.youtube.NewPipeYoutube
import vn.homekara.box.youtube.RemoteYoutube
import vn.homekara.box.youtube.SongSearch
import vn.homekara.box.youtube.StreamResolver
import java.util.concurrent.Executors

class App : MultiDexApplication() {

    lateinit var prefs: Prefs
        private set
    lateinit var store: SongStore
        private set
    lateinit var queue: SongQueue
        private set
    val newPipe = NewPipeYoutube()
    val io = Executors.newFixedThreadPool(3)
    /** Ghi DB tuần tự để bản hàng chờ lưu sau cùng luôn là bản mới nhất. */
    val dbWriter = Executors.newSingleThreadExecutor()
    val main = Handler(Looper.getMainLooper())

    lateinit var session: KaraokeSession
        private set

    var phoneServer: PhoneRemoteServer? = null
        private set

    private var remoteCache: Pair<String, RemoteYoutube>? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        try { Tls.install(this) } catch (e: Throwable) { Log.e(TAG, "Không cài được TLS", e) }
        NewPipe.init(NewPipeDownloader(), Localization("vi", "VN"), ContentCountry("VN"))

        prefs = Prefs(this)
        store = SongStore(KaraokeDb(this))
        val saved = try { store.loadQueue() } catch (e: Exception) { emptyList() }
        queue = SongQueue(saved) { snap ->
            dbWriter.execute { try { store.saveQueue(snap) } catch (e: Exception) { Log.w(TAG, "Lưu hàng chờ lỗi", e) } }
        }
        session = KaraokeSession(this)

        try {
            phoneServer = PhoneRemoteServer(this, PORT).also { it.start(5_000, false) }
        } catch (e: Exception) {
            Log.e(TAG, "Không mở được cổng $PORT cho điện thoại", e)
        }
    }

    private fun remote(): RemoteYoutube? {
        val url = prefs.resolverServer
        if (url.isBlank()) return null
        remoteCache?.let { if (it.first == url) return it.second }
        return RemoteYoutube(url).also { remoteCache = url to it }
    }

    val search: SongSearch get() = remote() ?: newPipe
    val resolver: StreamResolver get() = remote() ?: newPipe

    fun suggest(prefix: String): List<String> = newPipe.suggest(prefix)

    companion object {
        const val TAG = "HomeKara"
        const val PORT = 8686
        lateinit var instance: App
            private set
    }
}
