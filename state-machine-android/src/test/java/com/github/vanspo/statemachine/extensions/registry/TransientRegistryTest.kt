package com.github.vanspo.statemachine.extensions.registry

import com.github.vanspo.statemachine.StateMachine
import com.github.vanspo.statemachine.extensions.registry.TransientStateRegistry
import org.junit.Before
import org.junit.Test

class TransientRegistryTest {
    private lateinit var stateMachine: StateMachine<State, Event, Nothing>
    private lateinit var registry: TransientStateRegistry<State>
    @Before
    fun setup() {
        registry =
            TransientStateRegistry()
        stateMachine =
            transientStateMachine(
                registry
            )
    }

    @Test
    fun `when move to progress ignore transient state`() {
        stateMachine.postEvent(Event.OnRefresh)
        assert(stateMachine.state is State.Progress)
        assert(registry.latestNonTransientState is State.Empty)
    }

    @Test
    fun `when in transient state restore from latest content state`() {
        stateMachine.postEvent(Event.OnRefresh)
        stateMachine.postEvent(
                Event.OnContent(
                        "test"
                )
        )
        stateMachine.postEvent(Event.OnTemporaryError)
        assert(stateMachine.state is State.ErrorToast)
        val latestState: State.Content = registry.latestNonTransientState as State.Content
        assert(latestState == State.Content(
                "test"
        )
        )
        stateMachine.postEvent(
                Event.OnDismissToastError(
                        latestState
                )
        )
        assert(stateMachine.state == State.Content(
                "test"
        )
        )
    }
}
