package com.ipvans.statemachine.extensions.registry

import com.ipvans.statemachine.registry.Registry
import java.util.concurrent.atomic.AtomicReference

class TransientStateRegistry<T : Any> : Registry<T>() {
    private val _latestNonTransientState: AtomicReference<T?> = AtomicReference()
    var latestNonTransientState: T?
        get() = _latestNonTransientState.get()
        private set(value) {
            if (value is Transient) {
                return
            }
            _latestNonTransientState.set(value)
        }

    override fun onNewState(state: T) {
        latestNonTransientState = state
    }
}
