package com.example.githubsearch.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * GitHub rejects requests without a `User-Agent`. The token is optional: search is a public
 * endpoint, so a missing token means the lower anonymous rate limit rather than a hard failure.
 */
class GithubHeaderInterceptor(private val token: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val USER_AGENT = "GithubSearch-Android"
    }
}
