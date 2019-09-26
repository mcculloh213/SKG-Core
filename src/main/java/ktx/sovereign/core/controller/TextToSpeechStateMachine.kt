package ktx.sovereign.core.controller

import ktx.sovereign.core.contract.TextToSpeechContract
import ktx.sovereign.redux.AbstractStore
import ktx.sovereign.redux.Action
import ktx.sovereign.redux.Reducer
import ktx.sovereign.redux.State

data class TextToSpeechState(
        val state: TextToSpeechContract.State = TextToSpeechContract.State.INVALID,
        val pitch: Float = TextToSpeechContract.Pitch.DEFAULT,
        val rate: Float = TextToSpeechContract.SpeechRate.DEFAULT,
        val utterance: String? = null
) : State
sealed class TextToSpeechActions: Action {
    data class SetState(val state: TextToSpeechContract.State) : TextToSpeechActions()
    data class Transform(
            val pitch: Float = TextToSpeechContract.Pitch.DEFAULT,
            val rate: Float = TextToSpeechContract.SpeechRate.DEFAULT
    ) : TextToSpeechActions()
}
val TextToSpeechReducer: Reducer<TextToSpeechState> = { old, action ->
    when (action) {
        is TextToSpeechActions.SetState -> old.copy(state = action.state)
        is TextToSpeechActions.Transform -> old.copy(
                pitch = action.pitch,
                rate = action.rate
        )
        else -> old
    }
}
class TextToSpeechStore : AbstractStore<TextToSpeechState>(
        initialState = TextToSpeechState(),
        reducer = TextToSpeechReducer
)
