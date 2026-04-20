package com.lurenjia534.quotahub.data.model

data class QuotaSnapshot(
    val fetchedAt: Long,
    val resources: List<QuotaResource>
) {
    companion object {
        fun empty(): QuotaSnapshot = QuotaSnapshot(
            fetchedAt = 0L,
            resources = emptyList()
        )
    }
}

data class QuotaResource(
    val key: String,
    val title: String,
    val type: ResourceType,
    val role: ResourceRole? = null,
    val bucket: String? = null,
    val windows: List<QuotaWindow>
)

enum class ResourceType {
    Model,
    Plan,
    Feature
}

data class QuotaWindow(
    val windowKey: String,
    val scope: WindowScope,
    val label: String? = null,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetAtEpochMillis: Long?,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val unit: QuotaUnit = QuotaUnit.Request
)

enum class WindowScope {
    Interval,
    Daily,
    Weekly,
    Monthly,
    Rolling
}

enum class QuotaUnit {
    Request,
    Token,
    Credit,
    Minute,
    Percent
}

enum class ResourceRole {
    Limit,
    Contributor,
    Sampled,
    Anchor
}
