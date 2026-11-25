package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MacroIngredient
import com.example.data.model.MacroTargets
import kotlin.math.roundToInt

private fun formatMacroInput(v: Double): String {
    return if (v % 1.0 == 0.0) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
}

private fun parseMacroInput(raw: String, maxVal: Double): Double {
    val parsed = raw.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    return parsed.coerceIn(0.0, maxVal)
}

@Composable
fun MacroTargetsDialog(
    initialTargets: MacroTargets,
    onDismiss: () -> Unit,
    onSave: (MacroTargets) -> Unit
) {
    var dCarbs by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dailyCarbsG)) }
    var dProt by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dailyProteinG)) }
    var dFat by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dailyFatG)) }

    var bCarbs by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.breakfastCarbsG)) }
    var bProt by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.breakfastProteinG)) }
    var bFat by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.breakfastFatG)) }

    var lCarbs by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.lunchCarbsG)) }
    var lProt by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.lunchProteinG)) }
    var lFat by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.lunchFatG)) }

    var nCarbs by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dinnerCarbsG)) }
    var nProt by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dinnerProteinG)) }
    var nFat by remember(initialTargets) { mutableStateOf(formatMacroInput(initialTargets.dinnerFatG)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Objetivos de Macros",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Define manualmente tus gramos de Hidratos (HC), Proteínas (P) y Grasas (G) para el día y cada comida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MacroTargetGroupEditor(
                    title = "Total diario",
                    carbs = dCarbs,
                    protein = dProt,
                    fat = dFat,
                    onCarbsChange = { dCarbs = it },
                    onProteinChange = { dProt = it },
                    onFatChange = { dFat = it },
                    tagPrefix = "daily"
                )

                OutlinedButton(
                    onClick = {
                        val dc = parseMacroInput(dCarbs, 2000.0)
                        val dp = parseMacroInput(dProt, 1000.0)
                        val df = parseMacroInput(dFat, 1000.0)
                        bCarbs = formatMacroInput((dc * 0.27).roundToInt().toDouble())
                        bProt = formatMacroInput((dp * 0.25).roundToInt().toDouble())
                        bFat = formatMacroInput((df * 0.28).roundToInt().toDouble())
                        lCarbs = formatMacroInput((dc * 0.41).roundToInt().toDouble())
                        lProt = formatMacroInput((dp * 0.40).roundToInt().toDouble())
                        lFat = formatMacroInput((df * 0.38).roundToInt().toDouble())
                        val remC = (dc - parseMacroInput(bCarbs, 1000.0) - parseMacroInput(lCarbs, 1000.0)).coerceAtLeast(0.0)
                        val remP = (dp - parseMacroInput(bProt, 1000.0) - parseMacroInput(lProt, 1000.0)).coerceAtLeast(0.0)
                        val remF = (df - parseMacroInput(bFat, 1000.0) - parseMacroInput(lFat, 1000.0)).coerceAtLeast(0.0)
                        nCarbs = formatMacroInput(remC)
                        nProt = formatMacroInput(remP)
                        nFat = formatMacroInput(remF)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Repartir total diario entre las 3 comidas", fontSize = 12.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                MacroTargetGroupEditor(
                    title = "🥐 Desayuno",
                    carbs = bCarbs,
                    protein = bProt,
                    fat = bFat,
                    onCarbsChange = { bCarbs = it },
                    onProteinChange = { bProt = it },
                    onFatChange = { bFat = it },
                    tagPrefix = "breakfast"
                )

                MacroTargetGroupEditor(
                    title = "🍲 Comida",
                    carbs = lCarbs,
                    protein = lProt,
                    fat = lFat,
                    onCarbsChange = { lCarbs = it },
                    onProteinChange = { lProt = it },
                    onFatChange = { lFat = it },
                    tagPrefix = "lunch"
                )

                MacroTargetGroupEditor(
                    title = "🥗 Cena",
                    carbs = nCarbs,
                    protein = nProt,
                    fat = nFat,
                    onCarbsChange = { nCarbs = it },
                    onProteinChange = { nProt = it },
                    onFatChange = { nFat = it },
                    tagPrefix = "dinner"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialTargets.copy(
                            dailyCarbsG = parseMacroInput(dCarbs, 2000.0),
                            dailyProteinG = parseMacroInput(dProt, 1000.0),
                            dailyFatG = parseMacroInput(dFat, 1000.0),
                            breakfastCarbsG = parseMacroInput(bCarbs, 1000.0),
                            breakfastProteinG = parseMacroInput(bProt, 1000.0),
                            breakfastFatG = parseMacroInput(bFat, 1000.0),
                            lunchCarbsG = parseMacroInput(lCarbs, 1000.0),
                            lunchProteinG = parseMacroInput(lProt, 1000.0),
                            lunchFatG = parseMacroInput(lFat, 1000.0),
                            dinnerCarbsG = parseMacroInput(nCarbs, 1000.0),
                            dinnerProteinG = parseMacroInput(nProt, 1000.0),
                            dinnerFatG = parseMacroInput(nFat, 1000.0)
                        )
                    )
                },
                modifier = Modifier.testTag("save_macro_targets_button")
            ) {
                Text("Guardar objetivos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MacroTargetGroupEditor(
    title: String,
    carbs: String,
    protein: String,
    fat: String,
    onCarbsChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    tagPrefix: String
) {
    val c = carbs.replace(',', '.').toDoubleOrNull() ?: 0.0
    val p = protein.replace(',', '.').toDoubleOrNull() ?: 0.0
    val f = fat.replace(',', '.').toDoubleOrNull() ?: 0.0
    val kcal = (c * 4.0 + p * 4.0 + f * 9.0).roundToInt()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$kcal kcal",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = carbs,
                    onValueChange = onCarbsChange,
                    label = { Text("HC (g)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("macro_${tagPrefix}_carbs")
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = onProteinChange,
                    label = { Text("Prot (g)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("macro_${tagPrefix}_protein")
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = onFatChange,
                    label = { Text("Grasa (g)", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("macro_${tagPrefix}_fat")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroIngredientEditorDialog(
    initialIngredient: MacroIngredient? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, carbs: Double, protein: Double, fat: Double) -> Unit
) {
    var name by remember(initialIngredient) { mutableStateOf(initialIngredient?.name ?: "") }
    var emoji by remember(initialIngredient) { mutableStateOf(initialIngredient?.emoji ?: "🥗") }
    var carbs by remember(initialIngredient) {
        mutableStateOf(if (initialIngredient != null) formatMacroInput(initialIngredient.carbsPer100g) else "0")
    }
    var protein by remember(initialIngredient) {
        mutableStateOf(if (initialIngredient != null) formatMacroInput(initialIngredient.proteinPer100g) else "0")
    }
    var fat by remember(initialIngredient) {
        mutableStateOf(if (initialIngredient != null) formatMacroInput(initialIngredient.fatPer100g) else "0")
    }

    val emojiOptions = listOf(
        "🍗", "🥩", "🐟", "🍣", "🥚", "🥛", "🧀", "🍚", "🥔", "🍝",
        "🥣", "🍞", "🍌", "🥑", "🫒", "🥜", "🍲", "🥗", "🍎", "🫘"
    )

    val cVal = parseMacroInput(carbs, 100.0)
    val pVal = parseMacroInput(protein, 100.0)
    val fVal = parseMacroInput(fat, 100.0)
    val kcal100 = (cVal * 4.0 + pVal * 4.0 + fVal * 9.0).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialIngredient == null) "Nuevo ingrediente" else "Editar ingrediente",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Nombre del alimento (ej: Salmón, Avena)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("macro_ingredient_name_input")
                )

                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojiOptions.forEach { em ->
                        val picked = emoji == em
                        Surface(
                            shape = CircleShape,
                            color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { emoji = em }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(em, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Macros por cada 100 g",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$kcal100 kcal / 100g",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("HC (g)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("macro_ingredient_carbs_input")
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Prot (g)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("macro_ingredient_protein_input")
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Grasa (g)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("macro_ingredient_fat_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), emoji, cVal, pVal, fVal)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_macro_ingredient_button")
            ) {
                Text(if (initialIngredient == null) "Añadir" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
