package com.lurenjia534.quotahub.data.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun checkForUpdate_returnsReleaseWhenLatestVersionIsNewer() = runBlocking {
        val checker = UpdateChecker(
            releaseService = FakeReleaseService(
                GitHubReleaseResponse(
                    tagName = "v1.2",
                    name = "v1.2",
                    body = "Release notes",
                    htmlUrl = "https://github.com/lurenjia534/QuotaHub/releases/tag/v1.2"
                )
            )
        )

        val update = checker.checkForUpdate("1.1").getOrThrow()

        requireNotNull(update)
        assertEquals("v1.2", update.tagName)
        assertEquals("1.2", update.versionName)
        assertEquals("Release notes", update.releaseNotes)
    }

    @Test
    fun checkForUpdate_returnsNullWhenLatestVersionIsCurrentVersion() = runBlocking {
        val checker = UpdateChecker(
            releaseService = FakeReleaseService(
                GitHubReleaseResponse(
                    tagName = "v1.1",
                    htmlUrl = "https://github.com/lurenjia534/QuotaHub/releases/tag/v1.1"
                )
            )
        )

        assertNull(checker.checkForUpdate("1.1").getOrThrow())
    }

    @Test
    fun checkForUpdate_ignoresDraftAndPrereleaseVersions() = runBlocking {
        val draftChecker = UpdateChecker(
            releaseService = FakeReleaseService(
                GitHubReleaseResponse(
                    tagName = "v1.2",
                    htmlUrl = "https://github.com/lurenjia534/QuotaHub/releases/tag/v1.2",
                    draft = true
                )
            )
        )
        val prereleaseChecker = UpdateChecker(
            releaseService = FakeReleaseService(
                GitHubReleaseResponse(
                    tagName = "v1.2",
                    htmlUrl = "https://github.com/lurenjia534/QuotaHub/releases/tag/v1.2",
                    prerelease = true
                )
            )
        )

        assertNull(draftChecker.checkForUpdate("1.1").getOrThrow())
        assertNull(prereleaseChecker.checkForUpdate("1.1").getOrThrow())
    }

    @Test
    fun toVersionParts_parsesVersionTags() {
        assertEquals(listOf(1, 2, 3), "v1.2.3".toVersionParts())
        assertEquals(listOf(1, 10), "1.10".toVersionParts())
        assertEquals(listOf(2, 0), "V2.0-beta".toVersionParts())
    }

    @Test
    fun versionComparison_handlesDifferentSegmentLengths() {
        assertTrue("1.10".toVersionParts() > "1.2".toVersionParts())
        assertTrue("1.1.1".toVersionParts() > "1.1".toVersionParts())
        assertEquals(0, "1.1".toVersionParts().compareTo("v1.1.0".toVersionParts()))
    }

    private class FakeReleaseService(
        private val response: GitHubReleaseResponse
    ) : GitHubReleaseService {
        override suspend fun getLatestRelease(): GitHubReleaseResponse {
            return response
        }
    }
}
