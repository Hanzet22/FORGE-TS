package com.HG.Forge.Ts.data.network

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class PublicRepository(
    val fullName: String,
    val description: String,
    val htmlUrl: String,
    val stars: Int,
    val language: String,
    val license: String
)

data class RepositorySearchResult(
    val query: String,
    val totalCount: Int,
    val repositories: List<PublicRepository>
)

class RepositoryExplorerRepository {
    suspend fun search(queryInput: String): RepositorySearchResult = withContext(Dispatchers.IO) {
        val query = queryInput.trim()
        require(query.length in 2..100) { "Enter a search term between 2 and 100 characters" }
        require(query.none { it == '\n' || it == '\r' }) { "Search term contains an invalid line break" }

        val response = withTimeout(10_000) {
            HttpClientProvider.client.get("https://api.github.com/search/repositories") {
                parameter("q", query)
                parameter("sort", "stars")
                parameter("order", "desc")
                parameter("per_page", 15)
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
        }

        if (response.status.value == 403 || response.status.value == 429) {
            throw IllegalStateException("GitHub API rate limit reached. Wait before searching again.")
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("GitHub returned HTTP ${response.status.value}")
        }

        val json = JSONObject(response.bodyAsText())
        val items = json.optJSONArray("items")
        val repositories = buildList {
            if (items != null) {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val licenseObject = item.optJSONObject("license")
                    add(
                        PublicRepository(
                            fullName = item.optString("full_name"),
                            description = item.optString("description").takeUnless { it == "null" }.orEmpty(),
                            htmlUrl = item.optString("html_url"),
                            stars = item.optInt("stargazers_count"),
                            language = item.optString("language").takeUnless { it == "null" }.orEmpty(),
                            license = licenseObject?.optString("spdx_id").orEmpty().ifBlank { "Not specified" }
                        )
                    )
                }
            }
        }
        RepositorySearchResult(
            query = query,
            totalCount = json.optInt("total_count"),
            repositories = repositories
        )
    }
}
