package com.github.vanspo.statemachine.logger

import com.github.vanspo.statemachine.StateMachine

interface Logger {
    fun log(message: String)
}

class DefaultLogger : Logger {
    private val tag = StateMachine::class
    override fun log(message: String) = println("$tag: $message")
}
