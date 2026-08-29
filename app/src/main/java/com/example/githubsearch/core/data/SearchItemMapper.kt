package com.example.githubsearch.core.data

import com.example.githubsearch.core.model.SearchItem
import com.example.githubsearch.core.network.RepositoryDto
import com.example.githubsearch.core.network.UserDto

internal fun UserDto.toDomain() = SearchItem.User(
    id = id,
    login = login,
    htmlUrl = htmlUrl,
    avatarUrl = avatarUrl,
)

internal fun RepositoryDto.toDomain() = SearchItem.Repository(
    id = id,
    name = name,
    ownerLogin = owner?.login,
    description = description,
    htmlUrl = htmlUrl,
    avatarUrl = owner?.avatarUrl,
)
