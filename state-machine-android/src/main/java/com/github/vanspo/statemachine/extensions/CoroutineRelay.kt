package com.github.vanspo.statemachine.extensions

import com.github.vanspo.statemachine.relay.TransitionRelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CoroutineRelay(private val coroutineScope: CoroutineScope) :
    TransitionRelay {
    override fun onTransition(function: () -> Unit) {
        coroutineScope.launch { function() }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
