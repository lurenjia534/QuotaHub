package com.lurenjia534.quotahub.data.update

data class AvailableUpdate(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val releaseUrl: String
)

class UpdateChecker(
    private val releaseService: GitHubReleaseService = GitHubReleaseApiClient.releaseService
) {
    suspend fun checkForUpdate(currentVersionName: String): Result<AvailableUpdate?> {
        return runCatching {
            val release = releaseService.getLatestRelease()
            release.toAvailableUpdateIfNewerThan(currentVersionName)
        }
    }

    private fun GitHubReleaseResponse.toAvailableUpdateIfNewerThan(
        currentVersionName: String
    ): AvailableUpdate? {
        if (draft || prerelease) {
            return null
        }

        val releaseVersion = tagName.toVersionParts()
        val currentVersion = currentVersionName.toVersionParts()
        if (releaseVersion <= currentVersion) {
            return null
        }

        return AvailableUpdate(
            tagName = tagName,
            versionName = tagName.removePrefix("v").removePrefix("V"),
            title = name?.takeIf { it.isNotBlank() } ?: tagName,
            releaseNotes = body?.takeIf { it.isNotBlank() } ?: "No release notes provided.",
            releaseUrl = htmlUrl
        )
    }
}

internal fun String.toVersionParts(): List<Int> {
    return trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('.', '-', '_')
        .mapNotNull { part ->
            part.takeWhile { it.isDigit() }
                .takeIf { it.isNotBlank() }
                ?.toIntOrNull()
        }
        .ifEmpty { listOf(0) }
}

internal operator fun List<Int>.compareTo(other: List<Int>): Int {
    val maxSize = maxOf(size, other.size)
    for (index in 0 until maxSize) {
        val left = getOrElse(index) { 0 }
        val right = other.getOrElse(index) { 0 }
        if (left != right) {
            return left.compareTo(right)
        }
    }
    return 0
}
