package com.clonespace.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.CommandLog
import com.clonespace.app.data.model.ShizukuState
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.repository.AppRepositoryImpl
import com.clonespace.app.data.repository.ProfileRepositoryImpl
import com.clonespace.app.data.shizuku.ShellCommander
import com.clonespace.app.data.shizuku.ShizukuManager
import com.clonespace.app.domain.usecase.CloneAppUseCase
import com.clonespace.app.domain.usecase.CreateProfileUseCase
import com.clonespace.app.domain.usecase.GetInstalledAppsUseCase
import com.clonespace.app.domain.usecase.GetProfilesUseCase
import com.clonespace.app.domain.usecase.LaunchClonedAppUseCase
import com.clonespace.app.domain.usecase.ManageProfileStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Repositories
    private val profileRepository = ProfileRepositoryImpl()
    private val appRepository = AppRepositoryImpl()

    // Use cases
    private val getProfilesUseCase = GetProfilesUseCase(profileRepository)
    private val createProfileUseCase = CreateProfileUseCase(profileRepository)
    private val manageProfileStateUseCase = ManageProfileStateUseCase(profileRepository)
    private val getInstalledAppsUseCase = GetInstalledAppsUseCase(appRepository)
    private val cloneAppUseCase = CloneAppUseCase(appRepository)
    private val launchClonedAppUseCase = LaunchClonedAppUseCase(appRepository)

    // Shizuku state
    val shizukuState: StateFlow<ShizukuState> = ShizukuManager.state

    // Live terminal command logs
    val terminalLogs: StateFlow<List<CommandLog>> = ShellCommander.logs

    // Profiles
    val profiles: StateFlow<List<UserProfile>> = getProfilesUseCase.profilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Apps
    private val allApps: StateFlow<List<AppCloneItem>> = getInstalledAppsUseCase.appsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters and UI states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val _selectedProfileFilter = MutableStateFlow<Int?>(null)
    val selectedProfileFilter: StateFlow<Int?> = _selectedProfileFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Multi-selection state for batch cloning
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    // Batch progress
    private val _isBatchCloning = MutableStateFlow(false)
    val isBatchCloning: StateFlow<Boolean> = _isBatchCloning.asStateFlow()

    private val _batchCurrent = MutableStateFlow(0)
    val batchCurrent: StateFlow<Int> = _batchCurrent.asStateFlow()

    private val _batchTotal = MutableStateFlow(0)
    val batchTotal: StateFlow<Int> = _batchTotal.asStateFlow()

    private val _currentBatchPackage = MutableStateFlow<String?>(null)
    val currentBatchPackage: StateFlow<String?> = _currentBatchPackage.asStateFlow()

    // Filtered apps combining query, system app filter, and profile filter
    val filteredApps: StateFlow<List<AppCloneItem>> = combine(
        allApps,
        _searchQuery,
        _selectedProfileFilter
    ) { apps, query, profileId ->
        apps.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.appLabel.contains(query, ignoreCase = true) ||
                    item.packageName.contains(query, ignoreCase = true)

            val matchesProfile = profileId == null || item.isInstalledIn(profileId)

            matchesQuery && matchesProfile
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setIncludeSystemApps(context: Context, include: Boolean) {
        _includeSystemApps.value = include
        refreshApps(context)
    }

    fun setSelectedProfileFilter(profileId: Int?) {
        _selectedProfileFilter.value = profileId
    }

    fun toggleSelectionMode() {
        val newMode = !_isSelectionMode.value
        _isSelectionMode.value = newMode
        if (!newMode) {
            _selectedPackages.value = emptySet()
        }
    }

    fun togglePackageSelection(packageName: String) {
        val set = _selectedPackages.value.toMutableSet()
        if (set.contains(packageName)) {
            set.remove(packageName)
        } else {
            set.add(packageName)
        }
        _selectedPackages.value = set
    }

    fun selectAllFilteredApps() {
        val allPkgs = filteredApps.value.map { it.packageName }.toSet()
        _selectedPackages.value = allPkgs
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
        _isSelectionMode.value = false
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun refreshShizuku(context: Context) {
        ShizukuManager.refresh(context)
        refreshProfilesAndApps(context)
    }

    fun refreshProfilesAndApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            getProfilesUseCase().onSuccess { profs ->
                getInstalledAppsUseCase(context, profs, _includeSystemApps.value)
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to list profiles: ${err.message}"
            }
            _isLoading.value = false
        }
    }

    fun refreshApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            getInstalledAppsUseCase(context, profiles.value, _includeSystemApps.value)
            _isLoading.value = false
        }
    }

    fun createWorkspace(context: Context, name: String, isManaged: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            createProfileUseCase(name, isManaged)
                .onSuccess {
                    _snackbarMessage.value = "Workspace '${it.name}' created and started (UID ${it.id})"
                    refreshApps(context)
                }
                .onFailure {
                    _snackbarMessage.value = "Failed to create workspace: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    fun startProfile(userId: Int) {
        viewModelScope.launch {
            manageProfileStateUseCase.start(userId)
                .onSuccess {
                    _snackbarMessage.value = "Workspace (UID $userId) started."
                }
                .onFailure {
                    _snackbarMessage.value = "Error starting profile: ${it.message}"
                }
        }
    }

    fun stopProfile(userId: Int) {
        viewModelScope.launch {
            manageProfileStateUseCase.stop(userId)
                .onSuccess {
                    _snackbarMessage.value = "Workspace (UID $userId) stopped."
                }
                .onFailure {
                    _snackbarMessage.value = "Error stopping profile: ${it.message}"
                }
        }
    }

    fun deleteProfile(context: Context, userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            manageProfileStateUseCase.delete(userId)
                .onSuccess {
                    _snackbarMessage.value = "Workspace (UID $userId) deleted."
                    if (_selectedProfileFilter.value == userId) {
                        _selectedProfileFilter.value = null
                    }
                    refreshApps(context)
                }
                .onFailure {
                    _snackbarMessage.value = "Error deleting profile: ${it.message}"
                }
            _isLoading.value = false
        }
    }

    fun cloneApp(targetUserId: Int, packageName: String) {
        viewModelScope.launch {
            cloneAppUseCase.cloneSingle(targetUserId, packageName)
                .onSuccess {
                    _snackbarMessage.value = "Cloned $packageName to User $targetUserId"
                }
                .onFailure {
                    _snackbarMessage.value = "Clone failed: ${it.message}"
                }
        }
    }

    fun startBatchClone(targetUserId: Int, onComplete: () -> Unit) {
        val packagesToClone = _selectedPackages.value.toList()
        if (packagesToClone.isEmpty()) return

        viewModelScope.launch {
            _isBatchCloning.value = true
            _batchTotal.value = packagesToClone.size
            _batchCurrent.value = 0

            cloneAppUseCase.cloneBatch(targetUserId, packagesToClone) { current, total, pkg ->
                _batchCurrent.value = current
                _batchTotal.value = total
                _currentBatchPackage.value = pkg
            }.onSuccess { successCount ->
                _snackbarMessage.value = "Batch cloned $successCount / ${packagesToClone.size} apps to User $targetUserId"
                clearSelection()
            }.onFailure {
                _snackbarMessage.value = "Batch clone failed: ${it.message}"
            }

            _isBatchCloning.value = false
            onComplete()
        }
    }

    fun removeClone(targetUserId: Int, packageName: String) {
        viewModelScope.launch {
            cloneAppUseCase.removeClone(targetUserId, packageName)
                .onSuccess {
                    _snackbarMessage.value = "Removed $packageName from User $targetUserId"
                }
                .onFailure {
                    _snackbarMessage.value = "Remove failed: ${it.message}"
                }
        }
    }

    fun launchApp(targetUserId: Int, packageName: String) {
        viewModelScope.launch {
            launchClonedAppUseCase(targetUserId, packageName)
                .onSuccess {
                    _snackbarMessage.value = "Launched $packageName in User $targetUserId"
                }
                .onFailure {
                    _snackbarMessage.value = "Launch error: ${it.message}"
                }
        }
    }

    fun pinShortcut(context: Context, targetUserId: Int, packageName: String, appLabel: String) {
        cloneAppUseCase.pinShortcut(context, targetUserId, packageName, appLabel)
            .onSuccess {
                _snackbarMessage.value = "Shortcut added to home screen."
            }
            .onFailure {
                _snackbarMessage.value = "Shortcut error: ${it.message}"
            }
    }

    fun clearTerminalLogs() {
        ShellCommander.clearLogs()
    }
}
