package com.prof18.feedflow.feedsync.nextcloud

sealed class NextcloudException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {

    class AuthenticationException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)

    class NetworkException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)

    class FileNotFoundException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)

    class UploadException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)

    class DownloadException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)

    class ServerException(message: String? = null, cause: Throwable? = null) :
        NextcloudException(message, cause)
}
