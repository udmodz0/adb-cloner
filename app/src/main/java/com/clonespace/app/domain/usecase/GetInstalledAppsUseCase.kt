package com.clonespace.app.domain.usecase

import android.content.Context
import com.clonespace.app.data.model.AppCloneItem
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow

class GetInstalledAppsUseCase(private val repository: AppRepository) {
    val appsFlow: Flow<List<AppCloneItem>> = repository.appsFlow

    suspend operator fun invoke(
        context: Context,
        profiles: List<UserProfile>,
        includeSystemApps: Boolean
    ): Result<List<AppCloneItem>> {
        return repository.refreshApps(context, profiles, includeSystemApps)
    }
}
