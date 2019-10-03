package ktx.sovereign.core.contract

import android.net.Uri

interface MediaPlayerContract {
    enum class State {
        INVALID, READY, PLAYING, PAUSED, RESET, COMPLETED
    }
    interface Adapter {
        fun setPlaybackInfoListener(listener: PlaybackInfoListener)
        fun setPlaybackInfoListener(impl: PlaybackInfoListener_Impl.() -> Unit)
        fun load(uri: Uri)
        fun release()
        fun isPlaying(): Boolean
        fun play()
        fun reset()
        fun pause()
        fun stop()
        fun seekTo(position: Int)
    }
    interface PlaybackInfoListener {
        fun onDurationChanged(duration: Int)
        fun onPositionChanged(position: Int)
        fun onStateChanged(state: State)
        fun onPlaybackCompleted()
    }
    class PlaybackInfoListener_Impl : PlaybackInfoListener {
        var onDurationChanged: ((duration: Int) -> Unit)? = null
        var onPositionChanged: ((position: Int) -> Unit)? = null
        var onStateChanged: ((state: State) -> Unit)? = null
        var onPlaybackCompleted: (() -> Unit)? = null

        override fun onDurationChanged(duration: Int) {
            onDurationChanged?.invoke(duration)
        }
        override fun onPositionChanged(position: Int) {
            onPositionChanged?.invoke(position)
        }
        override fun onStateChanged(state: State) {
            onStateChanged?.invoke(state)
        }
        override fun onPlaybackCompleted() {
            onPlaybackCompleted?.invoke()
        }
    }
}