package com.clonespace.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.clonespace.app.CloneLaunchActivity
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.shizuku.ShellCommander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppRepositoryImpl : AppRepository {

    private val _appsFlow = MutableStateFlow<List<AppCloneItem>>(emptyList())
    override val appsFlow: Flow<List<AppCloneItem>> = _appsFlow.asStateFlow()

    override suspend fun refreshApps(
        context: Context,
        profiles: List<UserProfile>,
        includeSystemApps: Boolean
    ): Result<List<AppCloneItem>> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch package list on User 0
            val cmd = if (includeSystemApps) "pm list packages" else "pm list packages -3"
            val user0PackagesResult = ShellCommander.execute(cmd)
            val user0PackageNames = user0PackagesResult.getOrThrow()
                .lines()
                .map { it.trim().removePrefix("package:").trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            // 2. Fetch packages installed in other user profiles to cross-reference
            val profileAppMap = mutableMapOf<Int, Set<String>>()
            for (profile in profiles) {
                if (profile.id == 0) {
                    profileAppMap[0] = user0PackageNames
                } else if (profile.isRunning) {
                    val profileCmd = "pm list packages --user ${profile.id}"
                    val result = ShellCommander.execute(profileCmd)
                    if (result.isSuccess) {
                        val pkgs = result.getOrNull()
                            ?.lines()
                            ?.map { it.trim().removePrefix("package:").trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.toSet() ?: emptySet()
                        profileAppMap[profile.id] = pkgs
                    }
                }
            }

            // 3. Batch query PackageManager in a SINGLE IPC call to avoid freezing system_server
            val pm = context.packageManager
            val installedPackagesList = try {
                pm.getInstalledPackages(0)
            } catch (e: Exception) {
                emptyList()
            }
            val packageInfoMap = installedPackagesList.associateBy { it.packageName }

            val items = ArrayList<AppCloneItem>(user0PackageNames.size)

            for (pkg in user0PackageNames) {
                val pkgInfo = packageInfoMap[pkg]
                val appInfo = pkgInfo?.applicationInfo

                val label = if (appInfo != null) {
                    try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                } else {
                    pkg
                }

                val isSystem = if (appInfo != null) {
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } else {
                    false
                }

                val versionName = pkgInfo?.versionName ?: "1.0"

                val icon = if (appInfo != null) {
                    try {
                        val drawable = appInfo.loadIcon(pm)
                        createIconThumbnail(drawable)
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }

                val installedProfiles = profileAppMap.filter { (_, pkgs) ->
                    pkgs.contains(pkg)
                }.keys.toSet()

                items.add(
                    AppCloneItem(
                        packageName = pkg,
                        appLabel = label,
                        versionName = versionName,
                        isSystemApp = isSystem,
                        installedProfileIds = installedProfiles,
                        iconBitmap = icon
                    )
                )
            }

            val sortedItems = items.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.packageName }))
            _appsFlow.value = sortedItems
            Result.success(sortedItems)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun cloneApp(
        targetUserId: Int,
        packageName: String
    ): Result<Unit> {
        val cmd = "pm install-existing --user $targetUserId $packageName"
        val result = ShellCommander.execute(cmd)
        return result.mapCatching {
            // Update local state flow
            updateAppMembership(packageName, targetUserId, isAdded = true)
            Unit
        }
    }

    override suspend fun batchCloneApps(
        targetUserId: Int,
        packageNames: List<String>,
        onProgress: (current: Int, total: Int, packageName: String) -> Unit
    ): Result<Int> {
        var successCount = 0
        val total = packageNames.size

        for ((index, pkg) in packageNames.withIndex()) {
            onProgress(index + 1, total, pkg)
            val result = ShellCommander.execute("pm install-existing --user $targetUserId $pkg")
            if (result.isSuccess) {
                successCount++
                updateAppMembership(pkg, targetUserId, isAdded = true)
            }
        }
        return Result.success(successCount)
    }

    override suspend fun removeClone(
        targetUserId: Int,
        packageName: String
    ): Result<Unit> {
        val cmd = "pm uninstall --user $targetUserId $packageName"
        val result = ShellCommander.execute(cmd)
        return result.mapCatching {
            updateAppMembership(packageName, targetUserId, isAdded = false)
            Unit
        }
    }

    override suspend fun launchApp(
        targetUserId: Int,
        packageName: String
    ): Result<Unit> {
        // Resolve default launcher activity for target user
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
        return result.mapCatching { Unit }
    }

    override fun createHomeScreenShortcut(
        context: Context,
        targetUserId: Int,
        packageName: String,
        appLabel: String
    ): Result<Unit> {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return Result.failure(IllegalStateException("Home screen shortcut pinning is not supported on this launcher."))
            }

            val launchIntent = Intent(context, CloneLaunchActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(CloneLaunchActivity.EXTRA_USER_ID, targetUserId)
                putExtra(CloneLaunchActivity.EXTRA_PACKAGE_NAME, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pm = context.packageManager
            val appIcon: Drawable = try {
                pm.getApplicationIcon(packageName)
            } catch (e: Exception) {
                context.packageManager.defaultActivityIcon
            }

            val bitmap = createIconThumbnail(appIcon)
            val iconCompat = IconCompat.createWithBitmap(bitmap)

            val shortcutId = "clonespace_${targetUserId}_$packageName"
            val shortcutTitle = "$appLabel (U$targetUserId)"

            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
                .setIcon(iconCompat)
                .setShortLabel(shortcutTitle)
                .setLongLabel("$shortcutTitle Workspace")
                .setIntent(launchIntent)
                .build()

            val success = ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
            if (success) Result.success(Unit) else Result.failure(IllegalStateException("Shortcut request rejected"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun updateAppMembership(packageName: String, userId: Int, isAdded: Boolean) {
        val currentList = _appsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            val item = currentList[index]
            val updatedProfiles = if (isAdded) {
                item.installedProfileIds + userId
            } else {
                item.installedProfileIds - userId
            }
            currentList[index] = item.copy(installedProfileIds = updatedProfiles)
            _appsFlow.value = currentList
        }
    }

    private fun createIconThumbnail(drawable: Drawable): Bitmap {
        val size = 72 // 72x72 px = only ~20 KB per icon
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
