package com.ipvans.statemachine.registry

import java.util.concurrent.atomic.AtomicReference

open class Registry<T : Any> {
    private val lock: Any = Any()
    private val _state: AtomicReference<T> = AtomicReference()

    @Suppress("MemberVisibilityCanBePrivate")
    var state: T
        get() = _state.get()
        internal set(value) = synchronized(lock) {
            _state.set(value)
            onNewState(value)
        }

    open fun onNewState(state: T) {}
}
