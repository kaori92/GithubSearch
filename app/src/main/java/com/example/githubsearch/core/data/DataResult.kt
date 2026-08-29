package com.example.githubsearch.core.data

/** Result of a data-layer call. Separate from [kotlin.Result], which can only carry a [Throwable]. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}
