package com.example.githubsearch.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decodes fixtures shaped like GitHub's real search responses through the same [Json] config the
 * app ships (see NetworkModule.provideJson).
 */
class GithubSearchServiceJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a users search response`() {
        val body = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "login": "octocat",
                  "id": 583231,
                  "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4",
                  "html_url": "https://github.com/octocat",
                  "score": 1.0
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString(
            SearchResponseDto.serializer(UserDto.serializer()),
            body,
        )

        assertEquals(
            listOf(
                UserDto(
                    id = 583231,
                    login = "octocat",
                    htmlUrl = "https://github.com/octocat",
                    avatarUrl = "https://avatars.githubusercontent.com/u/583231?v=4",
                ),
            ),
            response.items,
        )
    }

    @Test
    fun `decodes a repositories search response`() {
        val body = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "id": 132935648,
                  "name": "hello-world",
                  "full_name": "octocat/hello-world",
                  "private": false,
                  "description": "My first repository on GitHub!",
                  "html_url": "https://github.com/octocat/hello-world",
                  "score": 1.0,
                  "owner": {
                    "login": "octocat",
                    "id": 583231,
                    "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4"
                  }
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString(
            SearchResponseDto.serializer(RepositoryDto.serializer()),
            body,
        )

        assertEquals(
            listOf(
                RepositoryDto(
                    id = 132935648,
                    name = "hello-world",
                    description = "My first repository on GitHub!",
                    htmlUrl = "https://github.com/octocat/hello-world",
                    owner = RepositoryOwnerDto(
                        login = "octocat",
                        avatarUrl = "https://avatars.githubusercontent.com/u/583231?v=4",
                    ),
                ),
            ),
            response.items,
        )
    }

    @Test
    fun `a repository with no owner in the payload decodes rather than failing`() {
        val body = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {"id": 1, "name": "orphaned", "html_url": "https://github.com/x/orphaned"}
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString(
            SearchResponseDto.serializer(RepositoryDto.serializer()),
            body,
        )

        assertEquals(null, response.items.single().owner)
    }

    @Test
    fun `unknown fields on the response are ignored, not fatal`() {
        val body =
            """{"total_count":0,"incomplete_results":false,"items":[],"unexpected_new_field":true}"""

        val response = json.decodeFromString(SearchResponseDto.serializer(UserDto.serializer()), body)

        assertEquals(emptyList<UserDto>(), response.items)
    }
}
