package com.clonespace.app.domain.usecase

import com.clonespace.app.data.repository.ProfileRepository

class ManageProfileStateUseCase(private val repository: ProfileRepository) {

    suspend fun start(userId: Int): Result<Unit> {
        return repository.startProfile(userId)
    }

    suspend fun stop(userId: Int): Result<Unit> {
        if (userId == 0) {
            return Result.failure(IllegalArgumentException("Safety guard: Cannot freeze or stop User 0 (Primary User)."))
        }
        return repository.stopProfile(userId)
    }

    suspend fun delete(userId: Int): Result<Unit> {
        if (userId == 0) {
            return Result.failure(IllegalArgumentException("Safety guard: Cannot delete User 0 (Primary User)."))
        }
        return repository.deleteProfile(userId)
    }
}
