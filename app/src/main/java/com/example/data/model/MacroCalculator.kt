package com.example.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class MacroMealSlot(
    val displayName: String,
    val emoji: String,
    val defaultRecipeCategory: String
) {
    DESAYUNO("Desayuno", "🥐", "Plato principal"),
    COMIDA("Comida", "🍲", "Plato principal"),
    CENA("Cena", "🥗", "Cenas Rápidas")
}

enum class MacroDominance(val label: String) {
    CARB("Hidratos"),
    PROTEIN("Proteína"),
    FAT("Grasa")
}

data class MacroTriple(
    val carbsG: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0
) {
    val kcal: Int
        get() = (carbsG * 4.0 + proteinG * 4.0 + fatG * 9.0).roundToInt()
}

fun MacroTargets.dailyTriple(): MacroTriple = MacroTriple(
    carbsG = dailyCarbsG,
    proteinG = dailyProteinG,
    fatG = dailyFatG
)

fun MacroTargets.targetForMeal(slot: MacroMealSlot): MacroTriple = when (slot) {
    MacroMealSlot.DESAYUNO -> MacroTriple(breakfastCarbsG, breakfastProteinG, breakfastFatG)
    MacroMealSlot.COMIDA -> MacroTriple(lunchCarbsG, lunchProteinG, lunchFatG)
    MacroMealSlot.CENA -> MacroTriple(dinnerCarbsG, dinnerProteinG, dinnerFatG)
}

object MacroUnits {
    val quickUnits: List<String> = listOf("g", "kg", "ml", "L", "ud")

    data class ReferencePreset(
        val label: String,
        val quantity: Double,
        val unit: String
    )

    val quickPresets: List<ReferencePreset> = listOf(
        ReferencePreset("100 g", 100.0, "g"),
        ReferencePreset("100 ml", 100.0, "ml"),
        ReferencePreset("1 L", 1.0, "L"),
        ReferencePreset("1 ud", 1.0, "ud")
    )

    fun cleanUnit(raw: String): String = raw.trim().ifBlank { "g" }.take(24)

    fun formatQuantityNumber(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return when {
            abs(rounded - rounded.roundToInt()) < 0.005 -> rounded.roundToInt().toString()
            abs(rounded * 10.0 - (rounded * 10.0).roundToInt()) < 0.05 ->
                String.format(java.util.Locale.US, "%.1f", rounded)
            else -> String.format(java.util.Locale.US, "%.2f", rounded)
        }
    }

    /**
     * Returns equivalent grams or milliliters if [unit] is a standard metric weight/volume unit
     * (`g`, `kg`, `ml`, `L`), or null for count/custom units (`ud`, `rebanada`, etc.).
     */
    fun toBaseMetricAmountOrNull(quantity: Double, unit: String): Double? {
        if (quantity <= 0.0) return null
        return when (unit.trim().lowercase()) {
            "g", "gr", "gramo", "gramos" -> quantity
            "kg", "kilo", "kilos", "kilogramo", "kilogramos" -> quantity * 1000.0
            "ml", "mililitro", "mililitros" -> quantity
            "l", "lt", "litro", "litros" -> quantity * 1000.0
            else -> null
        }
    }

    fun isMassOrVolumeMetric(unit: String): Boolean = toBaseMetricAmountOrNull(1.0, unit) != null

    /**
     * Normalizes macros from `[referenceQuantity] [referenceUnit]` to per-100g/ml for legacy storage
     * when metric, or keeps per-reference macros directly when count/custom unit.
     */
    fun toLegacyPer100g(
        referenceQuantity: Double,
        referenceUnit: String,
        carbsPerRef: Double,
        proteinPerRef: Double,
        fatPerRef: Double
    ): MacroTriple {
        val base = toBaseMetricAmountOrNull(referenceQuantity, referenceUnit)
        return if (base != null && base > 0.0) {
            val scale = 100.0 / base
            MacroTriple(
                carbsG = (carbsPerRef * scale).coerceIn(0.0, 5000.0),
                proteinG = (proteinPerRef * scale).coerceIn(0.0, 5000.0),
                fatG = (fatPerRef * scale).coerceIn(0.0, 5000.0)
            )
        } else {
            MacroTriple(
                carbsG = carbsPerRef.coerceIn(0.0, 5000.0),
                proteinG = proteinPerRef.coerceIn(0.0, 5000.0),
                fatG = fatPerRef.coerceIn(0.0, 5000.0)
            )
        }
    }
}

val MacroIngredient.effectiveRefQuantity: Double
    get() = if (referenceQuantity > 0.0) referenceQuantity else 100.0

val MacroIngredient.effectiveRefUnit: String
    get() = MacroUnits.cleanUnit(referenceUnit)

val MacroIngredient.effectiveCarbsPerRef: Double
    get() = (carbsPerRef ?: carbsPer100g).coerceAtLeast(0.0)

val MacroIngredient.effectiveProteinPerRef: Double
    get() = (proteinPerRef ?: proteinPer100g).coerceAtLeast(0.0)

val MacroIngredient.effectiveFatPerRef: Double
    get() = (fatPerRef ?: fatPer100g).coerceAtLeast(0.0)

val MacroIngredient.effectiveRefKcal: Int
    get() = (effectiveCarbsPerRef * 4.0 + effectiveProteinPerRef * 4.0 + effectiveFatPerRef * 9.0).roundToInt()

fun MacroIngredient.formatReferenceLabel(): String {
    return "${MacroUnits.formatQuantityNumber(effectiveRefQuantity)} $effectiveRefUnit"
}

data class DefaultMacroIngredientTemplate(
    val idSuffix: String,
    val name: String,
    val emoji: String,
    val carbsPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val referenceQuantity: Double = 100.0,
    val referenceUnit: String = "g"
)

object DefaultMacroIngredients {
    val all: List<DefaultMacroIngredientTemplate> = listOf(
        DefaultMacroIngredientTemplate("arroz", "Arroz blanco (crudo)", "🍚", 78.0, 7.0, 0.6),
        DefaultMacroIngredientTemplate("patata", "Patata", "🥔", 17.0, 2.0, 0.1),
        DefaultMacroIngredientTemplate("pasta", "Pasta integral (cruda)", "🍝", 66.0, 13.0, 2.0),
        DefaultMacroIngredientTemplate("avena", "Avena en copos", "🥣", 60.0, 13.5, 7.0),
        DefaultMacroIngredientTemplate("pan_integral", "Pan integral", "🍞", 45.0, 9.0, 3.0),
        DefaultMacroIngredientTemplate("platano", "Plátano", "🍌", 23.0, 1.1, 0.3),
        DefaultMacroIngredientTemplate("pollo", "Pechuga de pollo", "🍗", 0.0, 23.0, 1.5),
        DefaultMacroIngredientTemplate("pavo", "Pechuga de pavo", "🦃", 0.5, 24.0, 1.0),
        DefaultMacroIngredientTemplate("ternera", "Ternera magra", "🥩", 0.0, 21.0, 5.0),
        DefaultMacroIngredientTemplate("atun", "Atún al natural", "🐟", 0.0, 24.0, 0.8),
        DefaultMacroIngredientTemplate("salmon", "Salmón fresco", "🍣", 0.0, 20.0, 13.0),
        DefaultMacroIngredientTemplate("huevos", "Huevos enteros", "🥚", 1.0, 13.0, 11.0),
        DefaultMacroIngredientTemplate("yogur_griego", "Yogur griego natural", "🥛", 4.0, 9.0, 5.0),
        DefaultMacroIngredientTemplate("queso_batido", "Queso fresco batido 0%", "🧀", 4.0, 8.0, 0.2),
        DefaultMacroIngredientTemplate("lentejas", "Lentejas cocidas", "🍲", 20.0, 9.0, 0.5),
        DefaultMacroIngredientTemplate("aove", "Aceite de oliva virgen", "🫒", 0.0, 0.0, 100.0),
        DefaultMacroIngredientTemplate("aguacate", "Aguacate", "🥑", 2.0, 2.0, 15.0),
        DefaultMacroIngredientTemplate("almendras", "Almendras / Nueces", "🥜", 6.0, 21.0, 52.0),
        DefaultMacroIngredientTemplate("crema_cacahuete", "Crema de cacahuete", "🥜", 12.0, 25.0, 50.0)
    )
}

data class MacroIngredientPortion(
    val ingredient: MacroIngredient,
    val amount: Double,
    val unit: String,
    val formattedQuantity: String,
    val formattedAmount: String,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double,
    val dominance: MacroDominance,
    val groupSharePercent: Int
)

data class MacroCalculationResult(
    val target: MacroTriple,
    val achieved: MacroTriple,
    val portions: List<MacroIngredientPortion>
)

object MacroSolver {

    fun dominantMacro(ingredient: MacroIngredient, target: MacroTriple): MacroDominance {
        val cScore = ingredient.effectiveCarbsPerRef / max(target.carbsG, 10.0)
        val pScore = ingredient.effectiveProteinPerRef / max(target.proteinG, 10.0)
        val fScore = ingredient.effectiveFatPerRef / max(target.fatG, 10.0)
        return when {
            pScore >= cScore && pScore >= fScore -> MacroDominance.PROTEIN
            fScore >= cScore && fScore >= pScore -> MacroDominance.FAT
            else -> MacroDominance.CARB
        }
    }

    fun hasMacro(ingredient: MacroIngredient, macro: MacroDominance): Boolean {
        val cRef = ingredient.effectiveCarbsPerRef
        val pRef = ingredient.effectiveProteinPerRef
        val fRef = ingredient.effectiveFatPerRef
        val cKcal = cRef * 4.0
        val pKcal = pRef * 4.0
        val fKcal = fRef * 9.0
        val totalKcal = cKcal + pKcal + fKcal
        if (totalKcal <= 0.0) return false

        val normalized = MacroUnits.toLegacyPer100g(
            referenceQuantity = ingredient.effectiveRefQuantity,
            referenceUnit = ingredient.effectiveRefUnit,
            carbsPerRef = cRef,
            proteinPerRef = pRef,
            fatPerRef = fRef
        )
        val isMetric = MacroUnits.isMassOrVolumeMetric(ingredient.effectiveRefUnit)
        return when (macro) {
            MacroDominance.CARB ->
                (isMetric && normalized.carbsG >= 10.0) || (!isMetric && cRef >= 6.0) || (cKcal / totalKcal) >= 0.20
            MacroDominance.PROTEIN ->
                (isMetric && normalized.proteinG >= 8.0) || (!isMetric && pRef >= 5.0) || (pKcal / totalKcal) >= 0.18
            MacroDominance.FAT ->
                (isMetric && normalized.fatG >= 5.0) || (!isMetric && fRef >= 4.0) || (fKcal / totalKcal) >= 0.20
        }
    }

    private fun macroValuePerRef(ingredient: MacroIngredient, dominance: MacroDominance): Double =
        when (dominance) {
            MacroDominance.CARB -> ingredient.effectiveCarbsPerRef
            MacroDominance.PROTEIN -> ingredient.effectiveProteinPerRef
            MacroDominance.FAT -> ingredient.effectiveFatPerRef
        }

    private class LinearRow(
        val coeffs: DoubleArray,
        val rhs: Double
    )

    private data class RoundedPortionAmount(
        val displayAmount: Double,
        val displayUnit: String,
        val formattedQuantity: String,
        val refFactor: Double
    )

    private fun roundPortionAmount(rawRefPortions: Double, ingredient: MacroIngredient): RoundedPortionAmount {
        val refQty = ingredient.effectiveRefQuantity
        val refUnit = ingredient.effectiveRefUnit
        val rawAmount = (rawRefPortions * refQty).coerceAtLeast(0.0)
        val unitLower = refUnit.lowercase()

        return when (unitLower) {
            "g", "gr", "gramo", "gramos" -> {
                val g = rawAmount.roundToInt().coerceAtLeast(0)
                val unitOut = if (unitLower == "gr") "g" else refUnit
                RoundedPortionAmount(
                    displayAmount = g.toDouble(),
                    displayUnit = unitOut,
                    formattedQuantity = g.toString(),
                    refFactor = g / refQty
                )
            }
            "kg", "kilo", "kilos", "kilogramo", "kilogramos" -> {
                if (rawAmount < 1.0) {
                    val g = (rawAmount * 1000.0).roundToInt().coerceAtLeast(0)
                    RoundedPortionAmount(
                        displayAmount = g.toDouble(),
                        displayUnit = "g",
                        formattedQuantity = g.toString(),
                        refFactor = (g / 1000.0) / refQty
                    )
                } else {
                    val kg = (rawAmount * 100.0).roundToInt() / 100.0
                    RoundedPortionAmount(
                        displayAmount = kg,
                        displayUnit = "kg",
                        formattedQuantity = MacroUnits.formatQuantityNumber(kg),
                        refFactor = kg / refQty
                    )
                }
            }
            "ml", "mililitro", "mililitros" -> {
                val ml = rawAmount.roundToInt().coerceAtLeast(0)
                RoundedPortionAmount(
                    displayAmount = ml.toDouble(),
                    displayUnit = "ml",
                    formattedQuantity = ml.toString(),
                    refFactor = ml / refQty
                )
            }
            "l", "lt", "litro", "litros" -> {
                if (rawAmount < 1.0) {
                    val ml = (rawAmount * 1000.0).roundToInt().coerceAtLeast(0)
                    RoundedPortionAmount(
                        displayAmount = ml.toDouble(),
                        displayUnit = "ml",
                        formattedQuantity = ml.toString(),
                        refFactor = (ml / 1000.0) / refQty
                    )
                } else {
                    val l = (rawAmount * 100.0).roundToInt() / 100.0
                    RoundedPortionAmount(
                        displayAmount = l,
                        displayUnit = "L",
                        formattedQuantity = MacroUnits.formatQuantityNumber(l),
                        refFactor = l / refQty
                    )
                }
            }
            else -> {
                // Count / custom units (e.g., "ud", "rebanada", "cazo"): step 0.5 when >= 0.25, else 0.1
                val rounded = when {
                    rawAmount < 0.04 -> 0.0
                    rawAmount < 0.25 -> ((rawAmount * 10.0).roundToInt() / 10.0).coerceAtLeast(0.1)
                    else -> (rawAmount * 2.0).roundToInt() / 2.0
                }
                RoundedPortionAmount(
                    displayAmount = rounded,
                    displayUnit = refUnit,
                    formattedQuantity = MacroUnits.formatQuantityNumber(rounded),
                    refFactor = rounded / refQty
                )
            }
        }
    }

    /**
     * Solves non-negative portions in a reference-unit-invariant basis (each optimization variable u[i]
     * corresponds to ~100 kcal of ingredient i), minimizing normalized macro errors across multi-nutrient
     * foods while balancing contributions (~50%/50%) among ingredients that share the same dominant macro.
     */
    fun calculate(
        selectedIngredients: List<MacroIngredient>,
        target: MacroTriple
    ): MacroCalculationResult {
        if (selectedIngredients.isEmpty()) {
            return MacroCalculationResult(target = target, achieved = MacroTriple(), portions = emptyList())
        }

        val n = selectedIngredients.size
        val dominances = selectedIngredients.map { dominantMacro(it, target) }

        // Reference-unit invariance: 1 optimization unit u[i] represents ~100 kcal of ingredient i
        val refFactorPerU = DoubleArray(n) { i ->
            val ing = selectedIngredients[i]
            val kcalPerRef = ing.effectiveCarbsPerRef * 4.0 +
                ing.effectiveProteinPerRef * 4.0 +
                ing.effectiveFatPerRef * 9.0
            if (kcalPerRef > 1.0) 100.0 / kcalPerRef else 1.0
        }

        val rows = mutableListOf<LinearRow>()

        // 1. Macro target rows (normalized to target scale so Carbs/Protein/Fat balance cleanly)
        val cScale = 1.0 / max(target.carbsG, 12.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].effectiveCarbsPerRef * refFactorPerU[i] * cScale },
            rhs = target.carbsG * cScale
        )

        val pScale = 1.0 / max(target.proteinG, 12.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].effectiveProteinPerRef * refFactorPerU[i] * pScale },
            rhs = target.proteinG * pScale
        )

        val fScale = 1.0 / max(target.fatG, 10.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].effectiveFatPerRef * refFactorPerU[i] * fScale },
            rhs = target.fatG * fScale
        )

        // 2. Pairwise equal-contribution regularization (~50%/50%) per dominant macro group
        val groupedIndices = dominances.indices.groupBy { dominances[it] }
        for ((dom, indices) in groupedIndices) {
            if (indices.size < 2) continue
            val groupScale = when (dom) {
                MacroDominance.CARB -> max(target.carbsG, 12.0)
                MacroDominance.PROTEIN -> max(target.proteinG, 12.0)
                MacroDominance.FAT -> max(target.fatG, 10.0)
            }
            val pairWeight = sqrt(0.20 / max(indices.size - 1, 1)) / groupScale
            for (aPos in 0 until indices.size) {
                for (bPos in (aPos + 1) until indices.size) {
                    val i = indices[aPos]
                    val j = indices[bPos]
                    val mI = macroValuePerRef(selectedIngredients[i], dom) * refFactorPerU[i]
                    val mJ = macroValuePerRef(selectedIngredients[j], dom) * refFactorPerU[j]
                    if (mI > 0.0 || mJ > 0.0) {
                        val rowCoeffs = DoubleArray(n)
                        rowCoeffs[i] = pairWeight * mI
                        rowCoeffs[j] = -pairWeight * mJ
                        rows += LinearRow(rowCoeffs, 0.0)
                    }
                }
            }
        }

        // 3. Tiny ridge term for numerical stability on zero-macro edge cases
        for (i in 0 until n) {
            val reg = DoubleArray(n)
            reg[i] = 1e-3
            rows += LinearRow(reg, 0.0)
        }

        // 4. Exact coordinate descent over u[i] >= 0 (100-kcal-normalized units)
        val u = DoubleArray(n) { 1.0 }
        val residuals = DoubleArray(rows.size) { rIdx ->
            val row = rows[rIdx]
            var s = -row.rhs
            for (k in 0 until n) s += row.coeffs[k] * u[k]
            s
        }

        repeat(320) {
            for (i in 0 until n) {
                var a = 0.0
                var g = 0.0
                for (rIdx in rows.indices) {
                    val coeff = rows[rIdx].coeffs[i]
                    if (coeff != 0.0) {
                        a += coeff * coeff
                        g += coeff * residuals[rIdx]
                    }
                }
                if (a > 1e-12) {
                    val oldUi = u[i]
                    val newUi = (oldUi - g / a).coerceIn(0.0, 40.0)
                    val delta = newUi - oldUi
                    if (abs(delta) > 1e-9) {
                        u[i] = newUi
                        for (rIdx in rows.indices) {
                            val coeff = rows[rIdx].coeffs[i]
                            if (coeff != 0.0) {
                                residuals[rIdx] += coeff * delta
                            }
                        }
                    }
                }
            }
        }

        // Convert solved reference portions to readable kitchen amounts & compute exact achieved macros
        val roundedList = selectedIngredients.mapIndexed { idx, ing ->
            roundPortionAmount(u[idx] * refFactorPerU[idx], ing)
        }

        val rawContributions = selectedIngredients.indices.map { idx ->
            macroValuePerRef(selectedIngredients[idx], dominances[idx]) * roundedList[idx].refFactor
        }
        val groupTotals = mutableMapOf<MacroDominance, Double>()
        selectedIngredients.indices.forEach { idx ->
            val dom = dominances[idx]
            groupTotals[dom] = (groupTotals[dom] ?: 0.0) + rawContributions[idx]
        }

        var totalC = 0.0
        var totalP = 0.0
        var totalF = 0.0

        val portions = selectedIngredients.mapIndexed { idx, ing ->
            val rounded = roundedList[idx]
            val factor = rounded.refFactor
            val c = ing.effectiveCarbsPerRef * factor
            val p = ing.effectiveProteinPerRef * factor
            val f = ing.effectiveFatPerRef * factor
            totalC += c
            totalP += p
            totalF += f
            val dom = dominances[idx]
            val gTotal = groupTotals[dom] ?: 0.0
            val sharePct = if (gTotal > 0.01) ((rawContributions[idx] / gTotal) * 100.0).roundToInt() else 0
            MacroIngredientPortion(
                ingredient = ing,
                amount = rounded.displayAmount,
                unit = rounded.displayUnit,
                formattedQuantity = rounded.formattedQuantity,
                formattedAmount = "${rounded.formattedQuantity} ${rounded.displayUnit}",
                carbsG = c,
                proteinG = p,
                fatG = f,
                dominance = dom,
                groupSharePercent = sharePct
            )
        }

        return MacroCalculationResult(
            target = target,
            achieved = MacroTriple(carbsG = totalC, proteinG = totalP, fatG = totalF),
            portions = portions
        )
    }
}
