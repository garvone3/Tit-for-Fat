// app/src/main/java/com/weighttracker/ui/screens/GraphScreen.kt
package com.weighttracker.ui.screens

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.weighttracker.data.WeightEntry
import com.weighttracker.ui.theme.*
import com.weighttracker.viewmodel.AggregatedPoint
import com.weighttracker.viewmodel.GraphFilter
import com.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    navController: NavController,
    vm: WeightViewModel = viewModel()
) {
    val allEntries by vm.allWeights.collectAsStateWithLifecycle()
    val filter     by vm.graphFilter.collectAsStateWithLifecycle()
    val target     by vm.targetWeight.collectAsStateWithLifecycle()
    val height     by vm.heightCm.collectAsStateWithLifecycle()

    val filtered   = vm.filteredEntries(filter, allEntries)
    val weeklyAgg  = vm.weeklyAggregated(allEntries)
    val monthlyAgg = vm.monthlyAggregated(allEntries)
    val insight    = vm.buildInsights(allEntries, target)
    val trendLine  = vm.computeTrendLine(filtered)

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Weight Trend",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Filter chips ─────────────────────────────────────────────
            FilterChipRow(selected = filter, onSelect = { vm.setGraphFilter(it) })

            // ── Legend ───────────────────────────────────────────────────
            GraphLegend(showTarget = target != null, showTrend = trendLine != null)

            // ── Chart ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardColor)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                when (filter) {
                    GraphFilter.WEEK_BY_WEEK ->
                        AggregatedChart(points = weeklyAgg, chartColor = AccentBlue.toArgb(),
                            label = "Weekly Avg", targetWeight = target)
                    GraphFilter.MONTH_BY_MONTH ->
                        AggregatedChart(points = monthlyAgg, chartColor = AccentPurple.toArgb(),
                            label = "Monthly Avg", targetWeight = target)
                    else ->
                        if (filtered.isEmpty()) EmptyChartPlaceholder()
                        else WeightLineChart(
                            entries     = filtered,
                            chartColor  = AccentBlue.toArgb(),
                            filter      = filter,
                            targetWeight = target,
                            trendLine   = trendLine
                        )
                }
            }

            // ── Stats row ────────────────────────────────────────────────
            if (filtered.size >= 2 || allEntries.size >= 2) {
                val src = if (filtered.size >= 2) filtered else allEntries
                StatsSummaryRow(entries = src, targetWeight = target)
            }

            // ── Insights card ─────────────────────────────────────────────
            if (allEntries.size >= 2) {
                GraphInsightsCard(insight = insight)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Legend
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GraphLegend(showTarget: Boolean, showTrend: Boolean) {
    if (!showTarget && !showTrend) return
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        LegendDot(color = AccentBlue,   label = "Weight")
        if (showTarget) LegendDot(color = AccentGreen,  label = "Target")
        if (showTrend)  LegendDot(color = AccentOrange, label = "Trend")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Filter chip row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FilterChipRow(selected: GraphFilter, onSelect: (GraphFilter) -> Unit) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GraphFilter.entries.forEach { f ->
            val isSelected = f == selected
            FilterChip(
                selected = isSelected,
                onClick  = { onSelect(f) },
                label    = {
                    Text(
                        f.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (isSelected) Color(0xFF001F40) else TextSecondary
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentBlue,
                    containerColor         = CardColor,
                    selectedLabelColor     = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = isSelected,
                    borderColor         = CardBorder,
                    selectedBorderColor = AccentBlue
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Weight line chart — with target LimitLine + trend line overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeightLineChart(
    entries: List<WeightEntry>,
    chartColor: Int,
    filter: GraphFilter,
    targetWeight: Float?,
    trendLine: Pair<Float, Float>?   // slope, intercept
) {
    val timestamps = remember(entries) { entries.map { it.timestamp } }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory  = { ctx -> LineChart(ctx).apply { setupChart(this) } },
        update   = { chart ->
            if (entries.isEmpty()) { chart.clear(); return@AndroidView }

            val weightEntries = entries.mapIndexed { i, e -> Entry(i.toFloat(), e.weight) }
            val weightDataSet = LineDataSet(weightEntries, "Weight (kg)").apply {
                setupDataSet(this, chartColor)
            }

            val dataSets = mutableListOf<ILineDataSet>(weightDataSet)

            // Trend line dataset
            if (trendLine != null && entries.size >= 2) {
                val (slope, intercept) = trendLine
                val trendEntries = listOf(
                    Entry(0f, intercept),
                    Entry((entries.size - 1).toFloat(), slope * (entries.size - 1) + intercept)
                )
                val trendDataSet = LineDataSet(trendEntries, "Trend").apply {
                    color                = AccentOrange.toArgb()
                    lineWidth            = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    enableDashedLine(12f, 6f, 0f)
                    mode                 = LineDataSet.Mode.LINEAR
                    setDrawFilled(false)
                    isHighlightEnabled   = false
                }
                dataSets.add(trendDataSet)
            }

            // Target horizontal limit line
            chart.axisLeft.removeAllLimitLines()
            targetWeight?.let { tw ->
                val ll = LimitLine(tw, "Target: %.1f kg".format(tw)).apply {
                    lineColor     = AccentGreen.toArgb()
                    lineWidth     = 1.5f
                    textColor     = AccentGreen.toArgb()
                    textSize      = 10f
                    enableDashedLine(16f, 8f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                }
                chart.axisLeft.addLimitLine(ll)
            }

            val fmt = when (filter) {
                GraphFilter.LAST_7_DAYS, GraphFilter.THIS_WEEK ->
                    SimpleDateFormat("EEE", Locale.getDefault())
                GraphFilter.LAST_30_DAYS ->
                    SimpleDateFormat("MMM dd", Locale.getDefault())
                else ->
                    SimpleDateFormat("MMM dd", Locale.getDefault())
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val i = value.toInt()
                    if (i < 0 || i >= timestamps.size) return ""
                    return fmt.format(Date(timestamps[i]))
                }
            }

            chart.data = LineData(dataSets)
            chart.animateX(800)
            chart.invalidate()
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Aggregated chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AggregatedChart(
    points: List<AggregatedPoint>,
    chartColor: Int,
    label: String,
    targetWeight: Float?
) {
    val labels = remember(points) { points.map { it.label } }

    if (points.isEmpty()) { EmptyChartPlaceholder(); return }

    AndroidView(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        factory  = { ctx -> LineChart(ctx).apply { setupChart(this) } },
        update   = { chart ->
            val chartEntries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.weight) }
            val dataSet = LineDataSet(chartEntries, label).apply {
                setupDataSet(this, chartColor)
            }

            chart.axisLeft.removeAllLimitLines()
            targetWeight?.let { tw ->
                val ll = LimitLine(tw, "Target: %.1f kg".format(tw)).apply {
                    lineColor     = AccentGreen.toArgb()
                    lineWidth     = 1.5f
                    textColor     = AccentGreen.toArgb()
                    textSize      = 10f
                    enableDashedLine(16f, 8f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                }
                chart.axisLeft.addLimitLine(ll)
            }

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val i = value.toInt()
                    return if (i in labels.indices) labels[i] else ""
                }
            }
            chart.data = LineData(dataSet)
            chart.animateX(800)
            chart.invalidate()
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chart setup helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun setupChart(chart: LineChart) {
    val white       = android.graphics.Color.WHITE
    val gray        = android.graphics.Color.parseColor("#33FFFFFF")
    val transparent = android.graphics.Color.TRANSPARENT

    chart.apply {
        description.isEnabled    = false
        legend.isEnabled         = false
        setTouchEnabled(true)
        isDragEnabled            = true
        setScaleEnabled(true)
        setPinchZoom(true)
        isDoubleTapToZoomEnabled = true
        setBackgroundColor(transparent)
        setDrawGridBackground(false)
        setNoDataText("No data for this period")
        setNoDataTextColor(android.graphics.Color.parseColor("#8888AA"))
        setDrawBorders(false)
        extraBottomOffset = 10f
        extraTopOffset    = 16f

        xAxis.apply {
            position             = XAxis.XAxisPosition.BOTTOM
            textColor            = white
            textSize             = 10f
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setAvoidFirstLastClipping(true)
            granularity          = 1f
            labelRotationAngle   = -20f
        }
        axisLeft.apply {
            textColor      = white
            textSize       = 10f
            setDrawGridLines(true)
            gridColor      = gray
            gridLineWidth  = 0.5f
            setDrawAxisLine(false)
        }
        axisRight.isEnabled = false
    }
}

private fun setupDataSet(ds: LineDataSet, color: Int) {
    ds.apply {
        this.color         = color
        lineWidth          = 2.5f
        circleRadius       = 4f
        setCircleColor(color)
        circleHoleColor    = android.graphics.Color.parseColor("#1E1E2A")
        circleHoleRadius   = 2f
        setDrawValues(false)
        mode               = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity     = 0.2f
        highLightColor     = android.graphics.Color.WHITE
        highlightLineWidth = 1f
        isHighlightEnabled = true
        setDrawFilled(true)

        val r = (color shr 16) and 0xFF
        val g = (color shr 8)  and 0xFF
        val b =  color         and 0xFF
        fillDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                android.graphics.Color.argb(100, r, g, b),
                android.graphics.Color.argb(0,   r, g, b)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stats summary row — enhanced with target delta
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatsSummaryRow(entries: List<WeightEntry>, targetWeight: Float?) {
    val weights = entries.map { it.weight }
    val min     = weights.min()
    val max     = weights.max()
    val avg     = weights.average().toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Min", "%.1f kg".format(min), AccentGreen,  Modifier.weight(1f))
            StatChip("Avg", "%.1f kg".format(avg), AccentBlue,   Modifier.weight(1f))
            StatChip("Max", "%.1f kg".format(max), AccentRed,    Modifier.weight(1f))
        }
        targetWeight?.let { t ->
            val latest = weights.lastOrNull()
            if (latest != null) {
                val diff = latest - t
                StatChip(
                    label    = "vs Target",
                    value    = "${if (diff > 0) "+" else ""}${"%.1f".format(diff)} kg",
                    color    = if (abs(diff) < 0.5f) AccentGreen else if (diff > 0) AccentOrange else AccentBlue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = color))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Graph insights card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GraphInsightsCard(insight: com.weighttracker.viewmodel.WeightInsight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Analytics, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            Text("Analysis", style = MaterialTheme.typography.titleMedium)
        }

        val rows = buildList {
            insight.weeklyRate?.let { r ->
                val dir = if (r >= 0) "gaining" else "losing"
                add(Triple(Icons.Default.TrendingFlat, if (r >= 0) AccentRed else AccentGreen,
                    "Rate: $dir ${"%.2f".format(abs(r))} kg per week"))
            }
            insight.monthlyRate?.let { r ->
                add(Triple(Icons.Default.CalendarMonth, AccentBlue,
                    "Monthly change: ${if (r >= 0) "+" else ""}${"%.1f".format(r)} kg"))
            }
            insight.etaWeeks?.let {
                add(Triple(Icons.Default.DirectionsRun, AccentPurple,
                    "Goal ETA: ~${"%.0f".format(abs(it))} weeks"))
            }
            if (insight.hasSpikeOrDrop) {
                add(Triple(Icons.Default.Warning, AccentOrange,
                    insight.spikeMessage ?: "Sudden weight change detected"))
            }
        }

        if (rows.isEmpty()) {
            Text("Log more entries to see analysis", style = MaterialTheme.typography.bodyMedium)
        } else {
            rows.forEach { (icon, color, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty chart placeholder
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier         = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.BarChart, null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Text("No data for this period", style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted))
        }
    }
}
