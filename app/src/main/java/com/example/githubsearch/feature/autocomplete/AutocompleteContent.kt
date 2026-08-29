package com.example.githubsearch.feature.autocomplete

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.ui.theme.GithubSearchTheme

/**
 * Feature-level content: unpacks [AutocompleteUiState] for the reusable component. Takes no
 * ViewModel, so previews and UI tests drive it with plain values.
 */
@Composable
fun AutocompleteContent(
    uiState: AutocompleteUiState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (SearchItem) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    GithubAutocomplete(
        query = uiState.query,
        results = uiState.results,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onQueryChange = onQueryChange,
        modifier = modifier,
        onItemClick = onItemClick,
        onRetry = onRetry,
    )
}

internal val sampleResults = listOf(
    SearchItem.Repository(
        id = 1,
        name = "coroutines",
        ownerLogin = "Kotlin",
        description = "Library support for Kotlin coroutines",
        htmlUrl = "https://github.com/Kotlin/kotlinx.coroutines",
        avatarUrl = null,
    ),
    SearchItem.User(
        id = 2,
        login = "kotlin-dev",
        htmlUrl = "https://github.com/kotlin-dev",
        avatarUrl = null,
    ),
    SearchItem.Repository(
        id = 3,
        name = "serialization",
        ownerLogin = "Kotlin",
        description = "Kotlin multiplatform / multi-format serialization",
        htmlUrl = "https://github.com/Kotlin/kotlinx.serialization",
        avatarUrl = null,
    ),
)

@Preview(name = "Idle", showBackground = true)
@Composable
private fun IdlePreview() = PreviewSurface(AutocompleteUiState(query = "ko"))

@Preview(name = "Loading", showBackground = true)
@Composable
private fun LoadingPreview() =
    PreviewSurface(AutocompleteUiState(query = "kotlin", isLoading = true))

@Preview(name = "Empty", showBackground = true)
@Composable
private fun EmptyPreview() = PreviewSurface(AutocompleteUiState(query = "zzzqqq"))

@Preview(name = "Error", showBackground = true)
@Composable
private fun ErrorPreview() = PreviewSurface(
    AutocompleteUiState(query = "kotlin", error = AutocompleteError.RateLimited),
)

@Preview(name = "Populated", showBackground = true)
@Composable
private fun PopulatedPreview() =
    PreviewSurface(AutocompleteUiState(query = "kotlin", results = sampleResults))

@Composable
private fun PreviewSurface(uiState: AutocompleteUiState) {
    GithubSearchTheme {
        AutocompleteContent(
            uiState = uiState,
            onQueryChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
