package com.vitorpamplona.quartzPerfTest

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ProgressScreen(
    vm: ProgressViewModel,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = spacedBy(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DisplayProcessChart(vm)
                DisplayProcess(vm)
                DisplayOptions(vm)
            }
        }
    }
}

@Composable
fun DisplayProcessChart(vm: ProgressViewModel) {
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
fun DisplayProcess(vm: ProgressViewModel) {
    val pg by vm.progress.collectAsStateWithLifecycle()

    if (pg.linesProcessed > 0) {
        Text("${pg.percent()}% completed")
        Text("${pg.mbImported()}MB imported")
        Text("${pg.linesProcessed} lines processed")
        Text("${pg.totalDbSize} MB database size")
    }
}

@Composable
fun DisplayOptions(vm: ProgressViewModel) {
    val state = vm.state.collectAsStateWithLifecycle()
    when (val st = state.value) {
        is ImporterState.Finished -> {
            Text("Finished in ${st.timeMins()} minutes")
            DisplayQuery(vm)
        }
        is ImporterState.NotStarted -> {
            Button(vm::import) {
                Text("Start Import")
            }
        }
        else -> {

        }
    }
}

@Composable
fun DisplayQuery(vm: ProgressViewModel) {
    val queryResult = vm.queryTime.collectAsStateWithLifecycle()
    when (val st = queryResult.value) {
        null -> {
            Button(vm::query) {
                Text("Query Followers")
            }
        }

        else -> {
            Text("Query Finished with ${st.value.size} results in ${st.duration}")
        }
    }
}