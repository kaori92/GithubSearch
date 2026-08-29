package com.example.githubsearch.core.model

/**
 * One row of the merged autocomplete list.
 *
 * [sortKey] is the display name the merged list is ordered by — a user's `login`, a repository's
 * short `name` — so the two result types can be sorted against each other without the sorter
 * knowing which is which.
 */
sealed interface SearchItem {
    val id: Long
    val sortKey: String
    val htmlUrl: String
    val avatarUrl: String?

    data class User(
        override val id: Long,
        val login: String,
        override val htmlUrl: String,
        override val avatarUrl: String?,
    ) : SearchItem {
        override val sortKey: String get() = login
    }

    data class Repository(
        override val id: Long,
        val name: String,
        val ownerLogin: String?,
        val description: String?,
        override val htmlUrl: String,
        override val avatarUrl: String?,
    ) : SearchItem {
        override val sortKey: String get() = name
    }
}
