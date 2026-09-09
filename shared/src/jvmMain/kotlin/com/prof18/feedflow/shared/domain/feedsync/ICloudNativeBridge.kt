package com.prof18.feedflow.shared.domain.feedsync

interface ICloudFileTransfer {
    fun uploadToICloud(isDebug: Boolean): Int
    fun downloadToFile(isDebug: Boolean, destinationPath: String): Int
}

class ICloudNativeBridge : ICloudFileTransfer {
    /**
     * Result:
     *   0 -> success
     *   1 -> iCloud folder URL null
     *   2 -> upload error
     */
    external override fun uploadToICloud(isDebug: Boolean): Int

    /**
     * Result:
     *  0 -> success
     *  1 -> url null
     *  3 -> download error
     *  5 -> local file not found (remote absence is unconfirmed)
     *  6 -> completed cloud discovery found no backup
     */
    external override fun downloadToFile(isDebug: Boolean, destinationPath: String): Int
}

@Suppress("MagicNumber")
enum class UploadResult(val code: Int) {
    SUCCESS(0),
    ICLOUD_FOLDER_URL_NULL(1),
    UPLOAD_ERROR(2),
    UNKNOWN_ERROR(-1),
    ;

    companion object {
        fun fromCode(code: Int): UploadResult {
            return entries.find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}

@Suppress("MagicNumber")
enum class DownloadResult(val code: Int) {
    SUCCESS(0),
    URL_NULL(1),
    TEMP_URL_NULL(2),
    DOWNLOAD_ERROR(3),
    DATABASE_REPLACE_ERROR(4),
    FILE_NOT_FOUND(5),
    REMOTE_FILE_NOT_FOUND(6),
    UNKNOWN_ERROR(-1),
    ;

    companion object {
        fun fromCode(code: Int): DownloadResult {
            return entries.find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}
