package com.example.githubsearch.feature.autocomplete

import com.example.githubsearch.core.model.SearchItem

/** Below this many characters the screen stays idle: no request, no error. */
const val MIN_QUERY_LENGTH = 3

/** The one definition of "long enough to search", shared by the state and the stateless UI. */
fun isSearchable(query: String): Boolean = query.trim().length >= MIN_QUERY_LENGTH

data class AutocompleteUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchItem> = emptyList(),
    val error: AutocompleteError? = null,
) {
    val canSearch: Boolean get() = isSearchable(query)

    val showIdleHint: Boolean get() = !canSearch

    val showEmptyResults: Boolean
        get() = canSearch && !isLoading && error == null && results.isEmpty()
}
