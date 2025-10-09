package moe.shizuku.manager.utils

import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuStateMachine {

    enum class State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        CRASHED
    }

    private var currentState: State = State.STOPPED

    fun setState(newState: State) {
        currentState = newState
    }

    fun getState(): State {
        return currentState
    }
}