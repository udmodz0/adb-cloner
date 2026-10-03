package com.clonespace.app

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.clonespace.app.data.shizuku.ShellCommander
import com.clonespace.app.data.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

            val resolveCmd = "cmd package resolve-activity --brief --user $targetUserId $packageName"
            val resolveResult = ShellCommander.execute(resolveCmd)
            val activity = resolveResult.getOrNull()
                ?.lines()
                ?.lastOrNull { it.contains("/") }
                ?.trim()

            val launchCmd = if (!activity.isNullOrBlank()) {
                "am start --user $targetUserId -n $activity"
            } else {
                "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
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
