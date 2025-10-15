package moe.shizuku.manager.utils

import java.util.concurrent.atomic.AtomicReference
import rikka.shizuku.Shizuku

object ShizukuStateMachine {

    enum class State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        CRASHED
    }

    private var currentState = AtomicReference<State>()

    fun init() {
        if (currentState.get() != null) return
        val state = if (Shizuku.pingBinder()) State.RUNNING else State.STOPPED
        currentState.set(state)
    }

    fun setState(newState: State) {
        currentState.set(newState)
    }

    fun getState(): State? {
        return currentState.get()
    }
}