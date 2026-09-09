package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.feedsync.dropbox.DropboxUploadConflictException

internal inline fun <T> retryDropboxConflicts(block: () -> T): T {
    var conflicts = 0
    while (true) {
        try {
            return block()
        } catch (error: DropboxUploadConflictException) {
            conflicts++
            if (conflicts == MAX_DROPBOX_UPLOAD_ATTEMPTS) throw error
        }
    }
}

private const val MAX_DROPBOX_UPLOAD_ATTEMPTS = 3
