package com.vitorpamplona.quartzPerfTest

import androidx.compose.runtime.Stable
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import com.vitorpamplona.quartz.nip01Core.store.sqlite.RawEvent
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartz.nip10Notes.TextNoteEvent
import com.vitorpamplona.quartz.nip18Reposts.GenericRepostEvent
import com.vitorpamplona.quartz.nip18Reposts.RepostEvent
import com.vitorpamplona.quartz.nip25Reactions.ReactionEvent
import com.vitorpamplona.quartz.nip56Reports.ReportEvent
import com.vitorpamplona.quartz.nip57Zaps.LnZapEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class QueryTester(val db: EventStore) {
    val progress = MutableStateFlow<QueryResults?>(null)

    val followersFilter = Filter(
        kinds = listOf(ContactListEvent.KIND),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 500,
    )

    val notificationsFilter = Filter(
        kinds = listOf(
            TextNoteEvent.KIND,
            ReactionEvent.KIND,
            RepostEvent.KIND,
            GenericRepostEvent.KIND,
            LnZapEvent.KIND,
            ContactListEvent.KIND,
        ),
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

    fun measure(filter: Filter, db: EventStore): CombinedResult {
        return CombinedResult(
            count = measureTimedValue {
                db.store.count(filter)
            },
            projected = measureTimedValue {
                db.store.rawQuery(filter)
            }
        )
    }

    suspend fun run() {
        var result = QueryResults()
        progress.emit(result)
        result = result.copy(
            follows = measure(followsFilter, db)
        )
        progress.emit(result)
        result = result.copy(
            followers = measure(followersFilter, db)
        )
        progress.emit(result)
        result = result.copy(
            followersFromLastMonth = measure(followersFilterByDate, db)
        )
        progress.emit(result)
        result = result.copy(
            notifications = measure(notificationsFilter, db)
        )
        progress.emit(result)
        result = result.copy(
            reports = measure(reportsFilter, db)
        )
        progress.emit(result)
        result = result.copy(
            reportsByAnyone = measure(reportsByAnyoneFilter, db)
        )
        progress.emit(result)
        result = result.copy(
            ids = measure(idsFilter, db)
        )
        progress.emit(result)
    }
}

@Stable
class CombinedResult(
    val count: TimedValue<Int>,
    val projected: TimedValue<List<RawEvent>>,
) {
    init {
        assert(count.value == projected.value.size)
    }
}

data class QueryResults(
    val follows: CombinedResult? = null,
    val followers: CombinedResult? = null,
    val notifications: CombinedResult? = null,
    val reports: CombinedResult? = null,
    val reportsByAnyone: CombinedResult? = null,
    val ids: CombinedResult? = null,
    val followersFromLastMonth: CombinedResult? = null,
)