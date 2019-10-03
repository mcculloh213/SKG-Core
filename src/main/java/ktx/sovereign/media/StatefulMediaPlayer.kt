package ktx.sovereign.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ktx.sovereign.StateMachine

class StatefulMediaPlayer(
        context: Context
) : StatefulMediaPlayerAdapter, CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val context = context.applicationContext
    private lateinit var player: MediaPlayer

    override val isPlaying: Boolean
        get() = if (::player.isInitialized) { player.isPlaying } else { false }
    override fun initializeProgressCallback() {}
    override fun load(uri: Uri) {
        launch {
            create()
            when (val t = stateMachine.transition(Action.SetDataSource)) {
                is StateMachine.Transition.Valid -> {
                    try {
                        with (context) {
                            val pfd = contentResolver.openFileDescriptor(uri, "r")
                            player.setDataSource(pfd?.fileDescriptor)
                        }
                    } catch(ex: Exception) {
                        Log.e("SetDataSource", "Failed to load file descriptor")
                        ex.printStackTrace()
                        stateMachine.transition(Action.Error)
                    }
                }
            }
        }
    }
    override fun seekTo(position: Int) {}
    override fun play() {}
    override fun pause() {}
    override fun release() {}

    private fun create() {
        if (!::player.isInitialized) {
            player = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "What: $what\tExtra: $extra")
                    false
                }
                setOnPreparedListener {
                    //TODO: Notify ready
                }
                setOnCompletionListener {
                    when (val t = stateMachine.transition(Action.Completed)) {
                        is StateMachine.Transition.Valid -> { }
                        else -> { }
                    }
                }
            }
        }
    }

    private val stateMachine = StateMachine.create<State, Action, Effect> {
        setInitialState(State.Idle)
        state<State.Idle> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.SetDataSource> {
                transitionTo(State.Initialized)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Initialized> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.PrepareAsync> {
                transitionTo(State.Preparing)
            }
            on<Action.Prepare> {
                transitionTo(State.Prepared)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Preparing> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.Prepared> {
                transitionTo(State.Prepared)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Prepared> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.SeekTo> {
                transitionTo(State.Prepared)
            }
            on<Action.Start> {
                transitionTo(State.Started)
            }
            on<Action.Stop> {
                transitionTo(State.Stopped)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Started> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.SeekTo> {
                transitionTo(State.Started)
            }
            on<Action.Start> {
                transitionTo(State.Started)
            }
            on<Action.Pause> {
                transitionTo(State.Paused)
            }
            on<Action.Stop> {
                transitionTo(State.Stopped)
            }
            on<Action.Completed> {
                transitionTo(State.PlaybackComplete)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Paused> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.SeekTo> {
                transitionTo(State.Paused)
            }
            on<Action.Start> {
                transitionTo(State.Started)
            }
            on<Action.Pause> {
                transitionTo(State.Paused)
            }
            on<Action.Stop> {
                transitionTo(State.Stopped)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Stopped> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.PrepareAsync> {
                transitionTo(State.Preparing)
            }
            on<Action.Prepare> {
                transitionTo(State.Prepared)
            }
            on<Action.Stop> {
                transitionTo(State.Stopped)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.PlaybackComplete> {
            on<Action.Reset> {
                transitionTo(State.Idle)
            }
            on<Action.SeekTo> {
                transitionTo(State.PlaybackComplete)
            }
            on<Action.Start> {
                transitionTo(State.Started)
            }
            on<Action.Stop> {
                transitionTo(State.Stopped)
            }
            on<Action.Release> {
                transitionTo(State.End)
            }
            on<Action.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.End> {}
        state<State.Error> {}
    }
}
interface StatefulMediaPlayerAdapter {
    val isPlaying: Boolean
    fun initializeProgressCallback()
    fun load(uri: Uri)
    fun seekTo(position: Int)
    fun play()
    fun pause()
    fun release()
}