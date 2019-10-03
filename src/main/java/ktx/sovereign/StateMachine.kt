package ktx.sovereign

import java.util.concurrent.atomic.AtomicReference

typealias StateAction<S, E> = (S, E) -> Unit
typealias Transaction<S, A, E> = (StateMachine.Transition<S, A, E>) -> Unit
typealias ActionMatcher<In, Out> = StateMachine.Matcher<In, Out>
typealias StateTransition<S, A, E> = (S, A) -> StateMachine.Graph.State.TransitionTo<S, E>
/**
 * Finite State Machine implementation.
 * S: State
 * A: Action
 * E: Effect
 */
class StateMachine<S: Any, A: Any, E: Any> private constructor(
        private val graph: Graph<S, A, E>
) {
    private val _state: AtomicReference<S> = AtomicReference(graph.initialState)
    val state: S
        get() = _state.get()

    fun transition(action: A): Transition<S, A, E> {
        val t = synchronized(this) {
            val from = _state.get()
            val transition = from.getTransition(action)
            when (transition) {
                is Transition.Valid -> _state.set(transition.toState)
            }
            transition
        }
        t.notifyOnTransition()
        when (t) {
            is Transition.Valid -> {
                t.apply {
                    with(fromState) { notifyOnExit(action) }
                    with(toState) { notifyOnEntry(action) }
                }
            }
        }
        return t
    }
    fun with(init: GraphBuilder<S, A, E>.() -> Unit): StateMachine<S, A, E> = create(graph.copy(initialState = state), init)
    private fun S.getTransition(action: A): Transition<S, A, E> {
        for ((matcher, to) in getDefinitions().transitions) {
            if (matcher.matches(action)) {
                val (toState, effect) = to(this, action)
                return Transition.Valid(this, action, toState, effect)
            }
        }
        return Transition.Invalid(this, action)
    }
    private fun S.getDefinitions() = graph.stateDefinitions
            .filter { it.key.matches(this) }
            .map { it.value }
            .firstOrNull() ?: error("Missing definition for state ${this.javaClass.simpleName}!")
    private fun S.notifyOnEntry(cause: A) =
            getDefinitions().onEntryListeners.forEach { it(this, cause) }
    private fun S.notifyOnExit(cause: A) =
            getDefinitions().onExitListeners.forEach { it(this, cause) }
    private fun Transition<S, A, E>.notifyOnTransition() =
            graph.notifyOnTransition.forEach { it(this) }
    sealed class Transition<out S: Any, out A: Any, out E: Any> {
        abstract val fromState: S
        abstract val action: A
        data class Valid<out S: Any, out A: Any, out E: Any> internal constructor(
                override val fromState: S,
                override val action: A,
                val toState: S,
                val effect: E?
        ) : Transition<S, A, E>()
        data class Invalid<out S: Any, out A: Any, out E: Any> internal constructor(
                override val fromState: S,
                override val action: A
        ) : Transition<S, A, E>()
    }
    data class Graph<S : Any, A: Any, E: Any>(
            val initialState: S,
            val stateDefinitions: Map<Matcher<S, S>, State<S, A, E>>,
            val notifyOnTransition: List<Transaction<S, A, E>>
    ) {
        class State<S: Any, A: Any, E: Any> internal constructor() {
            val onEntryListeners: MutableList<StateAction<S, A>> = mutableListOf()
            val onExitListeners: MutableList<StateAction<S, A>> = mutableListOf()
            val transitions: LinkedHashMap<ActionMatcher<A, A>, StateTransition<S, A, E>> = linkedMapOf()
            data class TransitionTo<out S: Any, out E: Any> internal constructor(
                    val toState: S,
                    val effect: E?
            )
        }
    }
    class Matcher<T : Any, out R: T> private constructor(private val clazz: Class<R>) {
        private val predicates = mutableListOf<(T) -> Boolean>({ clazz.isInstance(it) })
        fun where(predicate: R.() -> Boolean): Matcher<T, R> = apply {
            predicates.add {
                @Suppress("UNCHECKED_CAST")
                (it as R).predicate()
            }
        }
        fun matches(value: T) = predicates.all { it(value) }
        companion object {
            fun <T: Any, R: T> any(clazz: Class<R>): Matcher<T, R> = Matcher(clazz)
            inline fun <T: Any, reified R: T> any(): Matcher<T, R> = any(R::class.java)
            inline fun <T: Any, reified R: T> eq(value: R): Matcher<T, R> = any<T, R>().where { this == value }
        }
    }
    class GraphBuilder<S: Any, A: Any, E: Any>(graph: Graph<S, A, E>? = null) {
        private var initialState = graph?.initialState
        private val stateDefinitions = LinkedHashMap(graph?.stateDefinitions ?: emptyMap())
        private val onTransitionListeners = ArrayList(graph?.notifyOnTransition ?: emptyList())

        fun setInitialState(state: S) {
            initialState = state
        }
        fun <State: S> state(matcher: Matcher<S, State>, init: StateDefinitionBuilder<State>.() -> Unit) {
            stateDefinitions[matcher] = StateDefinitionBuilder<State>().apply(init).build()
        }
        inline fun <reified State: S> state(noinline init: StateDefinitionBuilder<State>.() -> Unit) =
                state(Matcher.any(), init)
        inline fun <reified State: S> state(state: State, noinline init: StateDefinitionBuilder<State>.() -> Unit) =
                state(Matcher.eq<S, State>(state), init)
        fun onTransition(listener: Transaction<S, A, E>) =
                onTransitionListeners.add(listener)
        fun build(): Graph<S, A, E> =
                Graph(requireNotNull(initialState), stateDefinitions.toMap(), onTransitionListeners.toList())
        inner class StateDefinitionBuilder<State: S> {
            private val stateDefinition = Graph.State<S, A, E>()
            inline fun <reified Action: A> any(): Matcher<A, Action> = Matcher.any()
            inline fun <reified Action: A> eq(value: Action): Matcher<A, Action> = Matcher.eq(value)
            fun <Action: A> on(matcher: Matcher<A, Action>, to: State.(Action) -> Graph.State.TransitionTo<S, E>) {
                stateDefinition.transitions[matcher] = { state, action ->
                    @Suppress("UNCHECKED_CAST")
                    to((state as State), (action as Action))
                }
            }
            inline fun <reified Action: A> on(
                    noinline  to: State.(Action) -> Graph.State.TransitionTo<S, E>
            ) = on(any(), to)
            inline fun <reified Action: A> on(
                    action: Action, noinline to: State.(Action) -> Graph.State.TransitionTo<S, E>
            ) = on(eq(action), to)
            fun onEnter(listener: State.(A) -> Unit) = with (stateDefinition) {
                onEntryListeners.add { state, cause ->
                    @Suppress("UNCHECKED_CAST")
                    listener((state as State), cause)
                }
            }
            fun onExit(listener: State.(A) -> Unit) = with (stateDefinition) {
                onEntryListeners.add { state, cause ->
                    @Suppress("UNCHECKED_CAST")
                    listener((state as State), cause)
                }
            }
            fun build() = stateDefinition
            fun State.transitionTo(state: S, effect: E? = null) =
                    Graph.State.TransitionTo(state, effect)
            fun State.dontTransition(effect: E? = null) =
                    transitionTo(this, effect)
        }
    }
    companion object {
        fun <S: Any, A: Any, E: Any> create (
                init: GraphBuilder<S, A, E>.() -> Unit
        ): StateMachine<S, A, E> = create(null, init)
        private fun <S: Any, A: Any, E: Any> create (
                graph: Graph<S, A, E>?,
                init: GraphBuilder<S, A, E>.() -> Unit
        ): StateMachine<S, A, E> = StateMachine(GraphBuilder(graph).apply(init).build())
    }
}