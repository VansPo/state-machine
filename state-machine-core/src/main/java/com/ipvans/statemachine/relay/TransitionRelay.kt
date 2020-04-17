package com.ipvans.statemachine.relay

interface TransitionRelay {
    fun onTransition(function: () -> Unit)
    fun clear()
}
