package com.ipvans.statemachine.extensions

import com.ipvans.statemachine.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

suspend fun <T, S : Any, E : Any, SE : Any> StateMachine<S, E, SE>.observeFlow(
    flow: Flow<T>,
    mapper: (T) -> E
) = flow.collect { postEvent(mapper(it)) }

fun <T, S : Any, E : Any, SE : Any> StateMachine<S, E, SE>.observeFlow(
    flow: Flow<T>,
    coroutineScope: CoroutineScope,
    mapper: (T) -> E
): Job = coroutineScope.launch { flow.collect { postEvent(mapper(it)) } }
