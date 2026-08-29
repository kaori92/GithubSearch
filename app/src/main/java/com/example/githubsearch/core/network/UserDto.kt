package com.example.githubsearch.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val login: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)
