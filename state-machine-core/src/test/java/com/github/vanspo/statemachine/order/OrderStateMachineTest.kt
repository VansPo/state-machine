package com.github.vanspo.statemachine.order

import com.github.vanspo.statemachine.StateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

import org.junit.Before

class OrderStateMachineTest {
    private lateinit var stateMachine: StateMachine<State, Event, SideEffect>
    @Before
    fun setup() {
        stateMachine = OrderStateMachine().getState()
    }
    @Test
    fun `assert state change happens before side effect`() {
        val subscription = stateMachine.observe {
            state<State.Content> {
                onEnter { stateMachine.postEvent(Event.OnExit) }
            }
            onSideEffect<SideEffect.Reset> {
                stateMachine.postEvent(Event.OnReset)
            }
        }
        stateMachine.postEvent(Event.OnRefresh)
        stateMachine.postEvent(Event.OnContent)
        assertEquals(State.Terminal, stateMachine.state)
        subscription.unsubscribe()
    }
}
