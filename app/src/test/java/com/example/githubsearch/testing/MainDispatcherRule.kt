package com.example.githubsearch.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for the test's own dispatcher. Callers must hand the *same* instance to
 * `runTest`, otherwise the rule and the test body run on two schedulers and `advanceTimeBy` moves
 * only one of them.
 */
class MainDispatcherRule(private val dispatcher: TestDispatcher) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
