package com.clonespace.app.data.model

import android.graphics.Bitmap

data class AppCloneItem(
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val installedProfileIds: Set<Int> = emptySet(),
    val iconBitmap: Bitmap? = null
) {
    fun isInstalledIn(profileId: Int): Boolean = installedProfileIds.contains(profileId)
}
