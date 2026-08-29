package com.example.githubsearch.core.data

import android.util.Log
import javax.inject.Inject

class LogcatSearchFailureLogger @Inject constructor() : SearchFailureLogger {

    override fun logUnexpectedFailure(query: String, throwable: Throwable) {
        Log.e(TAG, "Unexpected failure searching GitHub for \"$query\"", throwable)
    }

    private companion object {
        const val TAG = "GithubSearchRepository"
    }
}
