package com.github.vanspo.statemachine

import java.util.LinkedList
import java.util.Queue

class MessageQueue {
    private val queue: Queue<() -> Unit> = LinkedList()
    private var isStopped: Boolean = false
    fun post(message: () -> Unit) {
        queue.add(message)
        runNext()
    }
    fun stop() { isStopped = true }
    fun start() {
        isStopped = false
        while (!queue.isEmpty()) {
            runNext()
        }
    }

    private fun runNext() {
        if (isStopped) {
            return
        }
        queue.poll().invoke()
    }
}
