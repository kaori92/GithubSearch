package com.example.githubsearch.feature.autocomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubsearch.core.data.DataResult
import com.example.githubsearch.core.data.GithubSearchRepository
import com.example.githubsearch.core.model.SearchItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val DEBOUNCE_MILLIS = 300L

@HiltViewModel
class AutocompleteViewModel @Inject constructor(
    private val repository: GithubSearchRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val retries = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(AutocompleteUiState())
    val uiState: StateFlow<AutocompleteUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AutocompleteEffect>(Channel.BUFFERED)
    val effects: Flow<AutocompleteEffect> = _effects.receiveAsFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResults: Flow<DataResult<List<SearchItem>>?> =
        combine(
            query.map { it.trim() }.distinctUntilChanged().debounce(DEBOUNCE_MILLIS),
            retries,
        ) { trimmed, _ -> trimmed }
            .flatMapLatest { trimmed ->
                // null stands for "below the threshold": idle, and no request goes out.
                if (!isSearchable(trimmed)) flowOf(null)
                else flow { emit(repository.search(trimmed)) }
            }

    init {
        viewModelScope.launch {
            searchResults.collect { result ->
                _uiState.update { it.applying(result.toOutcome()) }
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        // Loading starts the moment the query is long enough, so the debounce window reads as
        // "searching" rather than briefly as "no results".
        val outcome = if (isSearchable(value)) SearchOutcome.Loading else SearchOutcome.Idle
        _uiState.update { it.copy(query = value).applying(outcome) }
    }

    fun onRetry() {
        if (!_uiState.value.canSearch) return
        _uiState.update { it.applying(SearchOutcome.Loading) }
        retries.update { it + 1 }
    }

    fun onItemClick(item: SearchItem) {
        _effects.trySend(AutocompleteEffect.OpenUrl(item.htmlUrl))
    }
}

/**
 * A phase of the search pipeline. [AutocompleteUiState.applying] is the one place that says what
 * each phase does to the visible state, so the synchronous updates in [AutocompleteViewModel] and
 * the asynchronous ones from [AutocompleteViewModel.searchResults] can't drift apart from each
 * other.
 */
private sealed interface SearchOutcome {
    data object Idle : SearchOutcome
    data object Loading : SearchOutcome
    data class Success(val results: List<SearchItem>) : SearchOutcome
    data class Failure(val error: AutocompleteError) : SearchOutcome
}

private fun DataResult<List<SearchItem>>?.toOutcome(): SearchOutcome = when (this) {
    null -> SearchOutcome.Idle
    is DataResult.Success -> SearchOutcome.Success(data)
    is DataResult.Failure -> SearchOutcome.Failure(error.toAutocompleteError())
}

private fun AutocompleteUiState.applying(outcome: SearchOutcome): AutocompleteUiState =
    when (outcome) {
        SearchOutcome.Idle -> copy(isLoading = false, results = emptyList(), error = null)
        SearchOutcome.Loading -> copy(isLoading = true, error = null)
        is SearchOutcome.Success -> copy(isLoading = false, results = outcome.results, error = null)
        is SearchOutcome.Failure -> copy(isLoading = false, results = emptyList(), error = outcome.error)
    }
