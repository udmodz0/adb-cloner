package com.clonespace.app.data.repository

import com.clonespace.app.data.model.ProfileType
import com.clonespace.app.data.model.UserProfile
import com.clonespace.app.data.shizuku.ShellCommander
import kotlinx.coroutines.delay
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
        val dumpsysOutput = ShellCommander.execute("dumpsys user").getOrNull().orEmpty()
        return result.mapCatching { output ->
            val parsedList = parseProfiles(output, dumpsysOutput)
            _profilesFlow.value = parsedList
            parsedList
        }
    }

    override suspend fun createWorkspace(name: String, profileType: ProfileType): Result<UserProfile> {
        val sanitizedName = name.replace("\"", "\\\"").trim()
        val command = when (profileType) {
            ProfileType.CLONE, ProfileType.OEM_CLONE ->
                "pm create-user --profileOf 0 --user-type ${UserProfile.USER_TYPE_PROFILE_CLONE} \"$sanitizedName\""
            ProfileType.MANAGED_WORK ->
                "pm create-user --profileOf 0 --managed \"$sanitizedName\""
            else ->
                "pm create-user \"$sanitizedName\""
        }

        val result = ShellCommander.execute(command)
        return result.mapCatching { output ->
            // Output usually: "Success: created user id 10" or similar
            val idMatch = Regex("""(?:created user id|user id)\s*(\d+)""", RegexOption.IGNORE_CASE)
                .find(output)
            val newUserId = idMatch?.groupValues?.get(1)?.toIntOrNull()
                ?: throw IllegalStateException("Could not parse user ID from output: $output")

            // Auto start the newly created profile and unlock credentials if locked
            startAndUnlockUser(newUserId)

            // Refresh profiles
            refreshProfiles()

            val defaultFlags = when (profileType) {
                ProfileType.CLONE, ProfileType.OEM_CLONE -> 0x1010
                ProfileType.MANAGED_WORK -> 0x1030
                else -> 0x410
            }

            _profilesFlow.value.find { it.id == newUserId }
                ?: UserProfile(
                    id = newUserId,
                    name = sanitizedName,
                    flags = defaultFlags,
                    type = profileType,
                    isRunning = true,
                    isUnlocked = true,
                    userType = when (profileType) {
                        ProfileType.CLONE, ProfileType.OEM_CLONE -> UserProfile.USER_TYPE_PROFILE_CLONE
                        ProfileType.MANAGED_WORK -> UserProfile.USER_TYPE_PROFILE_MANAGED
                        else -> null
                    }
                )
        }
    }

    override suspend fun startProfile(userId: Int): Result<Unit> {
        val result = startAndUnlockUser(userId)
        return result.mapCatching {
            refreshProfiles()
            Unit
        }
    }

    private suspend fun startAndUnlockUser(userId: Int): Result<Unit> {
        val startResult = ShellCommander.execute("am start-user $userId")
        if (startResult.isFailure) {
            return startResult.map { Unit }
        }

        if (userId != 0) {
            val state = queryUserState(userId)
            if (!state.contains("RUNNING_UNLOCKED", ignoreCase = true)) {
                ShellCommander.execute(
                    "am start --user $userId -a android.app.action.CONFIRM_DEVICE_CREDENTIAL"
                )
                refreshProfiles()

                // Poll briefly while the user completes credential confirmation
                repeat(60) {
                    delay(400)
                    val currentState = queryUserState(userId)
                    if (currentState.contains("RUNNING_UNLOCKED", ignoreCase = true)) {
                        return Result.success(Unit)
                    }
                }
            }
        }
        return Result.success(Unit)
    }

    private suspend fun queryUserState(userId: Int): String {
        return ShellCommander.execute("am get-started-user-state $userId")
            .getOrNull()
            ?.trim()
            .orEmpty()
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

    private data class DumpsysUserMeta(
        val userType: String? = null,
        val state: String? = null
    )

    private fun parseDumpsysUserMetadata(dumpsysOutput: String): Map<Int, DumpsysUserMeta> {
        if (dumpsysOutput.isBlank()) return emptyMap()
        val metaMap = mutableMapOf<Int, DumpsysUserMeta>()
        var currentUserId: Int? = null

        for (rawLine in dumpsysOutput.lines()) {
            val trimmed = rawLine.trim()
            val matcher = userInfoPattern.matcher(trimmed)
            if (matcher.find()) {
                currentUserId = matcher.group(1)?.toIntOrNull()
                if (currentUserId != null && !metaMap.containsKey(currentUserId)) {
                    metaMap[currentUserId] = DumpsysUserMeta()
                }
                continue
            }

            if (currentUserId != null) {
                when {
                    trimmed.startsWith("Type:") -> {
                        val typeVal = trimmed.removePrefix("Type:").trim()
                        val existing = metaMap[currentUserId] ?: DumpsysUserMeta()
                        metaMap[currentUserId] = existing.copy(userType = typeVal)
                    }
                    trimmed.startsWith("State:") -> {
                        val stateVal = trimmed.removePrefix("State:").trim()
                        val existing = metaMap[currentUserId] ?: DumpsysUserMeta()
                        metaMap[currentUserId] = existing.copy(state = stateVal)
                    }
                    trimmed.startsWith("User types:") || trimmed.startsWith("Device properties:") -> {
                        currentUserId = null
                    }
                }
            }
        }
        return metaMap
    }

    private fun parseProfiles(output: String, dumpsysOutput: String = ""): List<UserProfile> {
        val profiles = mutableListOf<UserProfile>()
        val dumpsysMeta = parseDumpsysUserMetadata(dumpsysOutput)
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
                val meta = dumpsysMeta[id]
                val isRunning = runningStr?.equals("running", ignoreCase = true) == true ||
                    meta?.state?.startsWith("RUNNING", ignoreCase = true) == true ||
                    id == 0
                val isUnlocked = if (id == 0) {
                    true
                } else if (!isRunning) {
                    false
                } else {
                    meta?.state?.equals("RUNNING_UNLOCKED", ignoreCase = true) ?: true
                }
                val profileType = UserProfile.resolveType(id, name, flags, meta?.userType)

                profiles.add(
                    UserProfile(
                        id = id,
                        name = name,
                        flags = flags,
                        type = profileType,
                        isRunning = isRunning,
                        isCurrent = (id == 0),
                        isUnlocked = isUnlocked,
                        userType = meta?.userType
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
                    type = ProfileType.OWNER_SYSTEM,
                    isRunning = true,
                    isCurrent = true,
                    isUnlocked = true
                )
            )
        }

        return profiles.sortedBy { it.id }
    }
}
