import jetbrains.datalore.base.values.Colors
import jetbrains.datalore.plot.PlotSvgExport
import jetbrains.letsPlot.coordFixed
import jetbrains.letsPlot.geom.geomLine
import jetbrains.letsPlot.geom.geomPolygon
import jetbrains.letsPlot.geom.geomText
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.intern.toSpec
import jetbrains.letsPlot.letsPlot
import jetbrains.letsPlot.scale.guideLegend
import jetbrains.letsPlot.scale.scaleFillDiscrete
import jetbrains.letsPlot.theme
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DisplayGroupName {
    LABEL,
    LEGEND
}

fun pieChart(data: Map<String, Double>, displayGroupName: DisplayGroupName): Plot {
    // Based on workaround from https://github.com/JetBrains/lets-plot-kotlin/issues/68

    val numberOfVertices = data.map { (name, value) -> name to (value * 360).toInt() }.sortedByDescending { it.second }
    var counter = 1.0

    val pieX = mutableListOf<Double>()
    val pieY = mutableListOf<Double>()
    val pieGroup = mutableListOf<Int>()
    val labelsX = mutableListOf<Double>()
    val labelsY = mutableListOf<Double>()
    val labels = mutableListOf<String>()

    for (i in numberOfVertices.indices) {
        // center
        pieX += .0
        pieY += .0
        pieGroup += i
        val counter1 = counter - 1

        // sector start
        pieX += sin(counter1 * PI / 180)
        pieY += cos(counter1 * PI / 180)
        pieGroup += i

        for (j in 1..numberOfVertices[i].second) {
            val counter2 = counter++
            val x = sin(counter2 * PI / 180)
            val y = cos(counter2 * PI / 180)
            // label and line to it
            if (j == numberOfVertices[i].second / 2) {
                labelsX += x
                labelsY += y
                labels += numberOfVertices[i].first
            }

            // sector
            pieX += x
            pieY += y
            pieGroup += i
        }
    }

    return letsPlot()
        .plus(geomPolygon(showLegend = displayGroupName == DisplayGroupName.LEGEND) {
            val colors = Colors.distributeEvenly(data.size, 1.0)
            fill = pieGroup.map { colors[it] }
            group = pieGroup
            x = pieX
            y = pieY
        })
        .plus(scaleFillDiscrete(name = "group", labels = labels))
        .plus(coordFixed(1.0))
        .plus(
            theme(
                axisLine = "blank",
                axisTicks = "blank",
                axisText = "blank",
                axisTitle = "blank",
                panelGrid = "blank",
                legendText = guideLegend(123)
            ),
        )
        .run {
            if (displayGroupName == DisplayGroupName.LABEL) {
                // Unfortunately, lines between label and pie are built one by one
                labels.indices.fold(this) { acc, idx ->
                    acc + geomLine(color = "black") {
                        x = listOf(labelsX[idx], labelsX[idx] * 1.20)
                        y = listOf(labelsY[idx], labelsY[idx] * 1.20)
                    }
                } + geomText {
                    label = labels.map { "Gr-$it" }
                    x = labelsX.map { it * 1.25 }
                    y = labelsY.map { it * 1.25 }
                    vjust = labelsY.map { it + 1.0 }
                    hjust = labelsX.map { it + 1.0 }
                }
            } else
                this
        }
}

fun main() {
    val p = pieChart(mapOf("A" to 0.25, "B" to 0.25, "C" to 0.5), DisplayGroupName.LABEL)

    // Export to SVG and show in the default browser.
    val content = PlotSvgExport.buildSvgImageFromRawSpecs(p.toSpec())
    val file = Path.of("./plot.svg")
    file.writeText(content)
}
