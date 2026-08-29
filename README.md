# GithubSearch

Autocomplete over the GitHub search API: users and repositories fetched in parallel, merged into
one alphabetically sorted list (50 per request, capped at 50 after the merge), starting at 3
characters with a 300 ms debounce and latest-wins cancellation.

**Optional token** — unauthenticated search is limited to ~10 requests/minute. Add to
`local.properties` (never committed) and rebuild:

    GITHUB_TOKEN=ghp_yourtokenhere

**Tests**

    ./gradlew :app:testDebugUnitTest    # DefaultGithubSearchRepositoryTest, AutocompleteViewModelTest
    ./gradlew :app:connectedDebugAndroidTest   # GithubAutocompleteTest (needs a device/emulator)

## How the code is tested

Three layers, three styles:

- **Repository** (`DefaultGithubSearchRepositoryTest`) — plain JVM unit tests against a
  hand-written `FakeGithubSearchService`, no mocking framework. Covers the merge/sort/cap logic,
  every `DataError` mapping, and that cancellation propagates as `CancellationException` rather
  than turning into a swallowed error.
- **ViewModel** (`AutocompleteViewModelTest`) — a single `StandardTestDispatcher` instance feeds
  both `runTest(dispatcher)` and a `MainDispatcherRule`, so virtual time advances consistently
  across the whole debounce → flatMapLatest → repository pipeline. Effects are asserted with
  Turbine.
- **Compose UI** (`GithubAutocompleteTest`) — instrumented tests that drive `GithubAutocomplete`
  with hoisted state and plain lambdas, no ViewModel involved, selecting nodes by text/content
  description/click-action the way TalkBack or a real user would.

Fakes over mocks throughout: `FakeGithubSearchService` and `FakeGithubSearchRepository` are small
hand-rolled classes with recorded calls and controllable responses, rather than a mocking
framework — no unmockable Android classes to work around, and the tests keep the ergonomics of
"assert what actually got called" instead of "assert what was stubbed."

Here's the one that best shows the debounce/cancellation behavior end to end — a burst of
keystrokes, an in-flight request that hangs, and a proof that only the latest query's result
survives:

```kotlin
@Test
fun `a newer query cancels the in-flight one and only the newer result is shown`() =
    runTest(dispatcher) {
        val stale = user(id = 1, login = "stale")
        val fresh = user(id = 2, login = "fresh")
        repository.hangOn("kotlin")
        repository.responses["kotlin"] = DataResult.Success(listOf(stale))
        repository.responses["kotlinx"] = DataResult.Success(listOf(fresh))
        val viewModel = viewModel()

        viewModel.onQueryChange("kotlin")
        advanceUntilIdle()
        assertEquals(listOf("kotlin"), repository.queries)

        viewModel.onQueryChange("kotlinx")
        advanceUntilIdle()
        repository.release("kotlin")
        advanceUntilIdle()

        assertEquals(listOf("kotlin"), repository.cancelledQueries)
        assertEquals(listOf(fresh), viewModel.uiState.value.results)
        // The cancelled call must not read as a failure.
        assertNull(viewModel.uiState.value.error)
    }
```

`repository.hangOn("kotlin")` suspends the fake mid-request until `release` is called, which is
what lets the test simulate a slow first request that's still in flight when a second, different
query arrives — the assertion on `cancelledQueries` confirms `flatMapLatest` actually cancelled it
rather than letting both results race.

## Manual test cases

Automated coverage is above; these are the cases worth walking through by hand on a device or
emulator before a release, since they exercise real network timing, real GitHub responses, and
real system behavior (screen rotation, TalkBack, airplane mode) that fakes can't fully stand in
for.

| # | Case | Steps | Expected result |
|---|------|-------|------------------|
| 1 | Below the search threshold | Open the screen; type 1–2 characters (e.g. `oc`). | The idle hint is shown ("type at least 3 characters" or similar); no request is made; no result rows appear. |
| 2 | Crossing the threshold starts loading immediately | Type a 3rd character (e.g. `oct`). | A progress indicator appears right away, before the 300 ms debounce window elapses. |
| 3 | Debounce coalesces a fast burst | Type a full query quickly, letter by letter (e.g. `octocat`), without pausing. | Only one network round-trip happens, for the final text — not one per keystroke. (Watch network activity, e.g. via `adb logcat` or Android Studio's Network Inspector.) |
| 4 | Successful search shows mixed results | Search a common term with both user and repo matches (e.g. `kotlin`). | Rows for both users and repositories appear, alphabetically merged by display name, each labeled with its type. |
| 5 | No matches | Search a query unlikely to match anything (e.g. `asdkjhqwekjhasdlkj`). | An empty-state message naming the query is shown, not an error and not a blank screen. |
| 6 | Leading/trailing whitespace is trimmed | Type `  kotlin  ` with extra spaces. | Behaves identically to searching `kotlin` — same results, no separate request for the untrimmed text. |
| 7 | A newer query supersedes an older one | Type `kot`, pause briefly past the debounce, then before results finish loading keep typing to `kotlin`. | Only the results for `kotlin` are ever shown; the app never flashes stale results for `kot`. |
| 8 | Network error + retry | Turn on airplane mode, then search a valid query. | An error message appears with a retry button. Turn airplane mode back off and tap retry — results load normally. |
| 9 | Rate limiting | Without a `GITHUB_TOKEN` configured, perform ~12+ searches within a minute. | Eventually a rate-limit-specific error message appears (distinct from the generic network error), with a working retry. |
| 10 | Clearing the query | With text entered, tap the field's clear ("×") button. | The field empties, the result list clears, and the view returns to the idle-hint state. |
| 11 | Tapping a result opens it | Search any query with results, tap a user row and separately a repository row. | Each tap opens that GitHub user's or repository's page in the browser. |
| 12 | List cap at 50 | Search a very common term likely to exceed 50 combined matches (e.g. `a`). | The list never exceeds 50 rows, and scrolling is smooth (no jank) through the full list. |
| 13 | Screen rotation mid-search | Start a search, then rotate the device while it's still loading (or right after results appear). | State survives rotation — no crash, no reset to idle, no duplicate request fired. |
| 14 | TalkBack accessibility | Enable TalkBack, swipe through a results list. | Each row announces once as a single unit (avatar + name + subtitle + type), not as several separate stops per row. |
| 15 | Keyboard search action | With a query typed, tap the keyboard's search/IME action instead of waiting. | No crash and no duplicate request; behavior is unaffected since search already runs on debounce. |
