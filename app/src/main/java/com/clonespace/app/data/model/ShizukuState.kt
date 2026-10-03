package com.clonespace.app.data.model

sealed interface ShizukuState {
    data object NotInstalled : ShizukuState
    data object Disconnected : ShizukuState
    data class RunningUnauthorized(val version: Int) : ShizukuState
    data class Authorized(val version: Int, val isRoot: Boolean = false) : ShizukuState
}
