# State Machine DSL
Kotlin State Machine DSL implementation heavily inspired by [Tinder StateMachine](https://github.com/Tinder/StateMachine)

## Sample usage
First, we define states, events, side effects and machine 
```kotlin
fun exampleStateMachine() =
    stateMachine<State, Event, Effect>(State.Progress) {
        state<State.Progress> {
            onEvent<Event.OnSearch> { event, _ ->
                transitionTo(
                    State.Progress,
                    Effect.RunSearch(event.query)
                )
            }
            onEvent<Event.OnContent> { event, _ -> transitionTo(State.Content(event.content)) }
        }
        state<State.Content> {
            onEvent<Event.OnRefreshForever> { _, _ -> transitionTo(State.Progress) }
            onEvent<Event.OnRefresh> { _, _ -> transitionTo(State.Progress, Effect.RunFullRefresh) }
            onEvent<Event.OnSearch> { event, _ ->
                transitionTo(
                    State.Progress,
                    Effect.RunSearch(event.query)
                )
            }
            onEvent<Event.OnContent> { event, _ -> transitionTo(State.Content(event.content)) }
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
```
Then, subscribe to state updates:
```kotlin
private val stateObserver: Subscription? = null 

fun observeState() {
    stateObserver = stateMachine.observe {
        state<State.Content> { 
            onEnter { /* update content view */ }
        }
        state<State.Progress> {
            onEnter { /* show progress view */ }
            onExit { /* hide progress view */ }
        }
        onSideEffect<Effect.RunSearch> {
            val data = filterContent(query)
            stateMachine.postEvent(Event.OnContent(data))
        }
        onSideEffect<Effect.RunFullRefresh> {
            val newData = fetchDataFromServer()
            stateMachine.postEvent(Event.OnContent(newData))
        }
    }
}
// Don't forget to unsubscribe if you no longer need state updates
fun unsubscribe() = stateObserver?.unsubscribe()
```
## Download
### Gradle
`implementation 'com.github.vanspo:state-machine:0.2'`
