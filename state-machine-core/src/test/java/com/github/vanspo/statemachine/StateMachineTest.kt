package com.github.vanspo.statemachine

import org.junit.Before
import org.junit.Test

class StateMachineTest {
    private lateinit var stateMachine: StateMachine<State, Event, SideEffect>
    @Before
    fun setup() {
        stateMachine = testStateMachine()
    }

    @Test
    fun `when sending progress event state changes to progress`() {
        assert(stateMachine.state == State.Empty)
        stateMachine.postEvent(Event.OnLoadContent)
        assert(stateMachine.state == State.Progress)
    }

    @Test
    fun `when in initial state content event is ignored`() {
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf()
            )
        )
        assert(stateMachine.state == State.Empty)
    }

    @Test
    fun `when in progress state content loaded event changes state to content`() {
        stateMachine.postEvent(Event.OnLoadContent)
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf("test")
            )
        )
        assert(stateMachine.state == State.Content(
            listOf(
                "test"
            )
        )
        )
    }

    @Test
    fun `when empty content arrives state changes to empty`() {
        stateMachine.postEvent(Event.OnLoadContent)
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf()
            )
        )
        assert(stateMachine.state == State.Empty)
    }

    @Test
    fun `when wrong event arrives ignore it`() {
        stateMachine.postEvent(Event.OnLoadContent)
        stateMachine.postEvent(
            Event.OnLoadingFailed(
                Exception()
            )
        )
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf()
            )
        )
        assert(stateMachine.state is State.Error)
    }

    @Test
    fun `when transition to the same state only run side effect`() {
        var timesOnEnterCalled = 0
        var timesOnExitCalled = 0
        var timesSideEffectCalled = 0
        val subscription = stateMachine.observe {
            state<State.Content> {
                onEnter { timesOnEnterCalled++ }
                onExit { timesOnExitCalled++ }
            }
            onSideEffect<SideEffect.DoNothing> { timesSideEffectCalled++ }
        }
        stateMachine.postEvent(Event.OnLoadContent)
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf("test")
            )
        )
        stateMachine.postEvent(
            Event.OnContentLoaded(
                listOf("test")
            )
        )
        stateMachine.postEvent(Event.OnLoadContent)
        subscription.unsubscribe()
        assert(timesOnEnterCalled == 1)
        assert(timesOnExitCalled == 1)
        assert(timesSideEffectCalled == 2)
    }
}
