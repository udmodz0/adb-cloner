package com.clonespace.app.data.repository

import android.content.Context
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val appsFlow: Flow<List<AppCloneItem>>

    suspend fun refreshApps(
        context: Context,
        profiles: List<UserProfile>,
        includeSystemApps: Boolean
    ): Result<List<AppCloneItem>>

    suspend fun cloneApp(
        targetUserId: Int,
        packageName: String
    ): Result<Unit>

    suspend fun batchCloneApps(
        targetUserId: Int,
        packageNames: List<String>,
        onProgress: (current: Int, total: Int, packageName: String) -> Unit
    ): Result<Int>

    suspend fun removeClone(
        targetUserId: Int,
        packageName: String
    ): Result<Unit>

    suspend fun launchApp(
        targetUserId: Int,
        packageName: String
    ): Result<Unit>

    fun createHomeScreenShortcut(
        context: Context,
        targetUserId: Int,
        packageName: String,
        appLabel: String
    ): Result<Unit>
}
