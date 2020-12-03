package com.github.vanspo.statemachine.registry

import java.util.concurrent.atomic.AtomicReference

abstract class Registry<T : Any> {
    abstract var state: T
        protected set

    internal fun updateState(newState: T) {
        state = newState
    }
}

open class DefaultStateRegistry<T : Any> : Registry<T>() {
    protected val _state: AtomicReference<T> = AtomicReference()

    @Suppress("MemberVisibilityCanBePrivate")
    override var state: T
        get() = _state.get()
        set(value) = _state.set(value)
}
