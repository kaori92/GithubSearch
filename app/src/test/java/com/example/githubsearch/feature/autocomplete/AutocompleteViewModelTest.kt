package com.example.githubsearch.feature.autocomplete

import app.cash.turbine.test
import com.example.githubsearch.core.data.DataError
import com.example.githubsearch.core.data.DataResult
import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.testing.FakeGithubSearchRepository
import com.example.githubsearch.testing.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AutocompleteViewModelTest {

    // One dispatcher instance for both the rule and runTest: two schedulers would make
    // advanceUntilIdle move virtual time for only half of the pipeline.
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val repository = FakeGithubSearchRepository()

    @Test
    fun `queries shorter than the minimum never reach the repository`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQueryChange("k")
        viewModel.onQueryChange("ko")
        advanceUntilIdle()

        assertTrue(repository.queries.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.showIdleHint)
    }

    @Test
    fun `whitespace is trimmed before the length check`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onQueryChange(" k ")
        advanceUntilIdle()
        assertTrue(repository.queries.isEmpty())

        viewModel.onQueryChange("  kotlin  ")
        advanceUntilIdle()
        assertEquals(listOf("kotlin"), repository.queries)
    }

    @Test
    fun `a burst of keystrokes produces a single request for the last query`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChange("kot")
            viewModel.onQueryChange("kotl")
            viewModel.onQueryChange("kotli")
            viewModel.onQueryChange("kotlin")
            advanceUntilIdle()

            assertEquals(listOf("kotlin"), repository.queries)
        }

    @Test
    fun `a newer query cancels the in-flight one and only the newer result is shown`() =
        runTest(dispatcher) {
            val stale = user(id = 1, login = "stale")
            val fresh = user(id = 2, login = "fresh")
            repository.hangOn("kotlin")
            repository.responses["kotlin"] = DataResult.Success(listOf(stale))
            repository.responses["kotlinx"] = DataResult.Success(listOf(fresh))
            val viewModel = viewModel()

            viewModel.onQueryChange("kotlin")
            advanceUntilIdle()
            assertEquals(listOf("kotlin"), repository.queries)

            viewModel.onQueryChange("kotlinx")
            advanceUntilIdle()
            repository.release("kotlin")
            advanceUntilIdle()

            assertEquals(listOf("kotlin"), repository.cancelledQueries)
            assertEquals(listOf(fresh), viewModel.uiState.value.results)
            // The cancelled call must not read as a failure.
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `the third character starts the loading state before the request goes out`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.onQueryChange("ko")
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.onQueryChange("kot")
            assertTrue(viewModel.uiState.value.isLoading)
            assertTrue(repository.queries.isEmpty())
        }

    @Test
    fun `a successful search exposes results and stops loading`() = runTest(dispatcher) {
        val item = user(id = 1, login = "octocat")
        repository.responses["octocat"] = DataResult.Success(listOf(item))
        val viewModel = viewModel()

        viewModel.onQueryChange("octocat")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(item), state.results)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("octocat", state.query)
    }

    @Test
    fun `a successful search with no matches is an empty state, not an error`() =
        runTest(dispatcher) {
            repository.defaultResponse = DataResult.Success(emptyList())
            val viewModel = viewModel()

            viewModel.onQueryChange("zzzzzz")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.showEmptyResults)
            assertNull(state.error)
        }

    @Test
    fun `a failure maps to a ui error and clears loading`() = runTest(dispatcher) {
        repository.defaultResponse = DataResult.Failure(DataError.RateLimited)
        val viewModel = viewModel()

        viewModel.onQueryChange("kotlin")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AutocompleteError.RateLimited, state.error)
        assertFalse(state.isLoading)
        assertFalse(state.showEmptyResults)
    }

    @Test
    fun `retry re-runs the current query`() = runTest(dispatcher) {
        repository.defaultResponse = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()

        viewModel.onQueryChange("kotlin")
        advanceUntilIdle()
        assertEquals(AutocompleteError.Network, viewModel.uiState.value.error)

        repository.defaultResponse = DataResult.Success(listOf(user(id = 1, login = "kotlin")))
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(listOf("kotlin", "kotlin"), repository.queries)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clicking an item emits a single open url effect`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val item = user(id = 1, login = "octocat")

        viewModel.effects.test {
            viewModel.onItemClick(item)
            assertEquals(AutocompleteEffect.OpenUrl(item.htmlUrl), awaitItem())
            expectNoEvents()
        }
    }

    private fun viewModel() = AutocompleteViewModel(repository)

    private fun user(id: Long, login: String) = SearchItem.User(
        id = id,
        login = login,
        htmlUrl = "https://github.com/$login",
        avatarUrl = null,
    )
}
