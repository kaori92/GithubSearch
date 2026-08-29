package com.example.githubsearch.core.data

import com.example.githubsearch.core.model.SearchItem

interface GithubSearchRepository {

    /**
     * Requests 50 users and 50 repositories in parallel, then returns the merged list sorted by
     * display name and capped at 50 items.
     */
    suspend fun search(query: String): DataResult<List<SearchItem>>
}
