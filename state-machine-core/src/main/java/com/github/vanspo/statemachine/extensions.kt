package com.github.vanspo.statemachine

import com.github.vanspo.statemachine.registry.DefaultStateRegistry
import com.github.vanspo.statemachine.registry.Registry

fun <STATE : Any, EVENT : Any, SIDE_EFFECT : Any> stateMachine(
        initialState: STATE,
        registry: Registry<STATE> = DefaultStateRegistry(),
        block: StateGraph<STATE, EVENT, SIDE_EFFECT>.() -> Unit
): StateMachine<STATE, EVENT, SIDE_EFFECT> {
    val node = StateGraph<STATE, EVENT, SIDE_EFFECT>()
        .apply { block() }
    return StateMachine(
        initialState,
        registry = registry,
        stateGraph = node
    )
}
