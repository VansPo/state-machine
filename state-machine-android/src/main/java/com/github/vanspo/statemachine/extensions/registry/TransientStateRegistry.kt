package com.github.vanspo.statemachine.extensions.registry

import com.github.vanspo.statemachine.registry.DefaultStateRegistry
import java.util.concurrent.atomic.AtomicReference

class TransientStateRegistry<T : Any> : DefaultStateRegistry<T>() {
    private val lock: Any = Any()
    private val _latestNonTransientState: AtomicReference<T?> = AtomicReference()

    var latestNonTransientState: T?
        get() = _latestNonTransientState.get()
        set(value) {
            _latestNonTransientState.set(value)
        }

    override var state: T
        get() = super.state
        set(value) = synchronized(lock) {
            _state.set(value)
            if (value is Transient) {
                return
            }
            latestNonTransientState = value
        }
}
