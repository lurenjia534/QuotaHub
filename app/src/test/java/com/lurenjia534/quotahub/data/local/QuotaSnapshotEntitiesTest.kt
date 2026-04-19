package com.lurenjia534.quotahub.data.local

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotaSnapshotEntitiesTest {
    @Test
    fun toEntitiesAndBack_preservesMultipleWindowsPerScope() {
        val snapshot = QuotaSnapshot(
            fetchedAt = 1_234L,
            resources = listOf(
                QuotaResource(
                    key = "shared-pool",
                    title = "Shared pool",
                    type = ResourceType.Feature,
                    windows = listOf(
                        QuotaWindow(
                            windowKey = "rolling-3h",
                            scope = WindowScope.Rolling,
                            total = 300,
                            used = 120,
                            remaining = 180,
                            resetAtEpochMillis = 3_600_000L,
                            startsAt = 0L,
                            endsAt = 3_600_000L,
                            unit = QuotaUnit.Credit
                        ),
                        QuotaWindow(
                            windowKey = "rolling-24h",
                            scope = WindowScope.Rolling,
                            total = 1_200,
                            used = 400,
                            remaining = 800,
                            resetAtEpochMillis = 86_400_000L,
                            startsAt = 0L,
                            endsAt = 86_400_000L,
                            unit = QuotaUnit.Credit
                        )
                    )
                )
            )
        )

        val entities = snapshot.toEntities(
            subscriptionId = 42L,
            rawPayloadJson = """{"ok":true}""",
            rawPayloadFormat = "test.payload.v1",
            normalizerVersion = 7
        )

        assertEquals(listOf("rolling-3h", "rolling-24h"), entities.windows.map { it.windowKey })
        assertEquals(listOf(WindowScope.Rolling.name, WindowScope.Rolling.name), entities.windows.map { it.scope })
        assertEquals(listOf(3_600_000L, 86_400_000L), entities.windows.map { it.resetAtEpochMillis })

        val restored = buildRows(entities).toQuotaSnapshot()

        assertEquals(snapshot, restored)
    }

    private fun buildRows(entities: QuotaSnapshotEntities): List<QuotaSnapshotRow> {
        val snapshot = entities.snapshot
        return entities.resources.flatMap { resource ->
            entities.windows
                .filter { it.subscriptionId == resource.subscriptionId && it.resourceKey == resource.resourceKey }
                .map { window ->
                    QuotaSnapshotRow(
                        snapshotSubscriptionId = snapshot.subscriptionId,
                        fetchedAt = snapshot.fetchedAt,
                        resourceKey = resource.resourceKey,
                        title = resource.title,
                        resourceType = resource.type,
                        resourceDisplayOrder = resource.displayOrder,
                        windowKey = window.windowKey,
                        scope = window.scope,
                        windowDisplayOrder = window.displayOrder,
                        total = window.total,
                        used = window.used,
                        remaining = window.remaining,
                        resetAtEpochMillis = window.resetAtEpochMillis,
                        startsAt = window.startsAt,
                        endsAt = window.endsAt,
                        unit = window.unit
                    )
                }
        }
    }
}
