package com.github.vanspo.statemachine.extensions

import com.github.vanspo.statemachine.StateMachine
import com.github.vanspo.statemachine.stateMachine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LiveSideEffectTest {
    private lateinit var stateMachine: StateMachine<State, Event, Effect>
    private lateinit var effectFlow: Flow<Effect.Increment>

    @Before
    fun setUp() {
        stateMachine = stateMachine()
    }

    @Test
    fun `assert side effect collected once`() = runBlocking {
        var count = 0
        effectFlow = stateMachine.observeAsLiveEvent(this)
        stateMachine.postEvent(Event.OnClose)
        stateMachine.postEvent(Event.OnRestart)
        effectFlow.take(2).onEach { count++ }.collect()
        assertEquals(2, count)
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