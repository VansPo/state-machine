package com.github.vanspo.statemachine.order

import com.github.vanspo.statemachine.stateMachine

class OrderStateMachine {
    fun getState() = stateMachine<State, Event, SideEffect>(State.Initial) {
        state<State.Initial> {
            onEvent<Event.OnRefresh> { _, _ -> transitionTo(State.Loading) }
        }
        state<State.Loading> {
            onEvent<Event.OnContent> { _, _ -> transitionTo(State.Content, SideEffect.Reset) }
            onEvent<Event.OnReset> { _, _ -> transitionTo(State.Initial) }
        }
        state<State.Content> {
            onEvent<Event.OnExit> { _, _ -> transitionTo(State.Terminal) }
        }
    }
}

sealed class State {
    object Initial : State()
    object Loading : State()
    object Content : State()
    object Terminal: State()
}

sealed class Event {
    object OnContent : Event()
    object OnRefresh : Event()
    object OnExit : Event()
    object OnReset: Event()
}

sealed class SideEffect {
    object Reset : SideEffect()
}
