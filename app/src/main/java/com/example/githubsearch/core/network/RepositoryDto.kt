package com.example.githubsearch.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val owner: RepositoryOwnerDto? = null,
)
