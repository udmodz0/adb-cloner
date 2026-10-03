package com.clonespace.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clonespace.app.data.model.CommandLog
import com.clonespace.app.ui.components.TerminalLogView

@Composable
fun TerminalScreen(
    logs: List<CommandLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    TerminalLogView(
        logs = logs,
        onClearLogs = onClearLogs,
        modifier = modifier.fillMaxSize()
    )
}
