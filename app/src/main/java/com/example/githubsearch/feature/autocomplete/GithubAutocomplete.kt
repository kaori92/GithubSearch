package com.example.githubsearch.feature.autocomplete

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.githubsearch.R
import com.example.githubsearch.core.model.SearchItem

/**
 * Stateless GitHub users + repositories autocomplete: search field, progress, and the merged
 * result list. Everything it shows is passed in, so it can be dropped onto any screen with
 * different callbacks and the caller's own [modifier]; it adds no outer padding of its own.
 */
@Composable
fun GithubAutocomplete(
    query: String,
    results: List<SearchItem>,
    isLoading: Boolean,
    error: AutocompleteError?,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (SearchItem) -> Unit = {},
    onRetry: () -> Unit = {},
    listContentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (SearchItem) -> Unit = { item ->
        SearchResultRow(item = item, onClick = { onItemClick(item) })
    },
) {
    val searchingDescription = stringResource(R.string.autocomplete_searching)

    Column(modifier = modifier) {
        SearchField(query = query, onQueryChange = onQueryChange)

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = searchingDescription },
            )
        }

        when {
            error != null -> ErrorState(error = error, onRetry = onRetry)

            !isSearchable(query) ->
                Message(stringResource(R.string.autocomplete_idle_hint, MIN_QUERY_LENGTH))

            results.isEmpty() && !isLoading ->
                Message(stringResource(R.string.autocomplete_empty, query.trim()))

            else -> LazyColumn(contentPadding = listContentPadding) {
                items(items = results, key = { it.stableKey }) { item -> itemContent(item) }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        label = { Text(stringResource(R.string.autocomplete_field_label)) },
        placeholder = { Text(stringResource(R.string.autocomplete_field_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.autocomplete_clear),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun ErrorState(
    error: AutocompleteError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (error) {
        AutocompleteError.Network -> R.string.autocomplete_error_network
        AutocompleteError.RateLimited -> R.string.autocomplete_error_rate_limited
        is AutocompleteError.Server -> R.string.autocomplete_error_server
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(stringResource(R.string.autocomplete_retry))
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    )
}

/** Users and repositories have independent id spaces, so the type has to be part of the key. */
private val SearchItem.stableKey: String
    get() = when (this) {
        is SearchItem.User -> "user-$id"
        is SearchItem.Repository -> "repo-$id"
    }
