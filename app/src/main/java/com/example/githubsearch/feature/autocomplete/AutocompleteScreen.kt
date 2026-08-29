package com.example.githubsearch.feature.autocomplete

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubsearch.R

/** Host screen: the only place that knows about the ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteScreen(
    modifier: Modifier = Modifier,
    viewModel: AutocompleteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AutocompleteEffect.OpenUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.autocomplete_title)) }) },
    ) { innerPadding ->
        AutocompleteContent(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            modifier = Modifier.padding(innerPadding),
            onItemClick = viewModel::onItemClick,
            onRetry = viewModel::onRetry,
        )
    }
}
