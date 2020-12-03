package com.github.vanspo.statemachine.extensions.registry.observable

import com.github.vanspo.statemachine.StateMachine
import com.github.vanspo.statemachine.extensions.registry.ObservableStateRegistry
import com.github.vanspo.statemachine.stateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ObservableRegistryTest {
    private lateinit var stateMachine: StateMachine<State, Event, Nothing>
    private lateinit var registry: ObservableStateRegistry<State>

    @Before
    fun setUp() {
        registry = ObservableStateRegistry()
        stateMachine = observableStateMachine(registry)
    }

    @Test
    fun `subscribed to loading property`() = runBlockingTest {
        val observedValues: MutableList<Boolean> = mutableListOf()
        val observedFlow = registry.selectObserve(State::isLoading)
        val job = launch { observedFlow.toList(observedValues) }

        stateMachine.postEvent(Event.OnUpdate)
        stateMachine.postEvent(Event.OnContent(""))
        job.cancel()

        assertThat(observedValues, `is`(listOf(false, true, false)))
    }
}

data class State(val isLoading: Boolean, val data: String)

sealed class Event {
    object OnUpdate : Event()
    data class OnContent(val content: String) : Event()
}

private fun observableStateMachine(
        registry: ObservableStateRegistry<State>
): StateMachine<State, Event, Nothing> = stateMachine(State(false, ""), registry = registry) {
    state<State> {
        onEvent { _: Event.OnUpdate, oldState: State -> transitionTo(oldState.copy(isLoading = true)) }
        onEvent { event: Event.OnContent, _: State -> transitionTo(State(false, event.content)) }
    }
}
