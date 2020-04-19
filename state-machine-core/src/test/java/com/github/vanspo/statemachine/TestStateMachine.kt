package com.github.vanspo.statemachine

import com.github.vanspo.statemachine.Event.OnContentLoaded
import com.github.vanspo.statemachine.Event.OnLoadContent
import com.github.vanspo.statemachine.Event.OnLoadingFailed
import com.github.vanspo.statemachine.SideEffect.StartContentLoading
import com.github.vanspo.statemachine.State.Content
import com.github.vanspo.statemachine.State.Empty
import com.github.vanspo.statemachine.State.Error
import com.github.vanspo.statemachine.State.Progress

fun testStateMachine(): StateMachine<State, Event, SideEffect> =
    stateMachine(Empty) {
        state<Empty> {
            onEvent<OnLoadContent> { _, _ -> transitionTo(Progress, StartContentLoading) }
        }
        state<Progress> {
            onEvent<OnContentLoaded> { event, _ ->
                if (event.content.isEmpty()) {
                    return@onEvent transitionTo(Empty)
                }
                transitionTo(
                    Content(event.content),
                    SideEffect.DoNothing
                )
            }
            onEvent<OnLoadingFailed> { event, _ -> transitionTo(Error(event.exception)) }
        }
        state<Content> {
            onEvent<OnContentLoaded> { event, _ ->
                transitionTo(
                    Content(event.content),
                    SideEffect.DoNothing
                )
            }
            onEvent<OnLoadContent> { _, _ -> transitionTo(Progress, StartContentLoading) }
        }
        state<Error> {
            onEvent<OnLoadContent> { _, _ -> transitionTo(Progress, StartContentLoading) }
        }
    }

sealed class State {
    object Empty : State()
    object Progress : State()
    data class Content(val list: List<String>) : State()
    data class Error(val exception: Exception) : State()
}

sealed class Event {
    object OnLoadContent : Event()
    data class OnContentLoaded(val content: List<String>) : Event()
    data class OnLoadingFailed(val exception: Exception) : Event()
}

sealed class SideEffect {
    object StartContentLoading : SideEffect()
    object DoNothing : SideEffect()
}
