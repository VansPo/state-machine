package com.github.vanspo.statemachine.sideeffect

import com.github.vanspo.statemachine.stateMachine

fun sideEffectMachine() =
    stateMachine<State, Event, Effect>(
        State.Progress
    ) {
        state<State.Progress> {
            onEvent<Event.OnSearch> { event, _ ->
                transitionTo(
                    State.Progress,
                    Effect.RunSearch(
                        event.query
                    )
                )
            }
            onEvent<Event.OnContent> { event, _ -> transitionTo(
                State.Content(event.content)
            ) }
        }
        state<State.Content> {
            onEvent<Event.OnRefreshForever> { _, _ -> transitionTo(
                State.Progress
            ) }
            onEvent<Event.OnRefresh> { _, _ -> transitionTo(
                State.Progress,
                Effect.RunFullRefresh
            ) }
            onEvent<Event.OnSearch> { event, _ ->
                transitionTo(
                    State.Progress,
                    Effect.RunSearch(
                        event.query
                    )
                )
            }
            onEvent<Event.OnContent> { event, _ -> transitionTo(
                State.Content(event.content)
            ) }
        }
    }

sealed class State {
    object Progress : State()
    data class Content(val content: List<String>) : State()
}

sealed class Event {
    object OnRefresh : Event()
    object OnRefreshForever : Event()
    data class OnSearch(val query: String) : Event()
    data class OnContent(val content: List<String>) : Event()
}

sealed class Effect {
    data class RunSearch(val query: String) : Effect()
    object RunFullRefresh : Effect()
}
