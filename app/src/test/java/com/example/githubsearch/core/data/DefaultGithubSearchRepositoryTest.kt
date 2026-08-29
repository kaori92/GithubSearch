package com.example.githubsearch.core.data

import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.core.network.RepositoryDto
import com.example.githubsearch.core.network.RepositoryOwnerDto
import com.example.githubsearch.core.network.UserDto
import com.example.githubsearch.testing.FakeGithubSearchService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DefaultGithubSearchRepositoryTest {

    private val service = FakeGithubSearchService()

    @Test
    fun `merges users and repositories into one alphabetically sorted list`() = runTest {
        service.users = listOf(user(id = 1, login = "zeta"), user(id = 2, login = "Beta"))
        service.repositories = listOf(repo(id = 3, name = "alpha"), repo(id = 4, name = "Gamma"))

        val result = repository().search("query").successData()

        assertEquals(listOf("alpha", "Beta", "Gamma", "zeta"), result.map(SearchItem::sortKey))
    }

    @Test
    fun `breaks name ties by type then id so order is deterministic`() = runTest {
        service.users = listOf(user(id = 9, login = "kotlin"), user(id = 2, login = "kotlin"))
        service.repositories = listOf(repo(id = 1, name = "Kotlin"))

        val result = repository().search("kotlin").successData()

        assertEquals(listOf(2L, 9L, 1L), result.map(SearchItem::id))
        assertTrue(result[0] is SearchItem.User)
        assertTrue(result[2] is SearchItem.Repository)
    }

    @Test
    fun `requests 50 per endpoint and caps the merged list at 50`() = runTest {
        service.users = List(50) { user(id = it.toLong(), login = "user%02d".format(it)) }
        service.repositories = List(50) { repo(id = 100L + it, name = "repo%02d".format(it)) }

        val result = repository().search("query").successData()

        assertEquals(listOf(50, 50), service.recordedPerPage)
        assertEquals(50, result.size)
        // Sort happens before the cap: every "repo.." name sorts ahead of every "user..", so the
        // 50 surviving items are exactly the repositories.
        assertEquals("repo00", result.first().sortKey)
        assertEquals("repo49", result.last().sortKey)
    }

    @Test
    fun `maps connection failures to a network error`() = runTest {
        service.failWith = IOException("offline")

        assertEquals(DataResult.Failure(DataError.Network), repository().search("query"))
    }

    @Test
    fun `maps 403 and 429 to a rate limit error`() = runTest {
        service.failWith = httpException(403)
        assertEquals(DataResult.Failure(DataError.RateLimited), repository().search("query"))

        service.failWith = httpException(429)
        assertEquals(DataResult.Failure(DataError.RateLimited), repository().search("query"))
    }

    @Test
    fun `maps other http failures to a server error carrying the code`() = runTest {
        service.failWith = httpException(500)

        assertEquals(DataResult.Failure(DataError.Server(500)), repository().search("query"))
    }

    @Test
    fun `an unexpected failure maps to Unknown and is reported instead of swallowed`() = runTest {
        val logger = RecordingSearchFailureLogger()
        val unexpected = IllegalStateException("mapper bug")
        service.failWith = unexpected

        val result = repository(logger).search("kotlin")

        assertEquals(DataResult.Failure(DataError.Unknown), result)
        // Not assertEquals(listOf("kotlin" to unexpected), ...): kotlinx.coroutines recovers stack
        // traces on JVM by copying the exception across each suspension point it crosses (here,
        // the async/await boundary), so the Throwable the logger receives is type-and-message
        // equal to `unexpected` but not the same instance. Compare on that instead of identity.
        val (loggedQuery, loggedThrowable) = logger.failures.single()
        assertEquals("kotlin", loggedQuery)
        assertEquals(unexpected::class, loggedThrowable::class)
        assertEquals(unexpected.message, loggedThrowable.message)
    }

    @Test
    fun `exposes domain models only`() = runTest {
        service.users = listOf(user(id = 7, login = "octocat"))
        service.repositories = listOf(repo(id = 8, name = "hello-world", owner = "octocat"))

        val result = repository().search("octocat").successData()

        assertEquals(
            SearchItem.User(
                id = 7,
                login = "octocat",
                htmlUrl = "https://github.com/octocat",
                avatarUrl = "https://avatars.example/7",
            ),
            result.single { it is SearchItem.User },
        )
        assertEquals(
            SearchItem.Repository(
                id = 8,
                name = "hello-world",
                ownerLogin = "octocat",
                description = "a repo",
                htmlUrl = "https://github.com/octocat/hello-world",
                avatarUrl = "https://avatars.example/octocat",
            ),
            result.single { it is SearchItem.Repository },
        )
    }

    @Test
    fun `cancelling the caller makes search rethrow CancellationException rather than returning an error`() =
        runTest {
            service.suspendForever = true
            val repository = repository()
            var thrown: Throwable? = null

            val job = launch {
                try {
                    repository.search("kotlin")
                } catch (e: CancellationException) {
                    thrown = e
                    throw e
                }
            }
            advanceUntilIdle()
            job.cancelAndJoin()

            assertTrue(
                "expected search() to rethrow CancellationException, but caught $thrown",
                thrown is CancellationException,
            )
        }

    private fun TestScope.repository(logger: SearchFailureLogger = SearchFailureLogger { _, _ -> }) =
        DefaultGithubSearchRepository(service, StandardTestDispatcher(testScheduler), logger)

    private fun DataResult<List<SearchItem>>.successData(): List<SearchItem> {
        assertTrue("expected success but was $this", this is DataResult.Success)
        return (this as DataResult.Success).data
    }

    private fun user(id: Long, login: String) = UserDto(
        id = id,
        login = login,
        htmlUrl = "https://github.com/$login",
        avatarUrl = "https://avatars.example/$id",
    )

    private fun repo(id: Long, name: String, owner: String = "owner") = RepositoryDto(
        id = id,
        name = name,
        description = "a repo",
        htmlUrl = "https://github.com/$owner/$name",
        owner = RepositoryOwnerDto(login = owner, avatarUrl = "https://avatars.example/$owner"),
    )

    private fun httpException(code: Int) = HttpException(
        Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType())),
    )

    private class RecordingSearchFailureLogger : SearchFailureLogger {
        val failures = mutableListOf<Pair<String, Throwable>>()

        override fun logUnexpectedFailure(query: String, throwable: Throwable) {
            failures += query to throwable
        }
    }
}
