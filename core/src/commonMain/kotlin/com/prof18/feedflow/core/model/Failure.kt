package com.prof18.feedflow.core.model

sealed class Failure

sealed class NetworkFailure : Failure() {
    data object NoConnection : NetworkFailure()

    data object ServerFailure : NetworkFailure()

    data object Redirect : NetworkFailure()

    data object Unauthorised : NetworkFailure()

    data object NetworkError : NetworkFailure()

    data object UnhandledNetworkFailure : NetworkFailure()

    data object BadToken : NetworkFailure()
}

data object DataNotFound : Failure()

data object InvalidData : Failure()

data object Unknown : Failure()

data class Unhandled(val exception: Throwable) : Failure()
