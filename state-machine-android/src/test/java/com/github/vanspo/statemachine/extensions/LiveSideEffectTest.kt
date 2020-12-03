package com.github.vanspo.statemachine.extensions

import com.github.vanspo.statemachine.StateMachine
import com.github.vanspo.statemachine.stateMachine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LiveSideEffectTest {
    private lateinit var stateMachine: StateMachine<State, Event, Effect>
    private lateinit var effectFlow: Flow<Effect.Increment>

    @Before
    fun setUp() {
        stateMachine = stateMachine()
    }

    @Test
    fun `assert side effects collected once`() = runBlockingTest {
        effectFlow = stateMachine.sideEffectAsLiveEvent(this)

        stateMachine.postEvent(Event.OnClose)
        stateMachine.postEvent(Event.OnRestart)

        val firstSubscriptionEffects = collectAllEffects()
        val secondSubscriptionEffects = collectAllEffects()
        assertEquals(2, firstSubscriptionEffects.size)
        assertEquals(0, secondSubscriptionEffects.size)
    }

    private fun CoroutineScope.collectAllEffects(): List<Effect.Increment> {
        val observedValue = mutableListOf<Effect.Increment>()
        val job  = launch { effectFlow.toList(observedValue) }
        job.cancel()
        return observedValue
    }
}

private fun stateMachine() = stateMachine<State, Event, Effect>(State.Start) {
    state<State.Start> {
        onEvent<Event.OnClose> { _, _ -> transitionTo(State.Stop, Effect.Increment) }
    }
    state<State.Stop> {
        onEvent<Event.OnRestart> { _, _ -> transitionTo(State.Start, Effect.Increment) }
    }
}

private sealed class State {
    object Start : State()
    object Stop : State()
}

private sealed class Event {
    object OnClose : Event()
    object OnRestart : Event()
}

private sealed class Effect {
    object Increment : Effect()
}