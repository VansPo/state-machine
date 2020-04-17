package com.ipvans.statemachine.sideeffect

import com.ipvans.statemachine.StateMachine
import com.ipvans.statemachine.Subscription
import org.junit.After
import org.junit.Before
import org.junit.Test

class SideEffectTest {
    private lateinit var stateMachine: StateMachine<State, Event, Effect>
    private var stateObserver: Subscription? = null
    @Before
    fun setup() {
        stateMachine = sideEffectMachine()
    }

    @After
    fun tearDown() {
        stateObserver?.unsubscribe()
    }

    @Test
    fun `run effect`() {
        stateObserver = stateMachine.observe {
            onSideEffect<Effect.RunSearch> {
                val data = testData.filter { it.contains(query) }
                stateMachine.postEvent(
                    Event.OnContent(
                        data
                    )
                )
            }
            onSideEffect<Effect.RunFullRefresh> {
                stateMachine.postEvent(
                    Event.OnContent(
                        testData
                    )
                )
            }
        }

        stateMachine.postEvent(
            Event.OnContent(
                testData
            )
        )
        stateMachine.postEvent(Event.OnRefreshForever)
        stateMachine.postEvent(
            Event.OnSearch(
                "5"
            )
        )
        stateMachine.postEvent(
            Event.OnSearch(
                "2"
            )
        )
        stateMachine.postEvent(
            Event.OnSearch(
                "1"
            )
        )
        assert(
            stateMachine.state == State.Content(
                listOf("1", "31", "51")
            )
        )
    }

    companion object {
        private val testData: List<String> = listOf(
            "1", "2", "31", "4", "51"
        )
    }
}
