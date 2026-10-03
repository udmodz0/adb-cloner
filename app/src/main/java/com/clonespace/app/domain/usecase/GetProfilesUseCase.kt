package com.clonespace.app.domain.usecase

import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class GetProfilesUseCase(private val repository: ProfileRepository) {
    val profilesFlow: Flow<List<UserProfile>> = repository.profilesFlow

    suspend operator fun invoke(): Result<List<UserProfile>> {
        return repository.refreshProfiles()
    }
}
