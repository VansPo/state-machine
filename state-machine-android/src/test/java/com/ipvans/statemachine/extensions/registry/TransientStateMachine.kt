package com.ipvans.statemachine.extensions.registry

import com.ipvans.statemachine.StateMachine
import com.ipvans.statemachine.registry.Registry
import com.ipvans.statemachine.stateMachine

fun transientStateMachine(registry: Registry<State>): StateMachine<State, Event, Nothing> =
    stateMachine(
        State.Empty,
        registry
    ) {
        state<State.Empty> {
            onEvent<Event.OnRefresh> { _, _ ->
                transitionTo(
                    State.Progress
                )
            }
            onEvent<Event.OnContent> { event, _ ->
                transitionTo(
                    State.Content(
                        event.title
                    )
                )
            }
        }
        state<State.Progress> {
            onEvent<Event.OnContent> { event, _ ->
                transitionTo(
                    State.Content(
                        event.title
                    )
                )
            }
            onEvent<Event.OnError> { _, _ ->
                transitionTo(
                    State.Error
                )
            }
        }
        state<State.Content> {
            onEvent<Event.OnRefresh> { _, _ ->
                transitionTo(
                    State.Progress
                )
            }
            onEvent<Event.OnTemporaryError> { _, _ ->
                transitionTo(
                    State.ErrorToast
                )
            }
        }
        state<State.Error> {
            onEvent<Event.OnRefresh> { _, _ ->
                transitionTo(
                    State.Progress
                )
            }
        }
        state<State.ErrorToast> {
            onEvent<Event.OnRefresh> { _, _ ->
                transitionTo(
                    State.Progress
                )
            }
            onEvent<Event.OnDismissToastError> { event, _ ->
                transitionTo(event.oldState)
            }
        }
    }

sealed class State {
    data class Content(val title: String) : State()
    object Progress : State(),
        Transient
    object Error : State()
    object ErrorToast : State(),
        Transient
    object Empty : State()
}

sealed class Event {
    object OnRefresh : Event()
    object OnError : Event()
    object OnTemporaryError : Event()
    data class OnContent(val title: String) : Event()
    data class OnDismissToastError(val oldState: State.Content) : Event()
}
