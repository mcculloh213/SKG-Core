package ktx.sovereign.core.controller

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import ktx.sovereign.core.contract.MediaPlayerContract
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

class MediaPlayerHolder(context: Context) : MediaPlayerContract.Adapter, CoroutineScope {
    companion object {
        const val PLAYBACK_POSITION_REFRESH_INTERVAL_MS: Long = 1_000L
    }
    private val _job: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + _job

    private val contextRef: WeakReference<Context> = WeakReference(context.applicationContext)
    @Volatile
    private var player: MediaPlayer? = null
    private var listener: MediaPlayerContract.PlaybackInfoListener? = null

    override fun setPlaybackInfoListener(listener: MediaPlayerContract.PlaybackInfoListener) {
        this@MediaPlayerHolder.listener = listener
    }
    override fun setPlaybackInfoListener(impl: MediaPlayerContract.PlaybackInfoListener_Impl.() -> Unit) {
        val l = MediaPlayerContract.PlaybackInfoListener_Impl()
        impl.invoke(l)
        this@MediaPlayerHolder.listener = l
    }
    override fun load(uri: Uri) {
        launch {
            initialize()

            try {
                contextRef.get()?.let {
                    val pfd = it.contentResolver.openFileDescriptor(uri, "r")
                    player?.setDataSource(pfd?.fileDescriptor)
                }
            } catch (ex: Exception) {
                Log.e("Load", "Failed to load file descriptor")
                ex.printStackTrace()
            }

            try {
                player?.prepareAsync()
            } catch (ex: Exception) {
                Log.e("Prepare", "Failed to prepare Media Player")
                ex.printStackTrace()
            }
        }
    }
    override fun release() {
        _job.cancelChildren()
        player?.release()
        player = null
    }
    override fun isPlaying(): Boolean = player?.isPlaying ?: false
    override fun play() {
        player?.apply {
            if (!isPlaying) {
                start()
                listener?.onStateChanged(MediaPlayerContract.State.PLAYING)
            }
        }
    }
    override fun reset() {
        player?.let {
            it.reset()
            listener?.onStateChanged(MediaPlayerContract.State.RESET)
        }
    }
    override fun pause() {
        player?.apply {
            if (isPlaying) {
                pause()
                listener?.onStateChanged(MediaPlayerContract.State.PAUSED)
            }
        }
    }
    override fun seekTo(position: Int) {
        player?.let {
            it.seekTo(position)
            listener?.onPositionChanged(position)
        }
    }

    private fun initialize() {
        if (player == null) {
            player = MediaPlayer().also {
                it.setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "What: $what\tExtra: $extra")
                    false
                }
                it.setOnPreparedListener {
                    listener?.apply {
                        onStateChanged(MediaPlayerContract.State.READY)
                        onDurationChanged(it.duration)
                        onPositionChanged(0)
                    }
                }
                it.setOnCompletionListener {
                    listener?.apply {
                        it.reset()
                        onStateChanged(MediaPlayerContract.State.COMPLETED)
                        onPlaybackCompleted()
                    }
                }
            }
        }
    }
}