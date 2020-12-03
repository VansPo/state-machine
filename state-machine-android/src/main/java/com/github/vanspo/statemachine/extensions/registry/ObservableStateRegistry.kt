package com.github.vanspo.statemachine.extensions.registry

import com.github.vanspo.statemachine.registry.Registry
import kotlinx.coroutines.flow.*
import kotlin.reflect.KProperty1

class ObservableStateRegistry<S : Any> : Registry<S>() {
    private val _stateFlow: MutableStateFlow<S?> = MutableStateFlow(null)

    override var state: S
        get() = _stateFlow.value!!
        set(value) {
            _stateFlow.value = value
        }
    val stateFlow: Flow<S> = _stateFlow.filterNotNull()

    inline fun <reified P: S, R> selectObserve(property: KProperty1<P, R>) = stateFlow
            .filter { it is P }
            .map { property.get(it as P) }
            .distinctUntilChanged()
}
