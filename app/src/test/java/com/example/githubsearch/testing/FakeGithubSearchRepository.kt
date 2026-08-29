package com.example.githubsearch.testing

import com.example.githubsearch.core.data.DataResult
import com.example.githubsearch.core.data.GithubSearchRepository
import com.example.githubsearch.core.model.SearchItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

class FakeGithubSearchRepository : GithubSearchRepository {

    val queries = mutableListOf<String>()
    val cancelledQueries = mutableListOf<String>()

    val responses = mutableMapOf<String, DataResult<List<SearchItem>>>()
    var defaultResponse: DataResult<List<SearchItem>> = DataResult.Success(emptyList())

    private val gates = mutableMapOf<String, CompletableDeferred<Unit>>()

    /** Makes [search] hang for [query] until [release], so a test can leave a call in flight. */
    fun hangOn(query: String) {
        gates[query] = CompletableDeferred()
    }

    fun release(query: String) {
        gates.remove(query)?.complete(Unit)
    }

    override suspend fun search(query: String): DataResult<List<SearchItem>> {
        queries += query
        try {
            gates[query]?.await()
        } catch (e: CancellationException) {
            cancelledQueries += query
            throw e
        }
        return responses[query] ?: defaultResponse
    }
}
