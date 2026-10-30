package com.prof18.feedflow.feedsync.googledrive

object GoogleDriveBackupResolver {
    fun resolve(candidates: List<GoogleDriveBackupCandidate>): GoogleDriveBackupResolution? {
        val kept = candidates.sortedWith(
            compareByDescending<GoogleDriveBackupCandidate> { it.modifiedTimeMillis }.thenBy { it.id },
        ).firstOrNull() ?: return null
        return GoogleDriveBackupResolution(
            keptId = kept.id,
            orphanIds = candidates.map { it.id }.filter { it != kept.id }.distinct().sorted(),
        )
    }

    fun orphanName(fileName: String, fileId: String): String = "$fileName$ORPHAN_SUFFIX$fileId"
}

data class GoogleDriveBackupCandidate(
    val id: String,
    val modifiedTimeMillis: Long,
)

data class GoogleDriveBackupResolution(
    val keptId: String,
    val orphanIds: List<String>,
)

private const val ORPHAN_SUFFIX = ".orphan-"
