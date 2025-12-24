package com.vitorpamplona.quartzPerfTest

import android.content.Context
import androidx.compose.runtime.Stable
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.crypto.verify
import com.vitorpamplona.quartz.nip01Core.store.sqlite.EventStore
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartz.nip51Lists.muteList.MuteListEvent
import com.vitorpamplona.quartz.nip56Reports.ReportEvent
import com.vitorpamplona.quartz.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update
import kotlin.math.round

@Stable
class PerSecond(
    val time: Long = TimeUtils.now(),
    val linesFollows: Int = 0,
    val linesMutes: Int = 0,
    val linesReports: Int = 0,
    val linesBytes: Long = 0,
    val dbSize: Int = 0,
) {
    fun linesMB() = linesBytes / (1024.0 * 1024.0)
}

@Stable
sealed interface ImporterState {
    object NotStarted : ImporterState

    class Running(
        val startTime: Long
    ) : ImporterState

    class Finished(
        val time: Long
    ) : ImporterState {
        fun timeMins() = round(10*time/60.0)/10
    }
}

@Stable
class ProgressState(
    val linesProcessed: Int = 0,
    val bytesImported: Long = 0,
    val totalDbSize: Int = 0,
) {
    fun mbImported() = round(bytesImported/(1024.0*1024.0)).toInt()
    fun percent() = round(100*linesProcessed/2158366.0).toInt()
}

@Stable
class Importer(
    val db: EventStore,
    val context: Context,
    val scope: CoroutineScope
) {
    val progressOvertime = MutableStateFlow<List<PerSecond>>(mutableListOf())
    val progress = MutableStateFlow(ProgressState())
    val state = MutableStateFlow<ImporterState>(ImporterState.NotStarted)

    @OptIn(ExperimentalAtomicApi::class)
    fun import() = scope.launch {
        val startTime = TimeUtils.now()
        state.emit(ImporterState.Running(startTime))

        val linesImported = AtomicLong(0)
        val followsPerSec = AtomicLong(0)
        val mutesPerSec = AtomicLong(0)
        val reportsPerSec = AtomicLong(0)
        val bytesPerSec = AtomicLong(0)
        val bytesImported = AtomicLong(0)

        val computeJob = launch(Dispatchers.Default) {
            // this whole thing is limited by read speeds.
            // might need to parallelize this to fully test performance in real world
            val reader = context.assets
                .open("wot.jsonl")
                .reader(Charsets.UTF_8)
                .buffered()

            reader.forEachLine { line ->
                val event = Event.fromJson(line)

                event.verify()

                db.insert(event)

                when (event) {
                    is ContactListEvent -> followsPerSec.incrementAndFetch()
                    is MuteListEvent -> mutesPerSec.incrementAndFetch()
                    is ReportEvent -> reportsPerSec.incrementAndFetch()
                }

                followsPerSec.incrementAndFetch()

                bytesPerSec.update { it + line.length }
                bytesImported.update { it + line.length }

                linesImported.incrementAndFetch()
            }
        }

        val monitorJob = launch(Dispatchers.IO) {
            var pastSize = 0L

            while (isActive) {
                val currSize = App.instance.dbSize()

                progressOvertime.update {
                    it + PerSecond(
                        linesFollows = followsPerSec.fetchAndUpdate { 0 }.toInt(),
                        linesMutes = mutesPerSec.fetchAndUpdate { 0 }.toInt(),
                        linesReports = reportsPerSec.fetchAndUpdate { 0 }.toInt(),
                        linesBytes = bytesPerSec.fetchAndUpdate { 0L },
                        dbSize = (currSize - pastSize).toInt()
                    )
                }

                progress.update {
                    ProgressState(
                        linesProcessed = linesImported.load().toInt(),
                        bytesImported = bytesImported.load(),
                        totalDbSize = currSize.toInt()
                    )
                }

                pastSize = currSize

                // not good but it works.
                // Need to find better ways to split measurements in seconds
                // Without impacting performance
                delay(1000)
            }
        }

        computeJob.join()

        // avoids cancelling before the last screen update
        delay(1000)
        monitorJob.cancel()

        val processingTime = TimeUtils.now()-startTime

        state.emit(
            ImporterState.Finished(processingTime)
        )
    }
}