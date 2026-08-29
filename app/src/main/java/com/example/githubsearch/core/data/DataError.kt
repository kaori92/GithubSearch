package com.example.githubsearch.core.data

/** Every failure the data layer can surface. Platform exceptions stop at the repository. */
sealed interface DataError {
    /** No usable connection, DNS failure, timeout. */
    data object Network : DataError

    /** GitHub search quota exhausted (HTTP 403 with the rate-limit headers, or 429). */
    data object RateLimited : DataError

    data class Server(val code: Int) : DataError

    data object Unknown : DataError
}
