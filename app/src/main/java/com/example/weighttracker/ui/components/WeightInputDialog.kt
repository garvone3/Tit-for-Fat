// app/src/main/java/com/weighttracker/ui/components/WeightInputDialog.kt
package com.weighttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weighttracker.ui.theme.*

@Composable
fun WeightInputDialog(
    currentHeight: Float?,
    onDismiss: () -> Unit,
    onConfirm: (weight: Float, height: Float?) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf(currentHeight?.let { "%.0f".format(it) } ?: "") }
    var showHeightField by remember { mutableStateOf(currentHeight == null) }

    val weightError = weightText.isNotEmpty() &&
            (weightText.toFloatOrNull() == null || (weightText.toFloatOrNull() ?: 0f) <= 0f)
    val heightError = heightText.isNotEmpty() &&
            (heightText.toFloatOrNull() == null || (heightText.toFloatOrNull() ?: 0f) <= 0f)

    val keyboardController = LocalSoftwareKeyboardController.current
    val weightFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { weightFocus.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardColor)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Log Weight",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceVariant, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Weight field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null,
                            tint = AccentBlue, modifier = Modifier.size(18.dp))
                        Text("Weight (kg)", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = { Text("e.g. 75.5", color = TextMuted) },
                        singleLine = true,
                        isError = weightError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = if (showHeightField) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocus),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                    if (weightError) {
                        Text("Enter a valid weight", color = AccentRed,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Height toggle
                if (currentHeight != null) {
                    TextButton(
                        onClick = { showHeightField = !showHeightField },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Height, contentDescription = null,
                            tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showHeightField) "Hide height" else "Update height",
                            style = MaterialTheme.typography.labelLarge,
                            color = AccentPurple
                        )
                    }
                }

                // Height field
                AnimatedVisibility(visible = showHeightField) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Height, contentDescription = null,
                                tint = AccentPurple, modifier = Modifier.size(18.dp))
                            Text("Height (cm) — saved for BMI", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = { Text("e.g. 175", color = TextMuted) },
                            singleLine = true,
                            isError = heightError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            ),
                            colors = outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                        )
                        if (heightError) {
                            Text("Enter a valid height", color = AccentRed,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Confirm button
                Button(
                    onClick = {
                        val w = weightText.toFloatOrNull() ?: return@Button
                        val h = if (heightText.isNotBlank()) heightText.toFloatOrNull() else null
                        onConfirm(w, h)
                    },
                    enabled = weightText.isNotBlank() && !weightError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        contentColor = Color(0xFF001F40)
                    )
                ) {
                    Text("Save Entry", style = MaterialTheme.typography.titleMedium,
                        color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentHeight: Float?,
    currentTarget: Float?,
    onDismiss: () -> Unit,
    onSave: (height: Float?, target: Float?) -> Unit
) {
    var heightText by remember { mutableStateOf(currentHeight?.let { "%.0f".format(it) } ?: "") }
    var targetText by remember { mutableStateOf(currentTarget?.let { "%.1f".format(it) } ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardColor)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceVariant, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close",
                            tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Height
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Height (cm)", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = { Text("e.g. 175", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }

                // Target weight
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target Weight (kg)", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder = { Text("e.g. 70.0", color = TextMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                }

                Button(
                    onClick = {
                        onSave(
                            heightText.toFloatOrNull(),
                            targetText.toFloatOrNull()
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Save", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentBlue,
    unfocusedBorderColor = CardBorder,
    focusedContainerColor   = SurfaceVariant,
    unfocusedContainerColor = SurfaceVariant,
    cursorColor          = AccentBlue,
    errorBorderColor     = AccentRed
)