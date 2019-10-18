package ktx.sovereign.core.controller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import androidx.core.math.MathUtils.clamp
import ktx.sovereign.core.contract.TextToSpeechContract
import ktx.sovereign.database.provider.MediaProvider
import java.lang.ref.WeakReference

class TextToSpeechController(activity: Activity) : UtteranceProgressListener(),
        TextToSpeechContract.Holder {
    companion object {
        const val REQUEST_CHECK_TTS_DATA: Int = 0b1000
    }
    private val activityRef: WeakReference<Activity> = WeakReference(activity)
    private var engine: TextToSpeech? = null
    private var stateInfoListener: TextToSpeechContract.StateInfoListener? = null
    private var utteranceProgressListener: TextToSpeechContract.UtteranceProgressListener? = null

    override fun setStateInfoListener(listener: TextToSpeechContract.StateInfoListener) {
        this@TextToSpeechController.stateInfoListener = listener
    }
    override fun setStateInfoListener(impl: TextToSpeechContract.StateInfoListener_Impl.() -> Unit) {
        val sil = TextToSpeechContract.StateInfoListener_Impl()
        impl.invoke(sil)
        this@TextToSpeechController.stateInfoListener = sil
    }
    override fun setUtteranceProgressListener(listener: TextToSpeechContract.UtteranceProgressListener) {
        this@TextToSpeechController.utteranceProgressListener = listener
    }
    override fun setUtteranceProgressListener(impl: TextToSpeechContract.UtteranceProgressListener_Impl.() -> Unit) {
        val upl = TextToSpeechContract.UtteranceProgressListener_Impl()
        impl.invoke(upl)
        this@TextToSpeechController.utteranceProgressListener = upl
    }
    override fun check(code: Int) {
        activityRef.get()?.let {
            with (Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
                it.startActivityForResult(this, code)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_TTS_DATA -> {
                when (resultCode) {
                    TextToSpeech.Engine.CHECK_VOICE_DATA_PASS -> {
                        activityRef.get()?.let {
                            engine = TextToSpeech(it) { status ->
                                when (status) {
                                    TextToSpeech.SUCCESS -> {
                                        stateInfoListener?.onStateChanged(TextToSpeechContract.State.READY)
                                    }
                                    TextToSpeech.ERROR -> {
                                        stateInfoListener?.onStateChanged(TextToSpeechContract.State.DEAD)
                                    }
                                    else -> {
                                        stateInfoListener?.onStateChanged(TextToSpeechContract.State.INVALID)
                                    }
                                }
                            }.also { tts ->
                                tts.setOnUtteranceProgressListener(this@TextToSpeechController)
                            }
                        }
                    }
                    TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL -> {
                        activityRef.get()?.startActivity(
                                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        )
                    }
                }
            }
        }
    }
    override fun setPitch(pitch: Float): Int = engine?.setPitch(pitch) ?: TextToSpeech.ERROR
    override fun setSpeechRate(rate: Float): Int = engine?.setSpeechRate(
            clamp(rate, TextToSpeechContract.SpeechRate.SLOW, TextToSpeechContract.SpeechRate.FAST)
    ) ?: TextToSpeech.ERROR
    override fun setVoice(voice: Voice): Int = engine?.setVoice(voice) ?: TextToSpeech.ERROR
    override fun speak(text: String, mode: Int, params: Bundle?, utteranceId: String?) {
        engine?.speak(text.trim(), mode, params, utteranceId)
    }
    override fun synthesize(text: String, params: Bundle?, utteranceId: String?) {
        Log.i("TTS", "Max input size: ${TextToSpeech.getMaxSpeechInputLength()}\tInput size: ${text.length}")
        activityRef.get()?.let { context ->
            engine?.synthesizeToFile(text, params, MediaProvider.createAudioFile(context), utteranceId)
        }
    }
    override fun bulkSynthesize(parent: String, text: List<String>, params: Bundle?, utterancePrefix: String) {
        Log.i("TTS", "Max input size: ${TextToSpeech.getMaxSpeechInputLength()}")
        val chunk = mutableListOf<String>()
        val builder = StringBuilder()
        activityRef.get()?.let { context ->
            val dir = MediaProvider.getExternalAudioSubdirectory(context, parent)
            text.forEachIndexed { idx, line ->
                builder.append(line).append(" ")
                if (idx % 3 == 2) {
                    chunk.add(builder.toString().trim())
                    builder.clear()
                    builder.setLength(0)
                }
            }
            if (builder.isNotEmpty()) { chunk.add(builder.toString().trim()) }
            chunk.forEachIndexed { idx, line ->
                val name = "$utterancePrefix$idx-${Base64.encodeToString(line.toByteArray(), Base64.DEFAULT).substringBefore("=").take(6)}"
                engine?.synthesizeToFile(line, params,
                        MediaProvider.createFile(dir, name, ".wav"), name)
            }
        }
    }
    override fun destroy() {
        engine?.apply {
            stop()
            shutdown()
            stateInfoListener?.onStateChanged(TextToSpeechContract.State.DEAD)
        }
    }

    override fun onStart(utteranceId: String?) {
        utteranceProgressListener?.onUtteranceStart(utteranceId)
    }
    override fun onDone(utteranceId: String?) {
        utteranceProgressListener?.onUtteranceDone(utteranceId)
    }
    override fun onError(utteranceId: String?) {
        utteranceProgressListener?.onUtteranceError(utteranceId)
    }
}