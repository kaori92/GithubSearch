package com.example.githubsearch.feature.autocomplete

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.githubsearch.R
import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.ui.theme.GithubSearchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the component with hoisted state and plain lambdas — no ViewModel involved. */
@RunWith(AndroidJUnit4::class)
class GithubAutocompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val user = SearchItem.User(
        id = 1,
        login = "octocat",
        htmlUrl = "https://github.com/octocat",
        avatarUrl = null,
    )
    private val repository = SearchItem.Repository(
        id = 2,
        name = "hello-world",
        ownerLogin = "octocat",
        description = "My first repository",
        htmlUrl = "https://github.com/octocat/hello-world",
        avatarUrl = null,
    )

    @Test
    fun belowTheMinimumLengthItShowsTheIdleHintAndNoRows() {
        setContent(query = "oc")

        composeTestRule
            .onNodeWithText(context.getString(R.string.autocomplete_idle_hint, MIN_QUERY_LENGTH))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("octocat").assertDoesNotExist()
    }

    @Test
    fun whileLoadingItShowsTheProgressIndicator() {
        setContent(query = "octo", isLoading = true)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.autocomplete_searching))
            .assertIsDisplayed()
    }

    @Test
    fun withNoMatchesItShowsTheEmptyMessage() {
        setContent(query = "octo", results = emptyList())

        composeTestRule
            .onNodeWithText(context.getString(R.string.autocomplete_empty, "octo"))
            .assertIsDisplayed()
    }

    @Test
    fun onErrorItShowsTheMessageAndRetryCallsBack() {
        var retries = 0
        setContent(
            query = "octo",
            error = AutocompleteError.RateLimited,
            onRetry = { retries++ },
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.autocomplete_error_rate_limited))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.autocomplete_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun withResultsItRendersBothRowTypes() {
        setContent(query = "octo", results = listOf(user, repository))

        composeTestRule.onNodeWithText("octocat").assertIsDisplayed()
        composeTestRule.onNodeWithText("hello-world").assertIsDisplayed()
        composeTestRule.onNodeWithText("My first repository").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.autocomplete_type_user))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.autocomplete_type_repository))
            .assertIsDisplayed()
    }

    @Test
    fun eachResultRowIsASingleMergedAccessibilityNode() {
        setContent(query = "octo", results = listOf(user, repository))

        // With descendant semantics merged, the node found by the headline text also carries the
        // row's click action — proof TalkBack sees one focus stop per row, not one per text child.
        composeTestRule.onNodeWithText("octocat").assert(hasClickAction())
        composeTestRule.onNodeWithText("hello-world").assert(hasClickAction())
    }

    @Test
    fun clickingAResultReportsTheItem() {
        val clicked = mutableListOf<SearchItem>()
        setContent(
            query = "octo",
            results = listOf(user, repository),
            onItemClick = clicked::add,
        )

        composeTestRule.onNodeWithText("hello-world").performClick()

        assertEquals(listOf<SearchItem>(repository), clicked)
    }

    private fun setContent(
        query: String,
        results: List<SearchItem> = emptyList(),
        isLoading: Boolean = false,
        error: AutocompleteError? = null,
        onQueryChange: (String) -> Unit = {},
        onItemClick: (SearchItem) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            GithubSearchTheme {
                GithubAutocomplete(
                    query = query,
                    results = results,
                    isLoading = isLoading,
                    error = error,
                    onQueryChange = onQueryChange,
                    onItemClick = onItemClick,
                    onRetry = onRetry,
                )
            }
        }
    }
}
