package com.clonespace.app.data.shizuku

import com.clonespace.app.data.model.CommandLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ShellOutput(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object ShellCommander {

    private val _logs = MutableStateFlow<List<CommandLog>>(emptyList())
    val logs: StateFlow<List<CommandLog>> = _logs.asStateFlow()

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private val newProcessMethod by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
        } catch (e: Throwable) {
            null
        }
    }

    private fun createRemoteProcess(cmd: Array<String>): Process {
        val method = newProcessMethod
            ?: throw IllegalStateException("Shizuku.newProcess method not available on this API version")
        return method.invoke(null, cmd, null, null) as Process
    }

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (!ShizukuManager.isAuthorized()) {
            val err = "Shizuku is not running or CloneSpace has not been granted authorization."
            recordLog(
                CommandLog(
                    command = command,
                    stdout = "",
                    stderr = err,
                    exitCode = -1,
                    durationMs = 0
                )
            )
            return@withContext Result.failure(IllegalStateException(err))
        }

        try {
            val process = createRemoteProcess(arrayOf("sh", "-c", command))

            val (stdout, stderr) = coroutineScope {
                val stdoutDeferred = async(Dispatchers.IO) { readStream(process.inputStream) }
                val stderrDeferred = async(Dispatchers.IO) { readStream(process.errorStream) }
                Pair(stdoutDeferred.await(), stderrDeferred.await())
            }

            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - startTime

            val log = CommandLog(
                command = command,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                durationMs = duration
            )
            recordLog(log)

            if (exitCode == 0) {
                Result.success(stdout.trim())
            } else {
                val formattedError = interpretShellError(command, stdout, stderr, exitCode)
                Result.failure(ShellExecutionException(formattedError, exitCode, stdout, stderr))
            }
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            val log = CommandLog(
                command = command,
                stdout = "",
                stderr = t.localizedMessage ?: "Process spawn error",
                exitCode = -1,
                durationMs = duration
            )
            recordLog(log)
            Result.failure(t)
        }
    }

    private fun readStream(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).use { reader ->
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            sb.toString()
        }
    }

    private fun recordLog(log: CommandLog) {
        val current = _logs.value.toMutableList()
        current.add(0, log) // newest first
        if (current.size > 200) {
            _logs.value = current.take(200)
        } else {
            _logs.value = current
        }
    }

    private fun interpretShellError(
        cmd: String,
        stdout: String,
        stderr: String,
        exitCode: Int
    ): String {
        val combined = "$stdout\n$stderr".lowercase()

        return when {
            combined.contains("install_failed_user_restricted") || combined.contains("securityexception") -> {
                "Permission Denied by OEM: On Xiaomi (MIUI/HyperOS), please go to Developer Options and enable 'Install via USB' and 'USB Debugging (Security settings)'."
            }
            combined.contains("max_users") || combined.contains("maximum user limit is reached") || combined.contains("cannot add user") || combined.contains("cannot create more users") || combined.contains("user_creation_failed") -> {
                "Maximum user limit reached for Standard Secondary Profiles. Please choose 'Work / Managed Profile' instead, which uses an independent quota and works on this device."
            }
            combined.contains("not found") -> {
                "Command binary or package target not found (exit $exitCode)."
            }
            stderr.isNotBlank() -> stderr.trim()
            stdout.isNotBlank() -> stdout.trim()
            else -> "Command failed with exit code $exitCode: $cmd"
        }
    }
}

class ShellExecutionException(
    override val message: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) : Exception(message)
