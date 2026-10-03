package com.clonespace.app.data.model

enum class ProfileType(val displayName: String, val badge: String) {
    OWNER_SYSTEM("System Owner (Main)", "Owner"),
    MANAGED_WORK("Managed / Work Profile", "Work"),
    OEM_CLONE("Dual App / OEM Clone", "Clone"),
    SECONDARY("Secondary User", "Secondary"),
    GUEST("Guest Profile", "Guest")
}

data class UserProfile(
    val id: Int,
    val name: String,
    val flags: Int,
    val type: ProfileType,
    val isRunning: Boolean,
    val isCurrent: Boolean = false
) {
    val isOwner: Boolean get() = id == 0

    companion object {
        private const val FLAG_PRIMARY = 0x00000001
        private const val FLAG_ADMIN = 0x00000002
        private const val FLAG_GUEST = 0x00000004
        private const val FLAG_RESTRICTED = 0x00000008
        private const val FLAG_INITIALIZED = 0x00000010
        private const val FLAG_MANAGED_PROFILE = 0x00000020
        private const val FLAG_DISABLED = 0x00000040
        private const val FLAG_QUIET_MODE = 0x00000080
        private const val FLAG_EPHEMERAL = 0x00000100
        private const val FLAG_DEMO = 0x00000200
        private const val FLAG_FULL = 0x00000400
        private const val FLAG_SYSTEM = 0x00000800
        private const val FLAG_PROFILE = 0x00001000

        // Common OEM clone flags/IDs (e.g., Xiaomi Dual Apps uses 999)
        private const val OEM_DUAL_APP_USER_ID = 999

        fun resolveType(id: Int, name: String, flags: Int): ProfileType {
            if (id == 0) return ProfileType.OWNER_SYSTEM
            if (id == OEM_DUAL_APP_USER_ID || name.contains("clone", ignoreCase = true) || name.contains("dual", ignoreCase = true)) {
                return ProfileType.OEM_CLONE
            }
            if ((flags and FLAG_MANAGED_PROFILE) != 0 || name.contains("work", ignoreCase = true)) {
                return ProfileType.MANAGED_WORK
            }
            if ((flags and FLAG_GUEST) != 0 || name.contains("guest", ignoreCase = true)) {
                return ProfileType.GUEST
            }
            return ProfileType.SECONDARY
        }
    }
}
