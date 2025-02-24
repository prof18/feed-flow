@file:OptIn(ExperimentalContracts::class)

package com.prof18.feedflow.core.model

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(val failure: Failure) : DataResult<Nothing>()
}

fun <T> DataResult<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is DataResult.Success)
    }
    return this is DataResult.Success
}

fun <T> DataResult<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is DataResult.Error)
    }
    return this is DataResult.Error
}

inline fun <reified T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> {
    if (isError()) {
        return this
    }

    val data = this.requireSuccess()
    val mappedData = transform(data)
    return DataResult.Success(mappedData)
}

inline fun <reified T> DataResult<T>.requireSuccess(): T {
    require(this is DataResult.Success) {
        "DataResult should be a success"
    }
    return this.data
}

fun <T> DataResult<T>.requireError(): Failure {
    require(this is DataResult.Error) {
        "DataResult should be a success"
    }
    return this.failure
}

operator fun <T> DataResult<T>.plus(otherResult: DataResult<T>): List<DataResult<T>> {
    listOf<String>() + ""
    return listOf(this, otherResult)
}

operator fun <T> List<DataResult<T>>.plus(otherResult: DataResult<T>): List<DataResult<T>> {
    return toMutableList().apply {
        add(otherResult)
    }
}

fun <T> DataResult<T>.ignoreResultOnSuccess(): DataResult<Unit> {
    return when (this) {
        is DataResult.Success -> DataResult.Success(Unit)
        is DataResult.Error -> this
    }
}

fun <T> DataResult<T>.onError(
    onError: (Failure) -> Unit,
) {
    if (this is DataResult.Error) {
        onError(this.failure)
    }
}

suspend fun <T> DataResult<T>.onErrorSuspend(
    onError: suspend (Failure) -> Unit,
) {
    if (this is DataResult.Error) {
        onError(this.failure)
    }
}

inline fun <T> List<DataResult<T>>.doOnError(
    onError: (Failure) -> Unit,
) {
    val firstThrowable = this.filterIsInstance<DataResult.Error>()
        .map { it.failure }
        .firstOrNull()
    if (firstThrowable != null) {
        onError(firstThrowable)
    }
}

fun <T> DataResult<T>.onSuccess(
    onSuccess: (T) -> Unit,
) {
    if (this is DataResult.Success) {
        onSuccess(this.data)
    }
}

suspend fun <T> DataResult<T>.onSuccessSuspend(
    onSuccess: suspend (T) -> Unit,
) {
    if (this is DataResult.Success) {
        onSuccess(this.data)
    }
}

fun <T> List<DataResult<T>>.doOnEveryError(onEveryError: (Failure) -> Unit) {
    this.forEach {
        if (it is DataResult.Error) {
            onEveryError(it.failure)
        }
    }
}

fun <T> List<DataResult<T>>.firstError() =
    this.filterIsInstance<DataResult.Error>()
        .map { it.failure }
        .firstOrNull()

fun <T> List<DataResult<T>>.allSuccess(): Boolean {
    return all { it is DataResult.Success }
}

fun <T> List<DataResult<T>>.allErrors(): Boolean {
    return all { it is DataResult.Error }
}

fun Failure.error(): DataResult<Nothing> = DataResult.Error(this)

fun <T> T.success(): DataResult<T> = DataResult.Success(this)

inline fun <R, T> DataResult<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (failure: Failure) -> R,
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when {
        isSuccess() -> onSuccess(this.data)
        isError() -> onFailure(failure)
        else -> onFailure(Unhandled(Throwable("Unhandled DataResult")))
    }
}
