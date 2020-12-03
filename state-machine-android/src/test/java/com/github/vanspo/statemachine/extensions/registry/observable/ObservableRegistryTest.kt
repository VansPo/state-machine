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
    fun `subscribed to first state property`() = runBlockingTest {
        val observedValues: MutableList<Boolean> = mutableListOf()
        val observedFlow = registry.selectObserve(State.First::isLoading)
        val job = launch { observedFlow.toList(observedValues) }

        runEventSequence()
        job.cancel()

        assertThat(observedValues, `is`(listOf(false, true, false)))
    }

    @Test
    fun `subscribed to second state property`() = runBlockingTest {
        val observedValues: MutableList<Boolean> = mutableListOf()
        val observedFlow = registry.selectObserve(State.Second::isLoading)
        val job = launch { observedFlow.toList(observedValues) }

        runEventSequence()
        job.cancel()

        assertThat(observedValues, `is`(listOf(false, true)))
    }

    @Test
    fun `subscribed to common state property`() = runBlockingTest {
        val observedValues: MutableList<Int> = mutableListOf()
        val observedFlow = registry.selectObserve(State::common)
        val job = launch { observedFlow.toList(observedValues) }

        runEventSequence()
        job.cancel()

        assertThat(observedValues, `is`(listOf(1, 2)))
    }

    private fun runEventSequence() {
        stateMachine.postEvent(Event.OnUpdate)
        stateMachine.postEvent(Event.OnContent(""))
        stateMachine.postEvent(Event.GoToSecond)
        stateMachine.postEvent(Event.OnUpdate)
    }
}

sealed class State(val common: Int) {
    data class First(val isLoading: Boolean, val content: String = "") : State(1)
    data class Second(val isLoading: Boolean, val content: String = "") : State(2)
}

sealed class Event {
    object OnUpdate : Event()
    data class OnContent(val data: String) : Event()
    object GoToSecond : Event()
    object GoToFirst : Event()
}

private fun observableStateMachine(
        registry: ObservableStateRegistry<State>
): StateMachine<State, Event, Nothing> = stateMachine(State.First(false), registry = registry) {
    state<State.First> {
        onEvent { _: Event.OnUpdate, oldState: State.First -> transitionTo(oldState.copy(isLoading = true)) }
        onEvent { event: Event.OnContent, _: State.First ->  transitionTo(State.First(false, event.data))}
        onEvent { _: Event.GoToSecond, _: State -> transitionTo(State.Second(false)) }
    }
    state<State.Second> {
        onEvent { _: Event.OnUpdate, oldState: State.Second -> transitionTo(oldState.copy(isLoading = true)) }
        onEvent { event: Event.OnContent, _: State.Second ->  transitionTo(State.Second(false, event.data))}
        onEvent { _: Event.GoToFirst, _: State -> transitionTo(State.First(false)) }
    }
}
