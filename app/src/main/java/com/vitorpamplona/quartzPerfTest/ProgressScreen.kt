package com.vitorpamplona.quartzPerfTest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.vitorpamplona.quartzPerfTest.ui.theme.MyBlue
import kotlin.time.DurationUnit
import kotlin.time.TimedValue

@Composable
fun ProgressScreen(
    vm: ImporterViewModel,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            DisplayColumn(vm)
        }
    }
}

@Composable
fun DisplayColumn(vm: ImporterViewModel) {
    Column(
        verticalArrangement = spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DisplayProcessChart(vm)
        DisplayProcess(vm)
        DisplayOptions(vm)
    }
}

@Composable
fun DisplayProcessChart(vm: ImporterViewModel) {
    val state = vm.model.collectAsStateWithLifecycle(null)
    val model = state.value
    if (model != null) {
        CartesianChartHost(
            rememberCartesianChart(
                layers = vm.chartLayers,
                startAxis = VerticalAxis.rememberStart(
                    label = rememberAxisLabelComponent(),
                    titleComponent = rememberAxisLabelComponent(),
                    title = "events/sec"
                ),
                endAxis =
                    VerticalAxis.rememberEnd(
                        label = rememberAxisLabelComponent(color = MyBlue),
                        titleComponent = rememberAxisLabelComponent(),
                        title = "MB/sec"
                    ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = remember {
                        CartesianValueFormatter { context, value, _ ->
                            (value.toLong() - context.ranges.minX.toLong()).toString()
                        }
                    },
                    titleComponent = rememberAxisLabelComponent(),
                    title = "Seconds since start"
                ),
            ),
            model,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}

@Composable
fun DisplayProcess(vm: ImporterViewModel) {
    val pg by vm.progress.collectAsStateWithLifecycle()

    if (pg.impLines > 0) {
        PropertyRow("Completed", pg.percentFmt())
        PropertyRow("Lines Processed", pg.linesFmt())
        PropertyRow("Imported", pg.mbImportedFmt())
        PropertyRow("DB Size", pg.mbDbSizeFmt())
    }
}

@Composable
fun DisplayOptions(vm: ImporterViewModel) {
    val state = vm.state.collectAsStateWithLifecycle()
    when (val st = state.value) {
        is ImportState.Finished -> {
            Text("Finished in ${st.mins()} minutes")
            DisplayQuery(vm)
        }
        is ImportState.NotStarted -> {
            Button(vm::import) {
                Text("Start Import")
            }
        }
        else -> {

        }
    }
}

@Composable
fun DisplayQuery(vm: ImporterViewModel) {
    val queryResult = vm.queryTime.collectAsStateWithLifecycle()
    when (val st = queryResult.value) {
        QueryState.NotStarted -> {
            Button(vm::query) {
                Text("Query Followers")
            }

            Button(vm::vacuum) {
                Text("Vacuum")
            }

            Button(vm::analyse) {
                Text("Analyse")
            }
        }

        QueryState.Running -> {
            Text("Query Running")
        }

        is QueryState.Finished -> {
            PropertyRow("Follows", st.results.follows.explain())
            PropertyRow("Follower Count", st.results.followerCount.explain())
            PropertyRow("Followers", st.results.followers.explain())
            PropertyRow("Followers Last Month", st.results.followersFromLastMonth.explain())
            PropertyRow("Notifications", st.results.notifications.explain())
            PropertyRow("Reports", st.results.reports.explain())
            PropertyRow("Reports By Anyone", st.results.reportsByAnyone.explain())
            PropertyRow("Ids", st.results.ids.explain())

            Button(vm::query) {
                Text("Query Followers Again")
            }
        }
    }
}

fun <T: Any> TimedValue<T>.explain(): String {
    val time = this.duration.toString(DurationUnit.MILLISECONDS, 3)
    return when (val me = this.value) {
        is List<*> -> "(${me.size}) $time"
        is Int -> "(${me}) $time"
        else -> "(Unknown) $time"
    }
}

@Composable
fun TitleRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = value, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PropertyRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}