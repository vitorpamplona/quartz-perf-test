package com.vitorpamplona.quartzPerfTest

import android.R.attr.label
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

            val progress by vm.queryTester.progress.collectAsStateWithLifecycle()
            Text(progress)
        }

        is QueryState.Finished -> {
            TitleRow("Records", "Count\n(ms)", "Load\n(ms)", "Query")
            PropertyRow("Follows", st.results.follows)
            PropertyRow("Followers", st.results.followers)
            PropertyRow("Followers Last Month", st.results.followersFromLastMonth)
            PropertyRow("Notifications", st.results.notifications)
            PropertyRow("Reports", st.results.reports)
            PropertyRow("Reports By Anyone", st.results.reportsByAnyone)
            PropertyRow("Ids", st.results.ids)

            Button(vm::query) {
                Text("Query Followers Again")
            }
        }
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

@Composable
fun PropertyRow(
    label: String,
    result: CombinedResult
) {
    PropertyRow(
        count = result.count.value,
        countTime = result.count.duration.toString(DurationUnit.MILLISECONDS, 2).dropLast(2),
        projTime = result.projected.duration.toString(DurationUnit.MILLISECONDS, 2).dropLast(2),
        label
    )
}

@Composable
fun TitleRow(
    count: String,
    countTime: String,
    projTime: String,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))

        Row (
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.End
            )
            Text(
                text = countTime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = projTime,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun PropertyRow(
    count: Int,
    countTime: String,
    projTime: String,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.7f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row (
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count",
                modifier = Modifier.weight(0.5f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
            Text(
                text = countTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
            Text(
                text = projTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
    }
}