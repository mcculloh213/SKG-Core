package ktx.sovereign.core.contract

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice

interface TextToSpeechContract {
    enum class State {
        INVALID, READY, DEAD
    }
    object Pitch {
        val DEFAULT: Float = 1.0f
    }
    object SpeechRate {
        val SLOW: Float = 0.5f
        val DEFAULT: Float = 1.0f
        val FAST: Float = 2.5f
    }
    interface Holder {
        fun setStateInfoListener(listener: StateInfoListener)
        fun setStateInfoListener(impl: StateInfoListener_Impl.() -> Unit)
        fun setUtteranceProgressListener(listener: UtteranceProgressListener)
        fun setUtteranceProgressListener(impl: UtteranceProgressListener_Impl.() -> Unit)
        fun check(code: Int)
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        fun setPitch(pitch: Float): Int
        fun setSpeechRate(rate: Float): Int
        fun setVoice(voice: Voice): Int
        fun speak(text: String, mode: Int = TextToSpeech.QUEUE_ADD, params: Bundle? = null, utteranceId: String? = null)
        fun synthesize(text: String, params: Bundle? = null, utteranceId: String? = null)
        fun bulkSynthesize(parent: String, text: List<String>, params: Bundle? = null, utterancePrefix: String = "blk_")
        fun destroy()
    }
    interface StateInfoListener {
        fun onStateChanged(state: State)
    }
    class StateInfoListener_Impl : StateInfoListener {
        var onStateChanged: ((state: State) -> Unit)? = null
        override fun onStateChanged(state: State) = onStateChanged?.invoke(state) ?: Unit
    }
    interface UtteranceProgressListener {
        fun onFirstUtterance(utteranceId: String?)
        fun onLastUtterance(utteranceId: String?)
        fun onUtteranceStart(utteranceId: String?)
        fun onUtteranceDone(utteranceId: String?)
        fun onUtteranceError(utteranceId: String?)
    }
    class UtteranceProgressListener_Impl : UtteranceProgressListener {
        var onFirst: ((String?) -> Unit)? = null
        var onLast: ((String?) -> Unit)? = null
        var onStart: ((String?) -> Unit)? = null
        var onDone: ((String?) -> Unit)? = null
        var onError: ((String?) -> Unit)? = null
        override fun onFirstUtterance(utteranceId: String?) = onFirst?.invoke(utteranceId) ?: Unit
        override fun onLastUtterance(utteranceId: String?) = onLast?.invoke(utteranceId) ?: Unit
        override fun onUtteranceStart(utteranceId: String?) = onStart?.invoke(utteranceId) ?: Unit
        override fun onUtteranceDone(utteranceId: String?) = onDone?.invoke(utteranceId) ?: Unit
        override fun onUtteranceError(utteranceId: String?) = onError?.invoke(utteranceId) ?: Unit
    }
}