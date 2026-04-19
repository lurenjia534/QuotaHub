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
    val windows: List<QuotaWindow>
)

enum class ResourceType {
    Model,
    Plan,
    Feature
}

data class QuotaWindow(
    val scope: WindowScope,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetsAt: Long?,
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
    Minute
}
