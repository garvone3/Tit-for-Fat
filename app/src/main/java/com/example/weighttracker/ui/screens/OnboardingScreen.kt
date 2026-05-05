// app/src/main/java/com/weighttracker/ui/screens/OnboardingScreen.kt
package com.weighttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weighttracker.ui.theme.*
import com.weighttracker.viewmodel.WeightViewModel

@Composable
fun OnboardingScreen(
    vm: WeightViewModel = viewModel(),
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }  // 0 = welcome, 1 = height, 2 = weight+target

    var heightText  by remember { mutableStateOf("") }
    var weightText  by remember { mutableStateOf("") }
    var targetText  by remember { mutableStateOf("") }

    val heightVal = heightText.toFloatOrNull()
    val weightVal = weightText.toFloatOrNull()
    val targetVal = targetText.toFloatOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0A0F), Color(0xFF0D1117), Color(0xFF0A0A0F))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Step dots ───────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == step) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i == step) AccentBlue
                                else if (i < step) AccentGreen
                                else TextMuted
                            )
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Animated step content ────────────────────────────────────
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "onboarding_step"
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> HeightStep(
                        heightText = heightText,
                        onHeightChange = { heightText = it },
                        heightVal = heightVal
                    )
                    2 -> WeightTargetStep(
                        weightText  = weightText,
                        targetText  = targetText,
                        onWeightChange = { weightText = it },
                        onTargetChange = { targetText = it },
                        heightVal = heightVal,
                        weightVal = weightVal,
                        targetVal = targetVal
                    )
                    else -> WelcomeStep()
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Navigation buttons ───────────────────────────────────────
            val canProceed = when (step) {
                0 -> true
                1 -> heightVal != null && heightVal > 0
                2 -> weightVal != null && weightVal > 0 &&
                        targetVal != null && targetVal > 0
                else -> false
            }

            Button(
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        vm.completeOnboarding(
                            height        = heightVal ?: 170f,
                            targetWeight  = targetVal ?: 70f,
                            currentWeight = weightVal
                        )
                        onComplete()
                    }
                },
                enabled  = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = SurfaceVariant
                )
            ) {
                Text(
                    if (step < 2) "Continue" else "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (step < 2) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (step > 0) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { step-- }) {
                    Icon(Icons.Default.ArrowBack, null,
                        tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 0 — Welcome
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Hero icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
                .border(1.dp, AccentBlue.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MonitorWeight,
                contentDescription = null,
                tint     = AccentBlue,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Welcome to\nTit for Fat",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
                lineHeight = 40.sp
            ),
            color = TextPrimary
        )
        Text(
            "Track your weight, monitor BMI,\nand reach your health goals.",
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        // Feature chips
        listOf(
            Icons.Default.Insights to "Smart BMI Insights",
            Icons.Default.ShowChart to "Progress Graphs",
            Icons.Default.Flag to "Goal Tracking"
        ).forEach { (icon, label) ->
            FeatureRow(icon = icon, label = label)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceVariant)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 1 — Height
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeightStep(
    heightText: String,
    onHeightChange: (String) -> Unit,
    heightVal: Float?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StepHeader(
            icon  = Icons.Default.Height,
            color = AccentPurple,
            title = "Your Height",
            subtitle = "Used to calculate your BMI and ideal weight range"
        )

        OnboardingField(
            value       = heightText,
            onValueChange = { onHeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label       = "Height",
            unit        = "cm",
            placeholder = "e.g. 175",
            accentColor = AccentPurple
        )

        // Live BMI preview
        if (heightVal != null && heightVal > 0) {
            val hm2     = (heightVal / 100f) * (heightVal / 100f)
            val minW    = 18.5f * hm2
            val idealW  = 22f   * hm2
            val maxW    = 24.9f * hm2

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentPurple.copy(alpha = 0.07f))
                    .border(1.dp, AccentPurple.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "For your height (${heightVal.toInt()} cm)",
                    style = MaterialTheme.typography.labelLarge.copy(color = AccentPurple)
                )
                RangeRow("Healthy range", "%.1f – %.1f kg".format(minW, maxW), AccentGreen)
                RangeRow("Ideal weight",  "%.1f kg".format(idealW), AccentBlue)
            }
        }
    }
}

@Composable
private fun RangeRow(label: String, value: String, color: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.SemiBold))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Step 2 — Current weight + target
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeightTargetStep(
    weightText: String,
    targetText: String,
    onWeightChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    heightVal: Float?,
    weightVal: Float?,
    targetVal: Float?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        StepHeader(
            icon     = Icons.Default.FitnessCenter,
            color    = AccentBlue,
            title    = "Current & Goal",
            subtitle = "Log your starting weight and where you want to be"
        )

        OnboardingField(
            value         = weightText,
            onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label         = "Current Weight",
            unit          = "kg",
            placeholder   = "e.g. 80.0",
            accentColor   = AccentBlue
        )

        OnboardingField(
            value         = targetText,
            onValueChange = { onTargetChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label         = "Target Weight",
            unit          = "kg",
            placeholder   = "e.g. 70.0",
            accentColor   = AccentGreen
        )

        // Live preview
        if (weightVal != null && targetVal != null && weightVal > 0 && targetVal > 0) {
            val diff   = weightVal - targetVal
            val isLoss = diff > 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background((if (isLoss) AccentGreen else AccentBlue).copy(alpha = 0.07f))
                    .border(1.dp, (if (isLoss) AccentGreen else AccentBlue).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val color = if (isLoss) AccentGreen else AccentBlue
                Text(
                    if (isLoss) "You want to lose %.1f kg".format(kotlin.math.abs(diff))
                    else "You want to gain %.1f kg".format(kotlin.math.abs(diff)),
                    style = MaterialTheme.typography.titleMedium.copy(color = color)
                )
                if (heightVal != null && heightVal > 0) {
                    val hm2    = (heightVal / 100f) * (heightVal / 100f)
                    val curBmi = weightVal / hm2
                    val tgtBmi = targetVal / hm2
                    Text(
                        "Current BMI: %.1f → Target BMI: %.1f".format(curBmi, tgtBmi),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared composables
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepHeader(icon: ImageVector, color: Color, title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center), color = TextSecondary)
    }
}

@Composable
private fun OnboardingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    placeholder: String,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = TextMuted) },
            singleLine    = true,
            suffix        = { Text(unit, color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = accentColor,
                unfocusedBorderColor    = CardBorder,
                focusedContainerColor   = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                cursorColor             = accentColor
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            shape     = RoundedCornerShape(14.dp),
            modifier  = Modifier.fillMaxWidth()
        )
    }
}