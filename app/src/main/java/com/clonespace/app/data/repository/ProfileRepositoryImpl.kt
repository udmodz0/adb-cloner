package com.clonespace.app.data.repository

import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.shizuku.ShellCommander
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

class ProfileRepositoryImpl : ProfileRepository {

    private val _profilesFlow = MutableStateFlow<List<UserProfile>>(emptyList())
    override val profilesFlow: Flow<List<UserProfile>> = _profilesFlow.asStateFlow()

    // Example line: UserInfo{0:Owner:13} running  OR  UserInfo{10:Work Profile:30}
    private val userInfoPattern = Pattern.compile(
        """UserInfo\{(\d+):([^:]+):([0-9a-fA-F]+)\}(?:\s*(running))?"""
    )

    override suspend fun refreshProfiles(): Result<List<UserProfile>> {
        val result = ShellCommander.execute("pm list users")
        return result.mapCatching { output ->
            val parsedList = parseProfiles(output)
            _profilesFlow.value = parsedList
            parsedList
        }
    }

    override suspend fun createWorkspace(name: String, isManaged: Boolean): Result<UserProfile> {
        val sanitizedName = name.replace("\"", "\\\"").trim()
        val command = if (isManaged) {
            "pm create-user --profileOf 0 --managed \"$sanitizedName\""
        } else {
            "pm create-user \"$sanitizedName\""
        }

        val result = ShellCommander.execute(command)
        return result.mapCatching { output ->
            // Output usually: "Success: created user id 10" or similar
            val idMatch = Regex("""(?:created user id|user id)\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(output)
            val newUserId = idMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: throw IllegalStateException("Could not parse user ID from output: $output")

            // Auto start the newly created profile as per spec
            ShellCommander.execute("am start-user $newUserId")

            // Refresh profiles
            refreshProfiles()

            _profilesFlow.value.find { it.id == newUserId }
                ?: UserProfile(
                    id = newUserId,
                    name = sanitizedName,
                    flags = if (isManaged) 0x30 else 0,
                    type = if (isManaged) com.clonespace.app.data.model.ProfileType.MANAGED_WORK else com.clonespace.app.data.model.ProfileType.SECONDARY,
                    isRunning = true
                )
        }
    }

    override suspend fun startProfile(userId: Int): Result<Unit> {
        val result = ShellCommander.execute("am start-user $userId")
        return result.mapCatching {
            refreshProfiles()
            Unit
        }
    }

    override suspend fun stopProfile(userId: Int): Result<Unit> {
        if (userId == 0) {
            return Result.failure(IllegalArgumentException("Security Guard: User 0 (Primary system owner) cannot be stopped."))
        }
        val result = ShellCommander.execute("am stop-user -f $userId")
        return result.mapCatching {
            refreshProfiles()
            Unit
        }
    }

    override suspend fun deleteProfile(userId: Int): Result<Unit> {
        if (userId == 0) {
            return Result.failure(IllegalArgumentException("Security Guard: User 0 (Primary system owner) cannot be deleted."))
        }
        val result = ShellCommander.execute("pm remove-user $userId")
        return result.mapCatching {
            refreshProfiles()
            Unit
        }
    }

    private fun parseProfiles(output: String): List<UserProfile> {
        val profiles = mutableListOf<UserProfile>()
        val lines = output.lines()

        for (line in lines) {
            val matcher = userInfoPattern.matcher(line)
            if (matcher.find()) {
                val idStr = matcher.group(1) ?: continue
                val name = matcher.group(2) ?: "Profile $idStr"
                val flagsStr = matcher.group(3) ?: "0"
                val runningStr = matcher.group(4)

                val id = idStr.toIntOrNull() ?: continue
                val flags = flagsStr.toIntOrNull(16) ?: flagsStr.toIntOrNull() ?: 0
                val isRunning = runningStr?.equals("running", ignoreCase = true) == true || id == 0
                val profileType = UserProfile.resolveType(id, name, flags)

                profiles.add(
                    UserProfile(
                        id = id,
                        name = name,
                        flags = flags,
                        type = profileType,
                        isRunning = isRunning,
                        isCurrent = (id == 0)
                    )
                )
            }
        }

        // Always ensure User 0 is present if for any reason parsing missed it
        if (profiles.none { it.id == 0 }) {
            profiles.add(
                0,
                UserProfile(
                    id = 0,
                    name = "Owner",
                    flags = 0x13,
                    type = com.clonespace.app.data.model.ProfileType.OWNER_SYSTEM,
                    isRunning = true,
                    isCurrent = true
                )
            )
        }

        return profiles.sortedBy { it.id }
    }
}
