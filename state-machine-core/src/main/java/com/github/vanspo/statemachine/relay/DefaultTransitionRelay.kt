package com.github.vanspo.statemachine.relay

class DefaultTransitionRelay :
    TransitionRelay {
    override fun onTransition(function: () -> Unit) = function()
    override fun clear() {}
}
