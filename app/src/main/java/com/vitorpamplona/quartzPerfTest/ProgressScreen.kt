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
        verticalArrangement = spacedBy(30.dp),
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
        Text("${pg.percent()}% completed")
        Text("${pg.mbImported()}MB imported")
        Text("${pg.impLines} lines processed")
        Text("${pg.dbSizeMB} MB database size")
    }
}

@Composable
fun DisplayOptions(vm: ImporterViewModel) {
    val state = vm.state.collectAsStateWithLifecycle()
    when (val st = state.value) {
        is ImpState.Finished -> {
            Text("Finished in ${st.mins()} minutes")
            DisplayQuery(vm)
        }
        is ImpState.NotStarted -> {
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
            Text("Follows ${st.follows}")
            Text("Follower Count ${st.followerCount}")
            Text("Followers ${st.followers}")
            Text("Followers Obj Loaded ${st.followersLoaded}")

            Button(vm::query) {
                Text("Query Followers Again")
            }
        }
    }
}