package com.clonespace.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.ui.theme.ErrorRed
import com.clonespace.app.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppItemRow(
    app: AppCloneItem,
    profiles: List<UserProfile>,
    currentFilteredProfileId: Int?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onCloneToProfile: (targetUserId: Int, packageName: String) -> Unit,
    onLaunchApp: (targetUserId: Int, packageName: String) -> Unit,
    onRemoveClone: (targetUserId: Int, packageName: String) -> Unit,
    onPinShortcut: (targetUserId: Int, packageName: String, appLabel: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Eligible target secondary profiles (non-owner)
    val cloneableProfiles = profiles.filter { !it.isOwner }

    // If currently filtering a specific profile
    val isInstalledInCurrentFilter = currentFilteredProfileId != null &&
            app.isInstalledIn(currentFilteredProfileId)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .clickable {
                if (isSelectionMode) onToggleSelect()
            },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // App Icon
                val imageBitmap = remember(app.iconBitmap) {
                    app.iconBitmap?.asImageBitmap()
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = app.appLabel,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Title & Package
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "System",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = app.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "v${app.versionName}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Quick Launch button if filtered to a secondary profile and installed
                if (currentFilteredProfileId != null && isInstalledInCurrentFilter) {
                    FilledTonalButton(
                        onClick = { onLaunchApp(currentFilteredProfileId, app.packageName) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Launch",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Launch", fontSize = 11.sp)
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // Clone to options
                        if (cloneableProfiles.isNotEmpty()) {
                            Text(
                                text = "Clone into Workspace:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )

                            cloneableProfiles.forEach { profile ->
                                val alreadyCloned = app.isInstalledIn(profile.id)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${profile.name} (UID ${profile.id})${if (alreadyCloned) " [Already Cloned]" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = if (alreadyCloned) FontWeight.Normal else FontWeight.Medium
                                        )
                                    },
                                    enabled = !alreadyCloned,
                                    onClick = {
                                        menuExpanded = false
                                        onCloneToProfile(profile.id, app.packageName)
                                    }
                                )
                            }
                        }

                        // Launch options
                        val activeClones = app.installedProfileIds.filter { it != 0 }
                        if (activeClones.isNotEmpty()) {
                            Text(
                                text = "Launch in Workspace:",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )

                            activeClones.forEach { uid ->
                                val profileName = profiles.find { it.id == uid }?.name ?: "User $uid"
                                DropdownMenuItem(
                                    text = { Text("Launch in $profileName", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onLaunchApp(uid, app.packageName)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Add Home Shortcut ($profileName)", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.AddHome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onPinShortcut(uid, app.packageName, app.appLabel)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Remove from $profileName", fontSize = 12.sp, color = ErrorRed) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = ErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRemoveClone(uid, app.packageName)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Installed In Tags
            if (app.installedProfileIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    app.installedProfileIds.sorted().forEach { uid ->
                        val p = profiles.find { it.id == uid }
                        val label = if (uid == 0) "User 0 (Main)" else "${p?.name ?: "User $uid"} (U$uid)"
                        Surface(
                            shape = CircleShape,
                            color = if (uid == 0) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (uid == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
