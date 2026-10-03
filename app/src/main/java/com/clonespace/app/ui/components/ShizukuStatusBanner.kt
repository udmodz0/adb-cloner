package com.clonespace.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonespace.app.data.model.ShizukuState
import com.clonespace.app.ui.theme.ErrorRed
import com.clonespace.app.ui.theme.SuccessGreen
import com.clonespace.app.ui.theme.WarningAmber

@Composable
fun ShizukuStatusBanner(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onOpenOemGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, title, subtitle, icon) = when (state) {
        is ShizukuState.Authorized -> {
            val uidBadge = if (state.isRoot) "Root (UID 0)" else "ADB Shell (UID 2000)"
            Quadruple(
                SuccessGreen,
                "Shizuku Authorized (v${state.version})",
                "Binder connected via $uidBadge. Ready for clone commands.",
                Icons.Rounded.CheckCircle
            )
        }
        is ShizukuState.RunningUnauthorized -> {
            Quadruple(
                WarningAmber,
                "Shizuku Running (Unauthorized)",
                "Service active. CloneSpace needs ADB execution permission.",
                Icons.Rounded.WarningAmber
            )
        }
        is ShizukuState.Disconnected -> {
            Quadruple(
                ErrorRed,
                "Shizuku Not Running",
                "Start Shizuku via Wireless Debugging or PC ADB to proceed.",
                Icons.Rounded.ErrorOutline
            )
        }
        is ShizukuState.NotInstalled -> {
            Quadruple(
                ErrorRed,
                "Shizuku App Missing",
                "Install Shizuku from GitHub or Google Play to manage workspaces.",
                Icons.Rounded.Security
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = statusColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Status",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = state !is ShizukuState.Authorized) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onOpenOemGuide,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("OEM Troubleshooting", fontSize = 12.sp)
                        }

                        if (state is ShizukuState.RunningUnauthorized) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
