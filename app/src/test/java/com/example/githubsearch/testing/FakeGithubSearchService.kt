package com.example.githubsearch.testing

import com.example.githubsearch.core.network.GithubSearchService
import com.example.githubsearch.core.network.RepositoryDto
import com.example.githubsearch.core.network.SearchResponseDto
import com.example.githubsearch.core.network.UserDto
import kotlinx.coroutines.awaitCancellation

class FakeGithubSearchService : GithubSearchService {

    var users: List<UserDto> = emptyList()
    var repositories: List<RepositoryDto> = emptyList()
    var failWith: Throwable? = null

    /** When true both endpoints hang, so a test can observe what cancellation does to a call. */
    var suspendForever: Boolean = false

    val recordedPerPage = mutableListOf<Int>()

    override suspend fun searchUsers(query: String, perPage: Int): SearchResponseDto<UserDto> {
        recordedPerPage += perPage
        if (suspendForever) awaitCancellation()
        failWith?.let { throw it }
        return SearchResponseDto(users)
    }

    override suspend fun searchRepositories(
        query: String,
        perPage: Int,
    ): SearchResponseDto<RepositoryDto> {
        recordedPerPage += perPage
        if (suspendForever) awaitCancellation()
        failWith?.let { throw it }
        return SearchResponseDto(repositories)
    }
}
