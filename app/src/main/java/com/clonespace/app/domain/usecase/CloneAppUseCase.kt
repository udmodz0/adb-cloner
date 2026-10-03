package com.clonespace.app.domain.usecase

import android.content.Context
import com.clonespace.app.data.repository.AppRepository

class CloneAppUseCase(private val repository: AppRepository) {

    suspend fun cloneSingle(targetUserId: Int, packageName: String): Result<Unit> {
        return repository.cloneApp(targetUserId, packageName)
    }

    suspend fun cloneBatch(
        targetUserId: Int,
        packageNames: List<String>,
        onProgress: (current: Int, total: Int, packageName: String) -> Unit
    ): Result<Int> {
        return repository.batchCloneApps(targetUserId, packageNames, onProgress)
    }

    suspend fun removeClone(targetUserId: Int, packageName: String): Result<Unit> {
        return repository.removeClone(targetUserId, packageName)
    }

    fun pinShortcut(
        context: Context,
        targetUserId: Int,
        packageName: String,
        appLabel: String
    ): Result<Unit> {
        return repository.createHomeScreenShortcut(context, targetUserId, packageName, appLabel)
    }
}
