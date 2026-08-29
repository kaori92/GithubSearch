package com.example.githubsearch.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryOwnerDto(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
