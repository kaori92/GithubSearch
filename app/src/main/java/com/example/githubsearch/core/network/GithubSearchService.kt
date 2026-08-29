package com.example.githubsearch.core.network

import retrofit2.http.GET
import retrofit2.http.Query

interface GithubSearchService {

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("per_page") perPage: Int,
    ): SearchResponseDto<UserDto>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("per_page") perPage: Int,
    ): SearchResponseDto<RepositoryDto>
}
