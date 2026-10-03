package com.clonespace.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DevAboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(UdmodzCyan.copy(alpha = 0.2f))
                        .border(1.5.dp, UdmodzCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("UD", fontWeight = FontWeight.Black, color = UdmodzCyan, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DevLinks.BRAND_NAME,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = UdmodzCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "By ${DevLinks.DEV_NAME}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Full Stack Developer & Software Architect crafting modern utilities, privacy tools, and digital solutions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Official Channels & Contact:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // WhatsApp Channel
                ContactActionRow(
                    label = "WhatsApp Channel",
                    value = "Follow for instant releases & updates",
                    icon = Icons.Rounded.Forum,
                    accentColor = WhatsAppGreen,
                    onClick = { DevLinks.openUrl(context, DevLinks.WHATSAPP_CHANNEL) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp Direct
                ContactActionRow(
                    label = "WhatsApp Chat",
                    value = "+94 70 463 8406",
                    icon = Icons.Rounded.Forum,
                    accentColor = WhatsAppDarkGreen,
                    onClick = { DevLinks.openUrl(context, DevLinks.WHATSAPP_DM) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Telegram Channel
                ContactActionRow(
                    label = "Telegram Community",
                    value = "@udmodz0",
                    icon = Icons.Rounded.Send,
                    accentColor = Color(0xFF24A1DE),
                    onClick = { DevLinks.openUrl(context, DevLinks.TELEGRAM_CHANNEL) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Official Website
                ContactActionRow(
                    label = "Official Website",
                    value = "udmodz.site",
                    icon = Icons.Rounded.Language,
                    accentColor = UdmodzCyan,
                    onClick = { DevLinks.openUrl(context, DevLinks.WEBSITE) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GitHub Profile
                ContactActionRow(
                    label = "GitHub Profile",
                    value = "github.com/udmodz0",
                    icon = Icons.Rounded.Code,
                    accentColor = Color(0xFFA855F7),
                    onClick = { DevLinks.openUrl(context, DevLinks.GITHUB) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Source Code Repository
                ContactActionRow(
                    label = "CloneSpace Source Code",
                    value = "github.com/udmodz0/cloner",
                    icon = Icons.Rounded.Code,
                    accentColor = Color(0xFF38BDF8),
                    onClick = { DevLinks.openUrl(context, DevLinks.GITHUB_REPO) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email
                ContactActionRow(
                    label = "Email Support",
                    value = DevLinks.EMAIL,
                    icon = Icons.Rounded.Email,
                    accentColor = Color(0xFFF59E0B),
                    onClick = { DevLinks.openUrl(context, "mailto:${DevLinks.EMAIL}") }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ContactActionRow(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        color = accentColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = value,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
