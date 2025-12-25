package com.vitorpamplona.quartzPerfTest

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

@Stable
class ImporterViewModel(
    val importer: Importer
): ViewModel() {
    val progress = importer.progress
    val state = importer.state

    val model = importer.progressOvertime.map { points ->
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
                    series(idx, points.map { it.sizeMB })
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

    val queryTime = MutableStateFlow<TimedValue<List<ContactListEvent>>?>(null)

    fun import() {
        importer.import()
    }

    val filter = Filter(
        kinds = listOf(ContactListEvent.KIND),
        tags = mapOf(
            "p" to listOf("460c25e682fda7832b52d1f22d3d22b3176d972f60dcdc3212ed8c92ef85065c")
        ),
        limit = 10
    )

    fun query() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = measureTimedValue {
                importer.db.query<ContactListEvent>(
                    filter
                )
            }

            queryTime.tryEmit(result)
        }
    }
}

