package com.vitorpamplona.quartzPerfTest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.vitorpamplona.quartzPerfTest.ui.theme.MyBlue
import com.vitorpamplona.quartzPerfTest.ui.theme.MyCyan
import com.vitorpamplona.quartzPerfTest.ui.theme.MyGreen
import com.vitorpamplona.quartzPerfTest.ui.theme.MyRed
import com.vitorpamplona.quartzPerfTest.ui.theme.MyYellow
import com.vitorpamplona.quartzPerfTest.ui.theme.SimpleTheme
import kotlin.math.round
import kotlin.time.ExperimentalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Progress(
                        importer = App.instance.importer,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Progress(
    importer: Importer,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = spacedBy(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DisplayProcessChart(importer)
            DisplayProcess(importer)
            DisplayOptions(importer)
        }
    }
}

@Composable
fun DisplayProcessChart(importer: Importer) {
    val state = importer.model.collectAsStateWithLifecycle(null)
    val model = state.value
    if (model != null) {
        CartesianChartHost(
            rememberCartesianChart(
                layers = chartLayers,
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
                    valueFormatter = remember { EpochLabelFormatter() },
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
fun DisplayProcess(importer: Importer) {
    val pg by importer.progress.collectAsStateWithLifecycle()

    if (pg.linesProcessed > 0) {
        Text("${pg.percent()}% completed")
        Text("${pg.mbImported()}MB imported")
        Text("${pg.linesProcessed} lines processed")
        Text("${pg.totalDbSize} MB database size")
    }
}

@Composable
fun DisplayOptions(importer: Importer) {
    val state = importer.state.collectAsStateWithLifecycle()
    when (val st = state.value) {
        is ImporterState.Finished -> {
            Text("Finished in ${st.timeMins()} minutes")
        }
        is ImporterState.NotStarted -> {
            Button(importer::import) {
                Text("Start Import")
            }
        }
        else -> {

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

@Stable
class EpochLabelFormatter : CartesianValueFormatter {
    @OptIn(ExperimentalTime::class)
    override fun format(
        context: CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: Axis.Position.Vertical?,
    ): CharSequence {
        return (value.toLong() - context.ranges.minX.toLong()).toString()
    }
}