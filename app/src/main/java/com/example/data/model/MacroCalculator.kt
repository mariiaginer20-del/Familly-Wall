package com.example.data.model

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

data class DefaultMacroIngredientTemplate(
    val idSuffix: String,
    val name: String,
    val emoji: String,
    val carbsPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double
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
    val grams: Int,
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
        val cScore = ingredient.carbsPer100g / max(target.carbsG, 10.0)
        val pScore = ingredient.proteinPer100g / max(target.proteinG, 10.0)
        val fScore = ingredient.fatPer100g / max(target.fatG, 10.0)
        return when {
            pScore >= cScore && pScore >= fScore -> MacroDominance.PROTEIN
            fScore >= cScore && fScore >= pScore -> MacroDominance.FAT
            else -> MacroDominance.CARB
        }
    }

    private fun macroValueFor(ingredient: MacroIngredient, dominance: MacroDominance): Double =
        when (dominance) {
            MacroDominance.CARB -> ingredient.carbsPer100g
            MacroDominance.PROTEIN -> ingredient.proteinPer100g
            MacroDominance.FAT -> ingredient.fatPer100g
        }

    private class LinearRow(
        val coeffs: DoubleArray,
        val rhs: Double
    )

    /**
     * Solves non-negative portions (in 100g units) minimizing normalized macro errors
     * across multi-nutrient foods while balancing contributions (~50%/50%) among
     * ingredients that share the same dominant macro.
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
        val rows = mutableListOf<LinearRow>()

        // 1. Macro target rows (normalized to target scale so Carbs/Protein/Fat balance cleanly)
        val cScale = 1.0 / max(target.carbsG, 12.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].carbsPer100g * cScale },
            rhs = target.carbsG * cScale
        )

        val pScale = 1.0 / max(target.proteinG, 12.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].proteinPer100g * pScale },
            rhs = target.proteinG * pScale
        )

        val fScale = 1.0 / max(target.fatG, 10.0)
        rows += LinearRow(
            coeffs = DoubleArray(n) { i -> selectedIngredients[i].fatPer100g * fScale },
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
                    val mI = macroValueFor(selectedIngredients[i], dom)
                    val mJ = macroValueFor(selectedIngredients[j], dom)
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

        // 4. Exact coordinate descent over w[i] >= 0 (100g units)
        val w = DoubleArray(n) { 0.5 }
        val residuals = DoubleArray(rows.size) { rIdx ->
            val row = rows[rIdx]
            var s = -row.rhs
            for (k in 0 until n) s += row.coeffs[k] * w[k]
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
                    val oldWi = w[i]
                    val newWi = (oldWi - g / a).coerceIn(0.0, 20.0)
                    val delta = newWi - oldWi
                    if (kotlin.math.abs(delta) > 1e-9) {
                        w[i] = newWi
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

        // Convert 100g units to rounded grams and compute actual macros & group percentages
        val gramsList = w.map { (it * 100.0).roundToInt().coerceAtLeast(0) }
        val rawContributions = selectedIngredients.indices.map { idx ->
            macroValueFor(selectedIngredients[idx], dominances[idx]) * (gramsList[idx] / 100.0)
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
            val g = gramsList[idx]
            val factor = g / 100.0
            val c = ing.carbsPer100g * factor
            val p = ing.proteinPer100g * factor
            val f = ing.fatPer100g * factor
            totalC += c
            totalP += p
            totalF += f
            val dom = dominances[idx]
            val gTotal = groupTotals[dom] ?: 0.0
            val sharePct = if (gTotal > 0.01) ((rawContributions[idx] / gTotal) * 100.0).roundToInt() else 0
            MacroIngredientPortion(
                ingredient = ing,
                grams = g,
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
