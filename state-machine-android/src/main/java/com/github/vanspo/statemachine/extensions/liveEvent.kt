package com.github.vanspo.statemachine.extensions

import com.github.vanspo.statemachine.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

inline fun <S : Any, E : Any, SE : Any, reified K : SE> StateMachine<S, E, SE>.sideEffectAsLiveEvent(
    scope: CoroutineScope
): Flow<K> {
    val channel = Channel<K>()
    observe(CoroutineRelay(scope)) {
        onSideEffect<K> {
            scope.launch { channel.send(this@onSideEffect) }
        }
    }
    return channel.receiveAsFlow()
}
