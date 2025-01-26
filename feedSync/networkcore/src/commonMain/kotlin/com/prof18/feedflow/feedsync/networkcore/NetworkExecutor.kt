package com.prof18.feedflow.feedsync.networkcore

import com.prof18.feedflow.core.model.DataNotFound
import com.prof18.feedflow.core.model.DataResult
import com.prof18.feedflow.core.model.NetworkFailure
import com.prof18.feedflow.core.model.Unhandled
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.isSuccess
import io.ktor.util.reflect.typeInfo

suspend inline fun <reified A> executeNetwork(
    crossinline block: suspend () -> HttpResponse,
): DataResult<A> {
    try {
        val response = block()

        val isBadToken = response
            .headers["X-Reader-Google-Bad-Token"]
            .orEmpty()
            .toBoolean()

        if (isBadToken) {
            return DataResult.Error(NetworkFailure.BadToken)
        }

        if (!response.status.isSuccess()) {
            return when (response.status) {
                Unauthorized -> {
                    DataResult.Error(NetworkFailure.Unauthorised)
                }
                NotFound -> DataResult.Error(DataNotFound)
                ServiceUnavailable, InternalServerError -> DataResult.Error(NetworkFailure.ServerFailure)
                else -> DataResult.Error(NetworkFailure.UnhandledNetworkFailure)
            }
        }

        val res: A = response.call.bodyNullable(typeInfo<A>()) as A ?: return DataResult.Error(DataNotFound)

        return DataResult.Success(res)
    } catch (e: Throwable) {
        return if (e.isMissingConnectionError()) {
            DataResult.Error(NetworkFailure.NoConnection)
        } else {
            DataResult.Error(Unhandled(e))
        }
    }
}

expect fun Throwable.isMissingConnectionError(): Boolean
