// app/src/main/java/com/weighttracker/ui/screens/DashboardScreen.kt
package com.weighttracker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weighttracker.data.WeightEntry
import com.weighttracker.ui.components.SettingsDialog
import com.weighttracker.ui.components.WeightInputDialog
import com.weighttracker.ui.theme.*
import com.weighttracker.viewmodel.BmiInsight
import com.weighttracker.viewmodel.WeightInsight
import com.weighttracker.viewmodel.WeightViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    vm: WeightViewModel = viewModel()
) {
    val allEntries by vm.allWeights.collectAsStateWithLifecycle()
    val latest     by vm.latestWeight.collectAsStateWithLifecycle()
    val height     by vm.heightCm.collectAsStateWithLifecycle()
    val target     by vm.targetWeight.collectAsStateWithLifecycle()

    var showAddDialog      by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var deleteTarget: WeightEntry? by remember { mutableStateOf(null) }

    val weeklyChange  = vm.weeklyChange(allEntries)
    val monthlyChange = vm.monthlyChange(allEntries)
    val insight       = vm.buildInsights(allEntries, target)
    val bmiInsight    = if (latest != null && height != null)
        vm.buildBmiInsight(latest!!.weight, height!!) else null

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tit for Fat",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("graph") }) {
                        Icon(Icons.Default.ShowChart, "Graph", tint = AccentBlue)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
                modifier       = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.Add, "Add weight", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 110.dp, top = 8.dp)
        ) {
            // ── Hero weight card ─────────────────────────────────────────
            item {
                HeroWeightCard(latest = latest, weeklyChange = weeklyChange)
            }

            // ── BMI + healthy range card ─────────────────────────────────
            if (bmiInsight != null) {
                item { BmiInsightCard(bmiInsight = bmiInsight) }
            } else if (height == null && latest != null) {
                item { AddHeightBanner { showSettingsDialog = true } }
            }

            // ── Target progress card ─────────────────────────────────────
            if (target != null && latest != null) {
                item {
                    TargetProgressCard(
                        currentWeight = latest!!.weight,
                        targetWeight  = target!!,
                        etaWeeks      = insight.etaWeeks
                    )
                }
            }

            // ── Trend cards ──────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TrendCard(label = "This Week",  change = weeklyChange,  modifier = Modifier.weight(1f))
                    TrendCard(label = "This Month", change = monthlyChange, modifier = Modifier.weight(1f))
                }
            }

            // ── Smart insights card ──────────────────────────────────────
            if (allEntries.size >= 2) {
                item { SmartInsightsCard(insight = insight) }
            }

            // ── Recent entries ───────────────────────────────────────────
            if (allEntries.isNotEmpty()) {
                item {
                    Text(
                        "Recent Entries",
                        style    = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(allEntries.take(10)) { entry ->
                    RecentEntryRow(entry = entry, onDelete = { deleteTarget = entry })
                }
            } else {
                item { EmptyState { showAddDialog = true } }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showAddDialog) {
        WeightInputDialog(
            currentHeight = height,
            onDismiss     = { showAddDialog = false },
            onConfirm     = { w, h ->
                vm.addWeight(w)
                h?.let { vm.saveHeight(it) }
                showAddDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentHeight = height,
            currentTarget = target,
            onDismiss     = { showSettingsDialog = false },
            onSave        = { h, t ->
                vm.saveSettings(h, t)
                showSettingsDialog = false
            }
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = CardColor,
            title = { Text("Delete Entry", color = TextPrimary) },
            text  = {
                Text(
                    "Remove %.1f kg logged on %s?".format(
                        entry.weight,
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(entry.timestamp))
                    ),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deleteWeight(entry); deleteTarget = null }) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero weight card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroWeightCard(latest: WeightEntry?, weeklyChange: Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            )
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(
                "Current Weight",
                style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = latest?.weight,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "weight_anim"
            ) { w ->
                if (w != null) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%.1f".format(w),
                            style = MaterialTheme.typography.displayLarge.copy(
                                color = TextPrimary, fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "kg",
                            style    = MaterialTheme.typography.headlineMedium.copy(color = TextSecondary),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                } else {
                    Text("— kg", style = MaterialTheme.typography.displayLarge.copy(color = TextMuted))
                }
            }
            if (latest != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last logged: ${SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(latest.timestamp))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (weeklyChange != null) {
                Spacer(Modifier.height(12.dp))
                WeeklyBadge(weeklyChange)
            }
        }
    }
}

@Composable
private fun WeeklyBadge(change: Float) {
    val isGain = change >= 0
    val color  = if (isGain) AccentRed else AccentGreen
    val icon   = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val sign   = if (isGain) "+" else ""
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            "${sign}${"%.1f".format(change)} kg this week",
            style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.Medium)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BMI Insight card  ← NEW
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BmiInsightCard(bmiInsight: BmiInsight) {
    var expanded by remember { mutableStateOf(true) }
    val bmiColor = when (bmiInsight.category) {
        "Underweight" -> AccentBlue
        "Normal"      -> AccentGreen
        "Overweight"  -> AccentOrange
        else          -> AccentRed
    }

    DashCard {
        // Header row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bmiColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "%.0f".format(bmiInsight.bmi),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = bmiColor, fontWeight = FontWeight.Bold
                        )
                    )
                }
                Column {
                    Text("BMI · ${bmiInsight.category}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ideal: %.1f kg  ·  Range: %.0f–%.0f kg".format(
                            bmiInsight.idealWeight,
                            bmiInsight.minHealthyWeight,
                            bmiInsight.maxHealthyWeight
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = TextMuted
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier            = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(2.dp))

                // BMI scale bar
                BmiScaleBar(bmi = bmiInsight.bmi)

                Spacer(Modifier.height(4.dp))

                // Kg-to-optimal message
                val toOptimal = bmiInsight.kgToOptimal
                BmiActionRow(
                    icon  = Icons.Default.MyLocation,
                    color = AccentBlue,
                    text  = if (abs(toOptimal) < 0.5f) "You're at optimal BMI weight! 🎯"
                    else if (toOptimal < 0)
                        "Lose %.1f kg to reach optimal BMI (22)".format(abs(toOptimal))
                    else
                        "Gain %.1f kg to reach optimal BMI (22)".format(toOptimal)
                )

                // Kg-to-healthy-range message
                if (!bmiInsight.isInHealthyRange && bmiInsight.kgToHealthyRange != null) {
                    val toRange = bmiInsight.kgToHealthyRange
                    BmiActionRow(
                        icon  = Icons.Default.HealthAndSafety,
                        color = AccentGreen,
                        text  = if (toRange > 0)
                            "Gain %.1f kg to enter healthy BMI range".format(toRange)
                        else
                            "Lose %.1f kg to enter healthy BMI range".format(abs(toRange))
                    )
                } else if (bmiInsight.isInHealthyRange) {
                    BmiActionRow(
                        icon  = Icons.Default.CheckCircle,
                        color = AccentGreen,
                        text  = "You're in the healthy BMI range ✓"
                    )
                }

                // Weight range reference
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BmiRangeChip("Min Healthy", "%.1f kg".format(bmiInsight.minHealthyWeight), AccentBlue,   Modifier.weight(1f))
                    BmiRangeChip("Ideal",       "%.1f kg".format(bmiInsight.idealWeight),       AccentGreen,  Modifier.weight(1f))
                    BmiRangeChip("Max Healthy", "%.1f kg".format(bmiInsight.maxHealthyWeight),  AccentOrange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BmiActionRow(icon: ImageVector, color: Color, text: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
    }
}

@Composable
private fun BmiRangeChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier              = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = color, fontSize = 10.sp))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun BmiScaleBar(bmi: Float) {
    val segments = listOf(
        "Underweight" to AccentBlue,
        "Normal"      to AccentGreen,
        "Overweight"  to AccentOrange,
        "Obese"       to AccentRed
    )
    val activeSeg = when {
        bmi < 18.5f -> 0
        bmi < 25f   -> 1
        bmi < 30f   -> 2
        else        -> 3
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        segments.forEachIndexed { i, (label, color) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i == activeSeg) color else color.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color    = if (i == activeSeg) color else TextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Target progress card — enhanced
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TargetProgressCard(currentWeight: Float, targetWeight: Float, etaWeeks: Float?) {
    val diff     = currentWeight - targetWeight
    val isAbove  = diff > 0
    val color    = if (isAbove) AccentOrange else AccentGreen
    // Clamp progress between 0 and 1; treat 20 kg as a "full journey"
    val progress = (1f - (abs(diff) / (abs(diff) + 5f))).coerceIn(0f, 1f)

    DashCard {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Flag, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Text("Target Weight", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Text("%.1f kg".format(targetWeight),
                    style = MaterialTheme.typography.headlineMedium.copy(color = AccentGreen))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Remaining", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                if (abs(diff) < 0.1f) {
                    Text("Goal reached! 🎉",
                        style = MaterialTheme.typography.titleMedium.copy(color = AccentGreen))
                } else {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                                append(if (isAbove) "-" else "+")
                                append("%.1f".format(abs(diff)))
                            }
                            withStyle(SpanStyle(color = TextSecondary, fontSize = 14.sp)) {
                                append(" kg")
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color      = AccentGreen,
            trackColor = SurfaceVariant
        )
        Spacer(Modifier.height(6.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isAbove) "You need to lose %.1f kg".format(abs(diff))
                else "You need to gain %.1f kg".format(abs(diff)),
                style = MaterialTheme.typography.bodySmall.copy(color = color)
            )
            etaWeeks?.let {
                Text(
                    "~${"%.0f".format(abs(it))} weeks at current pace",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Trend card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TrendCard(label: String, change: Float?, modifier: Modifier = Modifier) {
    DashCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (change != null) {
            val positive = change >= 0
            val color    = if (positive) AccentRed else AccentGreen
            val icon     = if (positive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(
                    buildAnnotatedString {
                        val sign = if (positive) "+" else ""
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append("$sign${"%.1f".format(change)}")
                        }
                        withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) { append(" kg") }
                    }
                )
            }
        } else {
            Text("Not enough data", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Smart Insights card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SmartInsightsCard(insight: WeightInsight) {
    DashCard {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Lightbulb, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            Text("Insights", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            insight.weeklyRate?.let { rate ->
                val isGain = rate >= 0
                InsightRow(
                    icon  = if (isGain) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    color = if (isGain) AccentRed else AccentGreen,
                    text  = "You're ${if (isGain) "gaining" else "losing"} ${"%.2f".format(abs(rate))} kg/week"
                )
            }
            insight.monthlyRate?.let { rate ->
                InsightRow(
                    icon  = Icons.Default.CalendarMonth,
                    color = AccentBlue,
                    text  = "Approx. ${"%.1f".format(abs(rate))} kg/month ${if (rate >= 0) "gained" else "lost"}"
                )
            }
            insight.etaWeeks?.let { weeks ->
                InsightRow(
                    icon  = Icons.Default.Flag,
                    color = AccentPurple,
                    text  = "Est. ${"%.0f".format(abs(weeks))} weeks to target at current pace"
                )
            }
            if (insight.hasSpikeOrDrop && insight.spikeMessage != null) {
                InsightRow(
                    icon  = Icons.Default.Warning,
                    color = AccentOrange,
                    text  = insight.spikeMessage
                )
            }
        }
    }
}

@Composable
private fun InsightRow(icon: ImageVector, color: Color, text: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Recent entry row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecentEntryRow(entry: WeightEntry, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("EEE, MMM dd · hh:mm a", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("%.1f kg".format(entry.weight), style = MaterialTheme.typography.titleMedium)
            Text(fmt.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Add height banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddHeightBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentPurple.copy(alpha = 0.1f))
            .border(1.dp, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Height, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Add your height", style = MaterialTheme.typography.titleMedium.copy(color = AccentPurple))
            Text("Enable BMI tracking & insights", style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = AccentPurple)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier                = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.1f))
                .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FitnessCenter, null, tint = AccentBlue, modifier = Modifier.size(36.dp))
        }
        Text("No entries yet", style = MaterialTheme.typography.headlineMedium)
        Text("Tap + to log your first weight", style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onAdd,
            shape  = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Log Weight", color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable card wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}