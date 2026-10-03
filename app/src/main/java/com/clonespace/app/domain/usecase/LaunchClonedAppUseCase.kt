package com.clonespace.app.domain.usecase

import com.clonespace.app.data.repository.AppRepository

class LaunchClonedAppUseCase(private val repository: AppRepository) {
    suspend operator fun invoke(targetUserId: Int, packageName: String): Result<Unit> {
        return repository.launchApp(targetUserId, packageName)
    }
}
