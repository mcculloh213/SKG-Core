@file:JvmName("MediaPlayerState")
package ktx.sovereign.media

import android.media.MediaPlayer

/**
 * Media Player declared enum states, from mediaplayer.h
 *
 * ```
 * enum media_player_states {
 *     MEDIA_PLAYER_STATE_ERROR        = 0,
 *     MEDIA_PLAYER_IDLE               = 1 << 0,
 *     MEDIA_PLAYER_INITIALIZED        = 1 << 1,
 *     MEDIA_PLAYER_PREPARING          = 1 << 2,
 *     MEDIA_PLAYER_PREPARED           = 1 << 3,
 *     MEDIA_PLAYER_STARTED            = 1 << 4,
 *     MEDIA_PLAYER_PAUSED             = 1 << 5,
 *     MEDIA_PLAYER_STOPPED            = 1 << 6,
 *     MEDIA_PLAYER_PLAYBACK_COMPLETE  = 1 << 7
 * };
 * ```
 *
 * Additional [State] object [End] signifies that the [MediaPlayer.release] has been called, and
 * it can no longer be acted upon.
 */
sealed class State {
    abstract val code: Int
    object Error : State() {
        override val code: Int
            get() = 0
    }
    object Idle : State() {
        override val code: Int
            get() = 1
    }
    object Initialized : State() {
        override val code: Int
            get() = 2
    }
    object Preparing : State() {
        override val code: Int
            get() = 4
    }
    object Prepared : State() {
        override val code: Int
            get() = 8
    }
    object Started : State() {
        override val code: Int
            get() = 16
    }
    object Paused : State() {
        override val code: Int
            get() = 32
    }
    object Stopped : State() {
        override val code: Int
            get() = 64
    }
    object PlaybackComplete : State() {
        override val code: Int
            get() = 128
    }
    object End : State() {
        override val code: Int
            get() = 256
    }
}

/**
 * Media Player actions, as defined in the state diagram:
 * https://developer.android.com/reference/android/media/MediaPlayer
 */
sealed class Action {
    object Reset : Action()
    object SetDataSource : Action()
    object Prepare : Action()
    object PrepareAsync : Action()
    object Prepared : Action()
    object SeekTo : Action()
    object Start : Action()
    object Pause : Action()
    object Stop : Action()
    object Completed : Action()
    object Release : Action()
    object Error : Action()
}
sealed class Effect {

}
