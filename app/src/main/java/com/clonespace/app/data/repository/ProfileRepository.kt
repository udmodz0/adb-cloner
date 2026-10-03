package com.clonespace.app.data.repository

import com.clonespace.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profilesFlow: Flow<List<UserProfile>>
    suspend fun refreshProfiles(): Result<List<UserProfile>>
    suspend fun createWorkspace(name: String, isManaged: Boolean): Result<UserProfile>
    suspend fun startProfile(userId: Int): Result<Unit>
    suspend fun stopProfile(userId: Int): Result<Unit>
    suspend fun deleteProfile(userId: Int): Result<Unit>
}
