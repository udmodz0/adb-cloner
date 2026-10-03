package com.clonespace.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CommandLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long
) {
    val isSuccess: Boolean get() = exitCode == 0

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    fun toFormattedString(): String {
        return buildString {
            append("[$formattedTime] $ $command (exit: $exitCode, ${durationMs}ms)\n")
            if (stdout.isNotBlank()) {
                append("[STDOUT]\n").append(stdout.trimEnd()).append("\n")
            }
            if (stderr.isNotBlank()) {
                append("[STDERR]\n").append(stderr.trimEnd()).append("\n")
            }
        }
    }
}
