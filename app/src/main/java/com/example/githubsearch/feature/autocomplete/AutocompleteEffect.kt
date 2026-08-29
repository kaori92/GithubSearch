package com.example.githubsearch.feature.autocomplete

/** One-shot imperatives. Anything the screen must still show after a rotation belongs in state. */
sealed interface AutocompleteEffect {
    data class OpenUrl(val url: String) : AutocompleteEffect
}
