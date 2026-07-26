package com.songsit.fuellogpro.domain

enum class SyncDecision { UPLOAD, DOWNLOAD, UNCHANGED, CONFLICT }

fun decideSync(
    localExists: Boolean,
    cloudExists: Boolean,
    contentEqual: Boolean = false,
): SyncDecision = when {
    localExists && !cloudExists -> SyncDecision.UPLOAD
    !localExists && cloudExists -> SyncDecision.DOWNLOAD
    localExists && cloudExists && contentEqual -> SyncDecision.UNCHANGED
    localExists && cloudExists -> SyncDecision.CONFLICT
    else -> SyncDecision.UNCHANGED
}
