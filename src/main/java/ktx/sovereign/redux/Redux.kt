@file:JvmName("Redux")
package ktx.sovereign.redux

interface State
interface Action
typealias Reducer <S> = (S, Action) -> S
typealias StoreSubscriber <S> = (S) -> Unit
interface Store <S : State> {
    fun dispatch(action: Action)
    fun add(subscriber: StoreSubscriber<S>): Boolean
    fun remove(subscriber: StoreSubscriber<S>): Boolean
}
abstract class AbstractStore<S: State>(
        initialState: S,
        private val reducer: Reducer<S>
) : Store<S> {
    val state: S
        get() = _state
    private var _state: S = initialState
        set(value) {
            field = value
            subscribers.forEach { it(value) }
        }
    private val subscribers = mutableSetOf<StoreSubscriber<S>>()
    override fun dispatch(action: Action) { _state = reducer(_state, action) }
    override fun add(subscriber: StoreSubscriber<S>) = subscribers.add(subscriber)
    override fun remove(subscriber: StoreSubscriber<S>) = subscribers.remove(subscriber)
}