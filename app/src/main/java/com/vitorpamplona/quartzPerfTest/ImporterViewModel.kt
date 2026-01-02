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
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartzPerfTest.ui.theme.MyBlue
import com.vitorpamplona.quartzPerfTest.ui.theme.MyCyan
import com.vitorpamplona.quartzPerfTest.ui.theme.MyGreen
import com.vitorpamplona.quartzPerfTest.ui.theme.MyLightGreen
import com.vitorpamplona.quartzPerfTest.ui.theme.MyRed
import com.vitorpamplona.quartzPerfTest.ui.theme.MyYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Stable
class ImporterViewModel(
    val importer: Importer,
    val queryTester: QueryTester
): ViewModel() {
    val progress = importer.progress

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
                    // no need to add tags since they just follow the size db.
                    //series(idx, points.map { it.tags })
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
                    //makeLine(MyLightGreen),
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

    val state = MutableStateFlow<ImportState>(
        ImportState.NotStarted
    )

    fun init() {
        if (importer.db.store.dbSizeMB() > 100) {
            state.tryEmit(ImportState.Finished(0))
        }
    }

    fun import() = viewModelScope.launch(Dispatchers.IO) {
        val startTime = TimeUtils.now()
        state.emit(ImportState.Running())

        importer.import()

        val seconds = TimeUtils.now()-startTime
        state.emit(
            ImportState.Finished(seconds)
        )
    }

    val queryTime = MutableStateFlow<QueryState>(
        QueryState.NotStarted
    )

    fun query() = viewModelScope.launch(Dispatchers.IO) {
        queryTime.tryEmit(QueryState.Running)

        queryTester.run()

        queryTime.tryEmit(
            QueryState.Finished()
        )
    }

    fun vacuum() = viewModelScope.launch(Dispatchers.IO) {
        importer.db.store.analyse()
    }

    fun analyse() = viewModelScope.launch(Dispatchers.IO) {
        importer.db.store.vacuum()
    }
}

@Stable
sealed interface QueryState {
    object NotStarted : QueryState

    object Running : QueryState

    class Finished() : QueryState
}

@Stable
sealed interface ImportState {
    object NotStarted : ImportState

    class Running() : ImportState

    class Finished(
        val seconds: Long
    ) : ImportState {
        fun mins() = round1(seconds/60.0)
    }
}