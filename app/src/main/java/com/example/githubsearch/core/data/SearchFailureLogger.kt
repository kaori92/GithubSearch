package com.example.githubsearch.core.data

/**
 * Seam for reporting a search failure the repository couldn't otherwise explain — a malformed
 * response, or any other exception the catch-all in [DefaultGithubSearchRepository] wasn't
 * written to expect. Kept as its own interface (rather than calling `android.util.Log` directly)
 * so the repository has no Android framework dependency and stays a plain JVM unit under test.
 */
fun interface SearchFailureLogger {
    fun logUnexpectedFailure(query: String, throwable: Throwable)
}
