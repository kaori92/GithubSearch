package com.example.githubsearch.core.network

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto<T>(
    val items: List<T> = emptyList(),
)
