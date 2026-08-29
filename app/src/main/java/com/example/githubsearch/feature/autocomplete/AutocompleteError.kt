package com.example.githubsearch.feature.autocomplete

import com.example.githubsearch.core.data.DataError

/** What the UI needs to know about a failure; the string lives in the composable. */
sealed interface AutocompleteError {
    data object Network : AutocompleteError
    data object RateLimited : AutocompleteError
    data class Server(val code: Int) : AutocompleteError
}

internal fun DataError.toAutocompleteError(): AutocompleteError = when (this) {
    DataError.Network -> AutocompleteError.Network
    DataError.RateLimited -> AutocompleteError.RateLimited
    is DataError.Server -> AutocompleteError.Server(code)
    DataError.Unknown -> AutocompleteError.Server(code = 0)
}
