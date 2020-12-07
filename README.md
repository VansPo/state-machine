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
## Extensions (WIP)
### ObservableStateRegistry
An extension for `Registry<T>()` that allows you to observe a selected `State` value. It is useful if you want to trigger certain events only when the observed value has changed, regardless of state transition sequence:
```kotlin
sealed class State(val common: Int) {
    data class First(val isLoading: Boolean) : State(1)
    data class Second(val isLoading: Boolean) : State(2)
}

// init your state machine with ObservableStateregistry
val registry = ObservableStateRegistry<State>()
stateMachine(State.First(false), registry = registry) {
    // declare state transitions
}

fun example() {
    registry.selectObserve(State.First::isLoading)
        .onEach {
            // will be triggered whenever State.First::isLoading changes its value
        }
        .launchIn(lifecycleScope)
        
    registry.selectObserve(State::common)
        .onEach {
            // will be triggered every time State changes from First to Second and vice versa
        }
        .launchIn(lifecycleScope)
}

```
### TransientRegistry
An extension for `Registry<T>()` that allows you to mark some of the states as `Transient` in order to retain latest non-transient state.
Example:
```kotlin
sealed class State {
    data class Content(val title: String) : State()
    object Progress : State(), Transient
    object Error : State()
    object Empty: State()
}

// init your state machine with TransientRegistry
val registry = TransientRegistry<State>()
stateMachine(State.Empty,registry) {
    // declare state transitions
}

// ...
fun example() {
    stateMachine.postEvent(Event.OnContent("test"))
    assert(stateMachine.state is State.Content)
    stateMachine.postEvent(Event.OnRefresh)
    assert(stateMachine.state is State.Progress)
    val latestState = registry.latestNonTransientState
    assert(latestState == State.Content("test"))
}
```
### Lifecycle-aware observer
Subscribe to state updates with provided `LifecycleOwner` to automatically unsubscribe from events after receiving `Lifecycle.Event.ON_DESTROY`.
By default it uses a `kotlinx.coroutines.MainScope()` coroutine scope to force updates on main thread. You can substitute it with your own scope.
```kotlin
// in Activity class:
fun observe() {
    viewModel.stateMachine.observeWithLifecycle(
        this, // LifecycleOwner
        myScope // or leave a default scope
    ) {
        state<State.Content> {
            onEnter { /* update UI */ }
        }
    }
}
```
## Download
### Gradle
```
implementation 'com.github.vanspo:state-machine:0.6.2'
implementation 'com.github.vanspo:state-machine-extensions:0.6.2'
```
## Licence
```
MIT License

Copyright (c) 2020 Ivan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
