package com.vitorpamplona.quartzPerfTest

import com.vitorpamplona.quartz.experimental.trustedAssertions.list.tags.ProviderTypes
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import com.vitorpamplona.quartz.nip01Core.store.sqlite.RawEvent
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartz.nip56Reports.ReportEvent
import com.vitorpamplona.quartz.utils.TimeUtils
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class QueryTester(val db: EventStore) {
    val followersFilter = Filter(
        kinds = listOf(ContactListEvent.KIND),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 500,
    )

    val notificationsFilter = Filter(
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 500,
    )

    val reportsFilter = Filter(
        kinds = listOf(ReportEvent.KIND),
        authors = listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c"),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 500,
    )

    val reportsByAnyoneFilter = Filter(
        kinds = listOf(ReportEvent.KIND),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 500,
    )

    val followsFilter = Filter(
        kinds = listOf(ContactListEvent.KIND),
        authors = listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c"),
    )

    val idsFilter = Filter(
        ids = listOf("0a2a28949c42599506cc48709fb60ac77dcda257b8f9a8ff46abfee76ea0c75f", "a80548db0e8f519dcb542e9f8f2818d22a7fd7ed7d0b5d366af53a1358510431")
    )

    val followersFilterByDate = Filter(
        kinds = listOf(ContactListEvent.KIND),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        since = 1764553447, // Nov 2025
    )

    fun run(): QueryResults {
        val follows = measureTimedValue {
            db.store.rawQuery(followsFilter)
        }

        val followerCount = measureTimedValue {
            db.count(followersFilter)
        }

        val followers = measureTimedValue {
            db.store.rawQuery(followersFilter)
        }

        val notifications = measureTimedValue {
            db.store.rawQuery(notificationsFilter)
        }

        val reports = measureTimedValue {
            db.store.rawQuery(reportsFilter)
        }

        val reportsByAnyone = measureTimedValue {
            db.store.rawQuery(reportsByAnyoneFilter)
        }

        val ids = measureTimedValue {
            db.store.rawQuery(idsFilter)
        }

        val followersFromLastMonth = measureTimedValue {
            db.store.rawQuery(followersFilterByDate)
        }

        return QueryResults(
            follows,
            followerCount,
            followers,
            notifications,
            reports,
            reportsByAnyone,
            ids,
            followersFromLastMonth
        )
    }
}

class QueryResults(
    val follows: TimedValue<List<RawEvent>>,
    val followerCount:  TimedValue<Int>,
    val followers: TimedValue<List<RawEvent>>,
    val notifications: TimedValue<List<RawEvent>>,
    val reports: TimedValue<List<RawEvent>>,
    val reportsByAnyone: TimedValue<List<RawEvent>>,
    val ids: TimedValue<List<RawEvent>>,
    val followersFromLastMonth: TimedValue<List<RawEvent>>,
)