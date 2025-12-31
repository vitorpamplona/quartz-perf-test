package com.vitorpamplona.quartzPerfTest

import androidx.compose.runtime.Stable
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.crypto.verify
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartz.nip51Lists.muteList.MuteListEvent
import com.vitorpamplona.quartz.nip56Reports.ReportEvent
import com.vitorpamplona.quartz.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update
import kotlin.math.round

const val MB = 1024.0 * 1024.0

fun round1(v: Double) = round(v*10)/10

@Stable
class PerSecond(
    val time: Long = TimeUtils.now(),
    val linesFollows: Int = 0,
    val linesMutes: Int = 0,
    val linesReports: Int = 0,
    val linesBytes: Int = 0,
    val sizeMB: Int = 0,
) {
    fun linesMB() = linesBytes / MB
}

@Stable
sealed interface ImpState {
    object NotStarted : ImpState

    class Running() : ImpState

    class Finished(
        val seconds: Long
    ) : ImpState {
        fun mins() = round1(seconds/60.0)
    }
}

@Stable
class ProgressState(
    val impLines: Int = 0,
    val impBytes: Long = 0,
    val dbSizeMB: Int = 0,
) {
    companion object {
        val DF = DecimalFormat("#,###")
        const val TOTAL = 2158366.0
    }

    fun mbImported() = round(impBytes/MB)
    fun percent() = round1(100*impLines/TOTAL)

    fun percentFmt() = percent().toString() + "%"
    fun mbImportedFmt() = mbImported().toInt().toString() + " MB"
    fun mbDbSizeFmt() = "$dbSizeMB MB"
    fun linesFmt() = DF.format(impLines)
}

@Stable
class Importer(
    val db: EventStore,
    val app: App,
    val scope: CoroutineScope
) {
    val progressOvertime = MutableStateFlow(
        listOf<PerSecond>()
    )
    val progress = MutableStateFlow(
        ProgressState()
    )
    val state = MutableStateFlow<ImpState>(
        ImpState.NotStarted
    )

    @OptIn(ExperimentalAtomicApi::class)
    fun import() = scope.launch {
        val startTime = TimeUtils.now()
        state.emit(ImpState.Running())

        val totalLines = AtomicInt(0)
        val totalBytes = AtomicLong(0)

        val followsPerSec = AtomicInt(0)
        val mutesPerSec = AtomicInt(0)
        val reportsPerSec = AtomicInt(0)
        val bytesPerSec = AtomicInt(0)

        val computeJob = launch {
            val reader = app.assets
                .open("wot.jsonl")
                .reader(Charsets.UTF_8)
                .buffered(1024 * 1024) // 1MB

            reader.forEachLine { line ->
                val event = Event.fromJson(line)

                event.verify()

                db.insert(event)

                when (event) {
                    is ContactListEvent -> followsPerSec.incrementAndFetch()
                    is MuteListEvent -> mutesPerSec.incrementAndFetch()
                    is ReportEvent -> reportsPerSec.incrementAndFetch()
                }

                bytesPerSec.update {
                    it + line.length
                }
                totalBytes.update {
                    it + line.length
                }

                totalLines.incrementAndFetch()
            }
        }

        val monitorJob = launch {
            var pastSize = 0

            while (isActive) {
                val currSize = app.dbSizeMB()

                val new = PerSecond(
                    linesFollows = followsPerSec.fetchAndUpdate { 0 },
                    linesMutes = mutesPerSec.fetchAndUpdate { 0 },
                    linesReports = reportsPerSec.fetchAndUpdate { 0 },
                    linesBytes = bytesPerSec.fetchAndUpdate { 0 },
                    sizeMB = currSize - pastSize
                )

                progressOvertime.update {
                    it + new
                }

                val st = ProgressState(
                    impLines = totalLines.load(),
                    impBytes = totalBytes.load(),
                    dbSizeMB = currSize
                )

                progress.update { st }

                pastSize = currSize

                // not good but it works.
                // Need to find better ways to
                // split measurements in seconds
                // without impacting performance
                delay(1000)
            }
        }

        computeJob.join()

        // avoids cancelling before the
        // last screen update
        delay(1000)
        monitorJob.cancel()

        val elapsed = TimeUtils.now()-startTime
        state.emit(
            ImpState.Finished(elapsed)
        )
    }
}