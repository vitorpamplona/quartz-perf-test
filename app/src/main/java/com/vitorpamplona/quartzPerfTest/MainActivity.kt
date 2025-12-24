package com.vitorpamplona.quartzPerfTest

import android.icu.text.DisplayOptions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartzPerfTest.ui.theme.MyBlue
import com.vitorpamplona.quartzPerfTest.ui.theme.MyCyan
import com.vitorpamplona.quartzPerfTest.ui.theme.MyGreen
import com.vitorpamplona.quartzPerfTest.ui.theme.MyRed
import com.vitorpamplona.quartzPerfTest.ui.theme.MyYellow
import com.vitorpamplona.quartzPerfTest.ui.theme.SimpleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleTheme {
                val viewModel: MyViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Progress(
                        vm = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Stable
class MyViewModel(): ViewModel() {
    val progress = App.instance.importer.progress
    val state = App.instance.importer.state

    val model = App.instance.importer.progressOvertime.map { points ->
        if (points.isEmpty())
            null
        else {
            val idx = points.map { it.time }

            val chart1 =
                LineCartesianLayerModel.build {
                    series(idx, points.map { it.linesFollows })
                    series(idx, points.map { it.linesMutes })
                    series(idx, points.map { it.linesReports })
                }

            val chart2 =
                LineCartesianLayerModel.build {
                    series(idx, points.map { it.dbSize })
                    series(idx, points.map { it.linesMB() })
                }

            CartesianChartModel(chart1, chart2)
        }
    }

    val chartLayers =
        arrayOf(
            LineCartesianLayer(
                LineCartesianLayer.LineProvider.series(
                    makeLine(MyGreen),
                    makeLine(MyYellow),
                    makeLine(MyRed),
                ),
                verticalAxisPosition = Axis.Position.Vertical.Start,
            ),
            LineCartesianLayer(
                LineCartesianLayer.LineProvider.series(
                    makeLine(MyBlue),
                    makeLine(MyCyan),
                ),
                verticalAxisPosition = Axis.Position.Vertical.End,
            )
        )

    val queryTime = MutableStateFlow<TimedValue<List<ContactListEvent>>?>(null)

    fun import() {
        App.instance.importer.import()
    }

    fun query() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = measureTimedValue {
                App.instance.importer.db.query<ContactListEvent>(
                    Filter(
                        kinds = listOf(ContactListEvent.KIND),
                        tags = mapOf("p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")),
                        limit = 10
                    )
                )
            }

            queryTime.tryEmit(result)
        }
    }
}

@Composable
fun Progress(
    vm: MyViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = spacedBy(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DisplayProcessChart(vm)
            DisplayProcess(vm)
            DisplayOptions(vm)
        }
    }
}

@Composable
fun DisplayProcessChart(vm: MyViewModel) {
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
fun DisplayProcess(vm: MyViewModel) {
    val pg by vm.progress.collectAsStateWithLifecycle()

    if (pg.linesProcessed > 0) {
        Text("${pg.percent()}% completed")
        Text("${pg.mbImported()}MB imported")
        Text("${pg.linesProcessed} lines processed")
        Text("${pg.totalDbSize} MB database size")
    }
}

@Composable
fun DisplayOptions(vm: MyViewModel) {
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
fun DisplayQuery(vm: MyViewModel) {
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

fun makeLine(color: Color): LineCartesianLayer.Line =
    LineCartesianLayer.Line(
        fill = LineCartesianLayer.LineFill.single(fill(color)),
        areaFill =
            LineCartesianLayer.AreaFill.single(
                fill(
                    ShaderProvider.verticalGradient(
                        color.copy(alpha = 0.4f).toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                ),
            ),
        pointConnector = LineCartesianLayer.PointConnector.cubic(),
    )