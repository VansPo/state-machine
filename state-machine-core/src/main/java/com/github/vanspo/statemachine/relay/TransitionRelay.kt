package com.github.vanspo.statemachine.relay

interface TransitionRelay {
    fun onTransition(function: () -> Unit)
    fun clear()
}
