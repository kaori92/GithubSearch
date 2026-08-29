package com.example.githubsearch.core.data

import com.example.githubsearch.core.di.IoDispatcher
import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.core.model.SearchItemOrder
import com.example.githubsearch.core.network.GithubSearchService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

internal const val PER_PAGE = 50
internal const val MAX_RESULTS = 50

class DefaultGithubSearchRepository @Inject constructor(
    private val service: GithubSearchService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val logger: SearchFailureLogger,
) : GithubSearchRepository {

    /**
     * Requests [PER_PAGE] users and [PER_PAGE] repositories in parallel, then returns the merged
     * list sorted by display name and capped at [MAX_RESULTS] items.
     */
    override suspend fun search(query: String): DataResult<List<SearchItem>> = try {
        // The try wraps withContext rather than sitting inside it: catching a failed `async` from
        // within the scope would still leave the scope cancelled and rethrow on exit.
        withContext(dispatcher) {
            val users = async { service.searchUsers(query, PER_PAGE) }
            val repositories = async { service.searchRepositories(query, PER_PAGE) }
            val merged = users.await().items.map { it.toDomain() } +
                repositories.await().items.map { it.toDomain() }
            DataResult.Success(merged.sortedWith(SearchItemOrder).take(MAX_RESULTS))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        DataResult.Failure(DataError.Network)
    } catch (e: HttpException) {
        logger.logUnexpectedFailure(query, e)
        DataResult.Failure(e.code().toDataError())
    } catch (e: SerializationException) {
        // GitHub sent something the DTOs don't model — most likely an API shape change.
        logger.logUnexpectedFailure(query, e)
        DataResult.Failure(DataError.Unknown)
    } catch (e: Exception) {
        // Deliberately broad so a bug here can't crash the search field for the user — but logged,
        // so it never becomes a silent "Unknown" with no way to find out what actually happened.
        logger.logUnexpectedFailure(query, e)
        DataResult.Failure(DataError.Unknown)
    }
}

private fun Int.toDataError(): DataError = when (this) {
    // GitHub answers an exhausted search quota with 403 as often as with 429.
    403, 429 -> DataError.RateLimited
    else -> DataError.Server(this)
}
