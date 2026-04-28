package com.lurenjia534.quotahub.data.update

import retrofit2.http.GET

interface GitHubReleaseService {
    @GET("repos/lurenjia534/QuotaHub/releases/latest")
    suspend fun getLatestRelease(): GitHubReleaseResponse
}
