// app/src/main/java/com/weighttracker/viewmodel/WeightViewModel.kt
package com.weighttracker.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weighttracker.data.AppDatabase
import com.weighttracker.data.WeightEntry
import com.weighttracker.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
//  Graph filter enum
// ─────────────────────────────────────────────────────────────────────────────
enum class GraphFilter(val label: String) {
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    THIS_WEEK("This Week"),
    WEEK_BY_WEEK("By Week"),
    MONTH_BY_MONTH("By Month"),
    LIFETIME("All Time")
}

// ─────────────────────────────────────────────────────────────────────────────
//  Aggregated chart point for week / month views
// ─────────────────────────────────────────────────────────────────────────────
data class AggregatedPoint(
    val label: String,
    val weight: Float,
    val timestamp: Long
)

// ─────────────────────────────────────────────────────────────────────────────
//  Insight data
// ─────────────────────────────────────────────────────────────────────────────
data class WeightInsight(
    val weeklyRate: Float?,
    val monthlyRate: Float?,
    val etaWeeks: Float?,
    val hasSpikeOrDrop: Boolean,
    val spikeMessage: String?
)

// ─────────────────────────────────────────────────────────────────────────────
//  BMI Insight — new
// ─────────────────────────────────────────────────────────────────────────────
data class BmiInsight(
    val bmi: Float,
    val category: String,
    val idealWeight: Float,       // weight for BMI = 22
    val minHealthyWeight: Float,  // weight for BMI = 18.5
    val maxHealthyWeight: Float,  // weight for BMI = 24.9
    val kgToOptimal: Float,       // signed: negative = need to lose, positive = need to gain
    val kgToHealthyRange: Float?, // null if already in range; signed
    val isInHealthyRange: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────
class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).weightDao()
    private val ds  = application.dataStore

    companion object {
        val HEIGHT_KEY        = floatPreferencesKey("height_cm")
        val TARGET_KEY        = floatPreferencesKey("target_weight")
        val ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
    }

    // ── Raw DB streams ──────────────────────────────────────────────────────
    val allWeights: StateFlow<List<WeightEntry>> = dao
        .getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestWeight: StateFlow<WeightEntry?> = dao
        .getLatestWeight()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── DataStore streams ───────────────────────────────────────────────────
    val heightCm: StateFlow<Float?> = ds.data
        .map { it[HEIGHT_KEY] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val targetWeight: StateFlow<Float?> = ds.data
        .map { it[TARGET_KEY] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val onboardingDone: StateFlow<Boolean> = ds.data
        .map { it[ONBOARDING_DONE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Selected graph filter ───────────────────────────────────────────────
    private val _graphFilter = MutableStateFlow(GraphFilter.LAST_30_DAYS)
    val graphFilter: StateFlow<GraphFilter> = _graphFilter.asStateFlow()

    fun setGraphFilter(f: GraphFilter) { _graphFilter.value = f }

    // ── Write operations ────────────────────────────────────────────────────
    fun addWeight(weight: Float, note: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertWeight(WeightEntry(weight = weight, note = note))
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteWeight(entry) }
    }

    fun saveHeight(height: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            ds.edit { it[HEIGHT_KEY] = height }
        }
    }

    fun saveTargetWeight(weight: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            ds.edit { it[TARGET_KEY] = weight }
        }
    }

    fun completeOnboarding(height: Float, targetWeight: Float, currentWeight: Float?) {
        viewModelScope.launch(Dispatchers.IO) {
            ds.edit { prefs ->
                prefs[HEIGHT_KEY]      = height
                prefs[TARGET_KEY]      = targetWeight
                prefs[ONBOARDING_DONE] = true
            }
            currentWeight?.let {
                dao.insertWeight(WeightEntry(weight = it))
            }
        }
    }

    fun saveSettings(height: Float?, target: Float?) {
        viewModelScope.launch(Dispatchers.IO) {
            ds.edit { prefs ->
                height?.let { prefs[HEIGHT_KEY] = it }
                target?.let { prefs[TARGET_KEY] = it }
            }
        }
    }

    // ── BMI helpers ─────────────────────────────────────────────────────────
    fun calculateBMI(weightKg: Float, heightCm: Float): Float {
        val hm = heightCm / 100f
        return weightKg / (hm * hm)
    }

    fun bmiCategory(bmi: Float): String = when {
        bmi < 18.5f -> "Underweight"
        bmi < 25.0f -> "Normal"
        bmi < 30.0f -> "Overweight"
        else        -> "Obese"
    }

    fun bmiCategoryColor(bmi: Float): Long = when {
        bmi < 18.5f -> 0xFF2196F3L
        bmi < 25.0f -> 0xFF4CAF50L
        bmi < 30.0f -> 0xFFFF9800L
        else        -> 0xFFEF5350L
    }

    /**
     * Build rich BMI insight including ideal / healthy range weights
     * and how far the user is from each threshold.
     */
    fun buildBmiInsight(weightKg: Float, heightCm: Float): BmiInsight {
        val hm              = heightCm / 100f
        val hm2             = hm * hm
        val bmi             = weightKg / hm2
        val idealWeight     = 22f   * hm2
        val minHealthy      = 18.5f * hm2
        val maxHealthy      = 24.9f * hm2
        val kgToOptimal     = idealWeight - weightKg   // negative = need to lose
        val isInRange       = bmi in 18.5f..24.9f
        val kgToRange: Float? = when {
            isInRange   -> null
            bmi < 18.5f -> minHealthy - weightKg      // positive = need to gain
            else        -> maxHealthy - weightKg       // negative = need to lose
        }
        return BmiInsight(
            bmi              = bmi,
            category         = bmiCategory(bmi),
            idealWeight      = idealWeight,
            minHealthyWeight = minHealthy,
            maxHealthyWeight = maxHealthy,
            kgToOptimal      = kgToOptimal,
            kgToHealthyRange = kgToRange,
            isInHealthyRange = isInRange
        )
    }

    // ── Trend helpers ────────────────────────────────────────────────────────
    fun weeklyChange(entries: List<WeightEntry>): Float? {
        val cutoff = System.currentTimeMillis() - 7 * 86_400_000L
        return periodChange(entries.filter { it.timestamp >= cutoff })
    }

    fun monthlyChange(entries: List<WeightEntry>): Float? {
        val cutoff = System.currentTimeMillis() - 30 * 86_400_000L
        return periodChange(entries.filter { it.timestamp >= cutoff })
    }

    private fun periodChange(subset: List<WeightEntry>): Float? {
        if (subset.size < 2) return null
        val oldest = subset.minBy { it.timestamp }.weight
        val newest = subset.maxBy { it.timestamp }.weight
        return newest - oldest
    }

    // ── Insights ─────────────────────────────────────────────────────────────
    fun buildInsights(entries: List<WeightEntry>, target: Float?): WeightInsight {
        val sorted = entries.sortedBy { it.timestamp }
        val weeklyRate  = computeWeeklyRate(sorted)
        val monthlyRate = weeklyRate?.let { it * 4.33f }
        val eta: Float? = if (target != null && weeklyRate != null && weeklyRate != 0f) {
            val current = sorted.lastOrNull()?.weight
                ?: return WeightInsight(null, null, null, false, null)
            val delta = target - current
            if ((delta > 0 && weeklyRate > 0) || (delta < 0 && weeklyRate < 0)) delta / weeklyRate
            else null
        } else null

        var spike    = false
        var spikeMsg: String? = null
        for (i in 1 until sorted.size) {
            val diff = sorted[i].weight - sorted[i - 1].weight
            if (abs(diff) >= 2f) {
                spike    = true
                spikeMsg = if (diff > 0) "Spike of +%.1f kg detected".format(diff)
                else "Drop of %.1f kg detected".format(diff)
                break
            }
        }
        return WeightInsight(weeklyRate, monthlyRate, eta, spike, spikeMsg)
    }

    private fun computeWeeklyRate(sorted: List<WeightEntry>): Float? {
        val cutoff = System.currentTimeMillis() - 28 * 86_400_000L
        val subset = sorted.filter { it.timestamp >= cutoff }
        if (subset.size < 2) return null
        val first      = subset.first()
        val last       = subset.last()
        val deltaKg    = last.weight  - first.weight
        val deltaWeeks = (last.timestamp - first.timestamp).toFloat() / (7 * 86_400_000f)
        return if (deltaWeeks > 0) deltaKg / deltaWeeks else null
    }

    /**
     * Compute a linear-regression trend line over [entries].
     * Returns a pair (slope, intercept) where x = index (0-based).
     * Returns null if fewer than 2 points.
     */
    fun computeTrendLine(entries: List<WeightEntry>): Pair<Float, Float>? {
        if (entries.size < 2) return null
        val n     = entries.size.toFloat()
        val xs    = entries.indices.map { it.toFloat() }
        val ys    = entries.map { it.weight }
        val sumX  = xs.sum()
        val sumY  = ys.sum()
        val sumXY = xs.zip(ys).sumOf { (x, y) -> (x * y).toDouble() }.toFloat()
        val sumX2 = xs.sumOf { (it * it).toDouble() }.toFloat()
        val denom = n * sumX2 - sumX * sumX
        if (denom == 0f) return null
        val slope     = (n * sumXY - sumX * sumY) / denom
        val intercept = (sumY - slope * sumX) / n
        return Pair(slope, intercept)
    }

    // ── Filtered entries for chart ──────────────────────────────────────────
    fun filteredEntries(filter: GraphFilter, entries: List<WeightEntry>): List<WeightEntry> {
        val now    = System.currentTimeMillis()
        val sorted = entries.sortedBy { it.timestamp }
        return when (filter) {
            GraphFilter.LAST_7_DAYS   -> sorted.filter { it.timestamp >= now - 7  * 86_400_000L }
            GraphFilter.LAST_30_DAYS  -> sorted.filter { it.timestamp >= now - 30 * 86_400_000L }
            GraphFilter.THIS_WEEK     -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                sorted.filter { it.timestamp >= cal.timeInMillis }
            }
            GraphFilter.WEEK_BY_WEEK,
            GraphFilter.MONTH_BY_MONTH,
            GraphFilter.LIFETIME -> sorted
        }
    }

    fun weeklyAggregated(entries: List<WeightEntry>): List<AggregatedPoint> {
        val cal = Calendar.getInstance()
        return entries
            .groupBy { e ->
                cal.timeInMillis = e.timestamp
                "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
            }
            .map { (label, group) ->
                AggregatedPoint(label, group.map { it.weight }.average().toFloat(), group.first().timestamp)
            }
            .sortedBy { it.timestamp }
    }

    fun monthlyAggregated(entries: List<WeightEntry>): List<AggregatedPoint> {
        val cal    = Calendar.getInstance()
        val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return entries
            .groupBy { e ->
                cal.timeInMillis = e.timestamp
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            }
            .map { (_, group) ->
                cal.timeInMillis = group.first().timestamp
                val label = "${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
                AggregatedPoint(label, group.map { it.weight }.average().toFloat(), group.first().timestamp)
            }
            .sortedBy { it.timestamp }
    }
}