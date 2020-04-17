package com.ipvans.statemachine.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.ipvans.statemachine.StateMachine
import com.ipvans.statemachine.Subscription
import com.ipvans.statemachine.TransitionObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class LifecycleAwareSubscription(
    private val lifecycleOwner: LifecycleOwner,
    private val subscription: Subscription
) : LifecycleObserver, Subscription by subscription {
    init {
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycleOwner.lifecycle.addObserver(this)
        } else {
            unsubscribe()
        }
    }

    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        unsubscribe()
        lifecycleOwner.lifecycle.removeObserver(this)
    }
}

fun <S : Any, E : Any, SE : Any> StateMachine<S, E, SE>.observeWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    coroutineScope: CoroutineScope = MainScope(),
    function: TransitionObserver<S, SE>.() -> Unit
): Subscription {
    val transitionRelay =
        CoroutineRelay(coroutineScope)
    return LifecycleAwareSubscription(
        lifecycleOwner,
        observe(transitionRelay, function)
    )
}
