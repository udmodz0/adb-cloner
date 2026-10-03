package com.clonespace.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.ui.components.AppItemRow
import com.clonespace.app.ui.components.ProfileCarousel

@Composable
fun AppListScreen(
    apps: List<AppCloneItem>,
    profiles: List<UserProfile>,
    searchQuery: String,
    includeSystemApps: Boolean,
    selectedProfileFilter: Int?,
    isSelectionMode: Boolean,
    selectedPackages: Set<String>,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSystemApps: (Boolean) -> Unit,
    onSelectProfileFilter: (Int?) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onTogglePackageSelection: (String) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearSelection: () -> Unit,
    onStartBatchCloneDialog: () -> Unit,
    onCloneToProfile: (targetUserId: Int, packageName: String) -> Unit,
    onLaunchApp: (targetUserId: Int, packageName: String) -> Unit,
    onRemoveClone: (targetUserId: Int, packageName: String) -> Unit,
    onPinShortcut: (targetUserId: Int, packageName: String, appLabel: String) -> Unit,
    onRefreshApps: () -> Unit,
    onCreateProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Refresh bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by name or package...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRefreshApps,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh Packages",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filter chips (System Apps Toggle, Batch mode toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !includeSystemApps,
                onClick = { onToggleSystemApps(false) },
                label = { Text("User Apps") }
            )

            FilterChip(
                selected = includeSystemApps,
                onClick = { onToggleSystemApps(true) },
                label = { Text("Include System") }
            )

            Spacer(modifier = Modifier.weight(1f))

            FilterChip(
                selected = isSelectionMode,
                onClick = onToggleSelectionMode,
                label = { Text("Batch Mode") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            )
        }

        // Horizontal workspace filter
        ProfileCarousel(
            profiles = profiles,
            selectedProfileId = selectedProfileFilter,
            onSelectProfile = onSelectProfileFilter,
            onCreateProfileClick = onCreateProfileClick
        )

        // Batch Action Bar (if selection mode is active)
        AnimatedVisibility(visible = isSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedPackages.size} Selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onSelectAllFiltered) {
                            Text("Select All", fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onStartBatchCloneDialog,
                            enabled = selectedPackages.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CopyAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batch Clone", fontSize = 11.sp)
                        }

                        IconButton(onClick = onClearSelection) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Batch Mode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // List of Apps
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching packages found" else "No applications found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppItemRow(
                        app = app,
                        profiles = profiles,
                        currentFilteredProfileId = selectedProfileFilter,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedPackages.contains(app.packageName),
                        onToggleSelect = { onTogglePackageSelection(app.packageName) },
                        onCloneToProfile = onCloneToProfile,
                        onLaunchApp = onLaunchApp,
                        onRemoveClone = onRemoveClone,
                        onPinShortcut = onPinShortcut
                    )
                }
            }
        }
    }
}
