package com.github.vanspo.statemachine.extensions.registry

import com.github.vanspo.statemachine.registry.Registry
import kotlinx.coroutines.flow.*
import kotlin.reflect.KProperty1

class ObservableStateRegistry<S: Any> : Registry<S>() {
    private val _stateFlow: MutableStateFlow<S?> = MutableStateFlow(null)

    val stateFlow: Flow<S> = _stateFlow.filterNotNull()

    fun <R> selectObserve(property: KProperty1<S, R>) = _stateFlow
            .filterNotNull()
            .map { property.get(it) }
            .distinctUntilChanged()

    override fun onNewState(state: S) {
        _stateFlow.value = state
    }
}
