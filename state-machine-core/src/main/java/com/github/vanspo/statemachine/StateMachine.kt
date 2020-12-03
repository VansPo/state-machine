package com.github.vanspo.statemachine

import com.github.vanspo.statemachine.logger.DefaultLogger
import com.github.vanspo.statemachine.logger.Logger
import com.github.vanspo.statemachine.registry.DefaultStateRegistry
import com.github.vanspo.statemachine.registry.Registry
import com.github.vanspo.statemachine.relay.DefaultTransitionRelay
import com.github.vanspo.statemachine.relay.TransitionRelay
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

class StateMachine<STATE : Any, EVENT : Any, SIDE_EFFECT : Any> internal constructor(
        initialState: STATE,
        private val registry: Registry<STATE> = DefaultStateRegistry(),
        private val logger: Logger = DefaultLogger(),
        private val stateGraph: StateGraph<STATE, EVENT, SIDE_EFFECT>
) {
    private val transitionObservers: CopyOnWriteArrayList<TransitionObserver<STATE, SIDE_EFFECT>> =
            CopyOnWriteArrayList()
    private val messageQueue: MessageQueue = MessageQueue()

    val state: STATE
        get() = registry.state

    init {
        registry.updateState(initialState)
    }

    fun postEvent(event: EVENT) = synchronized(this) {
        messageQueue.post { onEvent(stateGraph, event) }
    }

    fun observe(
            transitionRelay: TransitionRelay = DefaultTransitionRelay(),
            block: TransitionObserver<STATE, SIDE_EFFECT>.() -> Unit
    ): Subscription = TransitionObserver<STATE, SIDE_EFFECT>(transitionRelay).let { observer ->
        block(observer)
        transitionObservers.add(observer)
        // Push latest state
        observer.onEnterState(registry.state)
        object : Subscription {
            override fun unsubscribe() {
                transitionObservers.remove(observer)
                transitionRelay.clear()
            }
        }
    }

    private fun onEvent(
            graph: StateGraph<STATE, EVENT, SIDE_EFFECT>,
            event: EVENT
    ) {
        messageQueue.stop()
        logger.log("Event ${registry.state::class.simpleName}:${event::class.java.simpleName}")
        val transition = graph.findCurrentStateDefinition()
                ?.reducers
                ?.get(event::class)
                ?.invoke(event, registry.state)
        if (transition == null) {
            logger.log("State ${registry.state::class.simpleName}: no transition found for event ${event::class.simpleName}")
            messageQueue.start()
            return
        }
        val oldState = registry.state
        if (oldState == transition.newState) {
            logger.log("Staying in a current state $oldState")
        } else {
            logger.log(
                    "State transition ${oldState::class.java.simpleName}->" +
                            transition.newState::class.java.simpleName
            )
            registry.updateState(transition.newState)
            transitionObservers.forEach { it.onExitState(oldState) }
            transitionObservers.forEach { it.onEnterState(registry.state) }
        }
        transition.sideEffect?.run {
            logger.log("Effect ${registry.state::class.simpleName}:${this::class.java.simpleName}")
            transitionObservers.forEach { it.onSideEffect(this) }
        }
        messageQueue.start()
    }

    @Suppress("UNCHECKED_CAST")
    private fun StateGraph<STATE, EVENT, SIDE_EFFECT>.findCurrentStateDefinition(): StateDefinition<STATE, STATE, EVENT, SIDE_EFFECT>? =
            stateDefinitionMap[registry.state::class] as StateDefinition<STATE, STATE, EVENT, SIDE_EFFECT>?
}

class StateGraph<STATE : Any, EVENTS : Any, SIDE_EFFECT : Any> {
    internal val stateDefinitionMap: MutableMap<KClass<out STATE>, StateDefinition<STATE, out STATE, EVENTS, SIDE_EFFECT>> =
            mutableMapOf()

    inline fun <reified S : STATE> state(
            noinline block: StateDefinition<STATE, S, EVENTS, SIDE_EFFECT>.() -> Unit
    ) = state(S::class, block)

    fun <S : STATE> state(
            clazz: KClass<S>,
            block: StateDefinition<STATE, S, EVENTS, SIDE_EFFECT>.() -> Unit
    ) {
        val definition = StateDefinition<STATE, S, EVENTS, SIDE_EFFECT>().apply(block)
        stateDefinitionMap[clazz] = definition
    }
}

class StateDefinition<TRANSITION : Any, STATE : TRANSITION, EVENT : Any, SIDE_EFFECT : Any> {
    internal var reducers: MutableMap<KClass<out EVENT>, Reducer<TRANSITION, STATE, EVENT, SIDE_EFFECT>> =
            mutableMapOf()

    inline fun <reified E : EVENT> onEvent(
            noinline reducer: Reducer<TRANSITION, STATE, EVENT, SIDE_EFFECT>.(event: E, oldState: STATE) -> Transition<TRANSITION, SIDE_EFFECT>
    ) = onEvent(E::class, reducer)

    fun <E : EVENT> onEvent(
            clazz: KClass<E>,
            reducer: Reducer<TRANSITION, STATE, EVENT, SIDE_EFFECT>.(event: E, oldState: STATE) -> Transition<TRANSITION, SIDE_EFFECT>
    ) {
        @Suppress("UNCHECKED_CAST")
        val castedReducer =
                reducer as Reducer<TRANSITION, STATE, EVENT, SIDE_EFFECT>.(EVENT, STATE) -> Transition<TRANSITION, SIDE_EFFECT>
        reducers[clazz] = Reducer(castedReducer)
    }
}

data class Reducer<T : Any, S : T, EV : Any, SE : Any>(
        internal var function: Reducer<T, S, EV, SE>.(event: EV, oldState: S) -> Transition<T, SE>
) {
    fun transitionTo(
            newState: T,
            sideEffect: SE? = null
    ): Transition<T, SE> = Transition(newState, sideEffect)

    internal fun invoke(event: EV, oldState: S) = function.invoke(this, event, oldState)
}

data class Transition<STATE : Any, SIDE_EFFECT : Any>(
        val newState: STATE,
        val sideEffect: SIDE_EFFECT? = null
)

class TransitionObserver<STATE : Any, SIDE_EFFECT : Any>(
        private val transitionRelay: TransitionRelay
) {
    private var eventListeners: MutableMap<KClass<out STATE>, EventListener<STATE>> =
            mutableMapOf()
    private var sideEffectListeners: MutableMap<KClass<out SIDE_EFFECT>, SIDE_EFFECT.() -> Unit> =
            mutableMapOf()

    inline fun <reified S : STATE> state(
            noinline block: EventListener<S>.() -> Unit
    ) = state(S::class, block)

    inline fun <reified S : SIDE_EFFECT> onSideEffect(
            noinline block: S.() -> Unit
    ) = onSideEffect(S::class, block)

    fun <S : SIDE_EFFECT> onSideEffect(sideEffect: KClass<S>, block: S.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        sideEffectListeners[sideEffect] = block as SIDE_EFFECT.() -> Unit
    }

    fun <S : STATE> state(state: KClass<S>, block: EventListener<S>.() -> Unit) {
        @Suppress("UNCHECKED_CAST")
        eventListeners[state] =
                EventListener<S>().apply { block() } as EventListener<STATE>
    }

    internal fun onExitState(state: STATE) = transitionRelay.onTransition {
        eventListeners[state::class]
                ?.onExitTransition
                ?.invoke(state)
    }

    internal fun onEnterState(state: STATE) = transitionRelay.onTransition {
        eventListeners[state::class]
                ?.onEnterTransition
                ?.invoke(state)
    }

    internal fun onSideEffect(effect: SIDE_EFFECT?) = transitionRelay.onTransition {
        effect?.run {
            sideEffectListeners[effect::class]
                    ?.invoke(effect)
        }
    }
}

data class EventListener<STATE>(
        internal var onEnterTransition: (STATE.() -> Unit)? = null,
        internal var onExitTransition: (STATE.() -> Unit)? = null
) {
    fun onEnter(transition: STATE.() -> Unit) = apply {
        onEnterTransition = transition
    }

    fun onExit(transition: STATE.() -> Unit) = apply {
        onExitTransition = transition
    }
}

interface Subscription {
    fun unsubscribe()
}
