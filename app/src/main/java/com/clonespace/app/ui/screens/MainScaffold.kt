package com.clonespace.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clonespace.app.ui.components.BatchCloneDialog
import com.clonespace.app.ui.components.CreateProfileDialog
import com.clonespace.app.ui.components.DevAboutDialog
import com.clonespace.app.ui.components.OemNoticeDialog
import com.clonespace.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showBatchCloneDialog by remember { mutableStateOf(false) }
    var showOemNoticeDialog by remember { mutableStateOf(false) }
    var showDevAboutDialog by remember { mutableStateOf(false) }

    // Collect ViewModel states
    val shizukuState by viewModel.shizukuState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val includeSystemApps by viewModel.includeSystemApps.collectAsState()
    val selectedProfileFilter by viewModel.selectedProfileFilter.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val isBatchCloning by viewModel.isBatchCloning.collectAsState()
    val batchCurrent by viewModel.batchCurrent.collectAsState()
    val batchTotal by viewModel.batchTotal.collectAsState()
    val currentBatchPackage by viewModel.currentBatchPackage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    // Show snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.refreshProfilesAndApps(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CloneSpace",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (profiles.isNotEmpty()) {
                                Badge { Text("${profiles.size}") }
                            }
                        }) {
                            Icon(Icons.Rounded.AccountTree, contentDescription = "Workspaces")
                        }
                    },
                    label = { Text("Workspaces") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.CopyAll, contentDescription = "Apps") },
                    label = { Text("App Cloner") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        val failedCount = terminalLogs.count { !it.isSuccess }
                        BadgedBox(badge = {
                            if (failedCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("$failedCount")
                                }
                            }
                        }) {
                            Icon(Icons.Rounded.Terminal, contentDescription = "Shell Logs")
                        }
                    },
                    label = { Text("Shell") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    shizukuState = shizukuState,
                    profiles = profiles,
                    apps = apps,
                    selectedProfileFilter = selectedProfileFilter,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onRefresh = { viewModel.refreshShizuku(context) },
                    onOpenOemGuide = { showOemNoticeDialog = true },
                    onOpenAboutDev = { showDevAboutDialog = true },
                    onSelectProfileFilter = { viewModel.setSelectedProfileFilter(it) },
                    onCreateProfileClick = { showCreateProfileDialog = true },
                    onNavigateToProfiles = { selectedTab = 1 },
                    onNavigateToApps = { selectedTab = 2 },
                    onNavigateToTerminal = { selectedTab = 3 }
                )

                1 -> ProfilesScreen(
                    profiles = profiles,
                    onStartProfile = { viewModel.startProfile(it) },
                    onStopProfile = { viewModel.stopProfile(it) },
                    onDeleteProfile = { viewModel.deleteProfile(context, it) },
                    onViewProfileApps = { uid ->
                        viewModel.setSelectedProfileFilter(uid)
                        selectedTab = 2
                    },
                    onCreateProfileClick = { showCreateProfileDialog = true }
                )

                2 -> AppListScreen(
                    apps = apps,
                    profiles = profiles,
                    searchQuery = searchQuery,
                    includeSystemApps = includeSystemApps,
                    selectedProfileFilter = selectedProfileFilter,
                    isSelectionMode = isSelectionMode,
                    selectedPackages = selectedPackages,
                    isLoading = isLoading,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleSystemApps = { viewModel.setIncludeSystemApps(context, it) },
                    onSelectProfileFilter = { viewModel.setSelectedProfileFilter(it) },
                    onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                    onTogglePackageSelection = { viewModel.togglePackageSelection(it) },
                    onSelectAllFiltered = { viewModel.selectAllFilteredApps() },
                    onClearSelection = { viewModel.clearSelection() },
                    onStartBatchCloneDialog = { showBatchCloneDialog = true },
                    onCloneToProfile = { targetUid, pkg -> viewModel.cloneApp(targetUid, pkg) },
                    onLaunchApp = { targetUid, pkg -> viewModel.launchApp(targetUid, pkg) },
                    onRemoveClone = { targetUid, pkg -> viewModel.removeClone(targetUid, pkg) },
                    onPinShortcut = { targetUid, pkg, label -> viewModel.pinShortcut(context, targetUid, pkg, label) },
                    onRefreshApps = { viewModel.refreshApps(context) },
                    onCreateProfileClick = { showCreateProfileDialog = true }
                )

                3 -> TerminalScreen(
                    logs = terminalLogs,
                    onClearLogs = { viewModel.clearTerminalLogs() }
                )
            }
        }
    }

    // Create Profile Dialog
    if (showCreateProfileDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onCreate = { name, isManaged ->
                showCreateProfileDialog = false
                viewModel.createWorkspace(context, name, isManaged)
            }
        )
    }

    // Batch Clone Dialog
    if (showBatchCloneDialog) {
        BatchCloneDialog(
            selectedPackages = selectedPackages.toList(),
            profiles = profiles,
            isProcessing = isBatchCloning,
            progressCurrent = batchCurrent,
            progressTotal = batchTotal,
            currentCloningPackage = currentBatchPackage,
            onDismiss = { showBatchCloneDialog = false },
            onStartBatchClone = { targetUid ->
                viewModel.startBatchClone(targetUid) {
                    showBatchCloneDialog = false
                }
            }
        )
    }

    // OEM Guide Dialog
    if (showOemNoticeDialog) {
        OemNoticeDialog(
            onDismiss = { showOemNoticeDialog = false }
        )
    }

    // Developer About & Contact Dialog
    if (showDevAboutDialog) {
        DevAboutDialog(
            onDismiss = { showDevAboutDialog = false }
        )
    }
}
