package com.clonespace.app.domain.usecase

import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.repository.ProfileRepository

class CreateProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String, isManaged: Boolean): Result<UserProfile> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Workspace name cannot be empty."))
        }
        return repository.createWorkspace(trimmed, isManaged)
    }
}
