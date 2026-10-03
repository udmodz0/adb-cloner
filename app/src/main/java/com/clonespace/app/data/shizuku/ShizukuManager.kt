package com.clonespace.app.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.clonespace.app.data.model.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE_PERMISSION = 1001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Disconnected)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var isListenersRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _state.value = ShizukuState.Disconnected
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                updateState()
            }
        }

    fun initialize(context: Context) {
        if (!isListenersRegistered) {
            try {
                Shizuku.addBinderReceivedListener(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
                isListenersRegistered = true
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        checkInstalledAndConnected(context)
    }

    fun cleanup() {
        if (isListenersRegistered) {
            try {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            isListenersRegistered = false
        }
    }

    fun refresh(context: Context) {
        checkInstalledAndConnected(context)
    }

    private fun checkInstalledAndConnected(context: Context) {
        scope.launch {
            val isInstalled = try {
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            if (!isInstalled) {
                _state.value = ShizukuState.NotInstalled
                return@launch
            }

            updateState()
        }
    }

    fun updateState() {
        try {
            if (!Shizuku.pingBinder()) {
                _state.value = ShizukuState.Disconnected
                return
            }

            val isPreV11 = Shizuku.isPreV11()
            val version = if (isPreV11) -1 else Shizuku.getVersion()

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val isRoot = try {
                    Shizuku.getUid() == 0
                } catch (t: Throwable) {
                    false
                }
                _state.value = ShizukuState.Authorized(version = version, isRoot = isRoot)
            } else {
                _state.value = ShizukuState.RunningUnauthorized(version = version)
            }
        } catch (t: Throwable) {
            _state.value = ShizukuState.Disconnected
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                } else {
                    updateState()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAuthorized(): Boolean {
        return _state.value is ShizukuState.Authorized
    }
}
