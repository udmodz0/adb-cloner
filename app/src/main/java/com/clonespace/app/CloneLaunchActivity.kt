package com.clonespace.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.clonespace.app.data.shizuku.ShellCommander
import com.clonespace.app.data.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloneLaunchActivity : Activity() {

    companion object {
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUserId = intent.getIntExtra(EXTRA_USER_ID, -1)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (targetUserId == -1 || packageName.isNullOrBlank()) {
            Toast.makeText(this, "Invalid shortcut parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        scope.launch {
            if (!ShizukuManager.isAuthorized()) {
                Toast.makeText(
                    this@CloneLaunchActivity,
                    "Shizuku is not running or authorized to launch clone",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }

            if (targetUserId != 0) {
                var state = ShellCommander.execute("am get-started-user-state $targetUserId")
                    .getOrNull()
                    ?.trim()
                    .orEmpty()

                if (!state.contains("RUNNING_", ignoreCase = true)) {
                    ShellCommander.execute("am start-user $targetUserId")
                    state = ShellCommander.execute("am get-started-user-state $targetUserId")
                        .getOrNull()
                        ?.trim()
                        .orEmpty()
                }

                if (!state.contains("RUNNING_UNLOCKED", ignoreCase = true)) {
                    ShellCommander.execute(
                        "am start --user $targetUserId -a android.app.action.CONFIRM_DEVICE_CREDENTIAL"
                    )
                    var unlocked = false
                    repeat(60) {
                        delay(400)
                        val pollState = ShellCommander.execute("am get-started-user-state $targetUserId")
                            .getOrNull()
                            ?.trim()
                            .orEmpty()
                        if (pollState.contains("RUNNING_UNLOCKED", ignoreCase = true)) {
                            unlocked = true
                            return@repeat
                        }
                    }

                    if (!unlocked) {
                        Toast.makeText(
                            this@CloneLaunchActivity,
                            "Workspace (UID $targetUserId) remains locked; launch aborted to prevent fallback to User 0",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@launch
                    }
                }
            }

            val resolveCmd = "cmd package resolve-activity --brief --user $targetUserId $packageName"
            val resolveResult = ShellCommander.execute(resolveCmd)
            val activity = resolveResult.getOrNull()
                ?.lines()
                ?.lastOrNull { it.contains("/") }
                ?.trim()

            val launchCmd = if (!activity.isNullOrBlank()) {
                "am start --user $targetUserId -n $activity"
            } else {
                "am start --user $targetUserId -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p $packageName"
            }
            val result = ShellCommander.execute(launchCmd)

            if (result.isFailure) {
                Toast.makeText(
                    this@CloneLaunchActivity,
                    "Launch failed: ${result.exceptionOrNull()?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

            finish()
        }
    }
}
