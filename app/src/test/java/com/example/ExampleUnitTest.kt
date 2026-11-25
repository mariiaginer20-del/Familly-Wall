package com.example

import com.example.data.model.MacroDominance
import com.example.data.model.MacroIngredient
import com.example.data.model.MacroSolver
import com.example.data.model.MacroTriple
import com.example.data.model.MealPlan
import com.example.data.model.ProductHistoryEntry
import com.example.data.model.ShoppingItem
import com.example.data.model.Supermarket
import com.example.data.model.formatReferenceLabel
import com.example.ui.ShoppingItemDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun macroSolver_isUnitInvariantAcross75gAnd100g() {
    val target = MacroTriple(carbsG = 78.0, proteinG = 46.0, fatG = 12.0)

    val rice100g = MacroIngredient(
      id = "rice_100",
      name = "Arroz",
      referenceQuantity = 100.0,
      referenceUnit = "g",
      carbsPerRef = 78.0,
      proteinPerRef = 7.0,
      fatPerRef = 0.6,
      carbsPer100g = 78.0,
      proteinPer100g = 7.0,
      fatPer100g = 0.6
    )
    val rice75g = MacroIngredient(
      id = "rice_75",
      name = "Arroz",
      referenceQuantity = 75.0,
      referenceUnit = "g",
      carbsPerRef = 58.5,
      proteinPerRef = 5.25,
      fatPerRef = 0.45,
      carbsPer100g = 78.0,
      proteinPer100g = 7.0,
      fatPer100g = 0.6
    )
    val chicken100g = MacroIngredient(
      id = "chicken",
      name = "Pollo",
      referenceQuantity = 100.0,
      referenceUnit = "g",
      carbsPerRef = 0.0,
      proteinPerRef = 23.0,
      fatPerRef = 1.5,
      carbsPer100g = 0.0,
      proteinPer100g = 23.0,
      fatPer100g = 1.5
    )

    val res100 = MacroSolver.calculate(listOf(rice100g, chicken100g), target)
    val res75 = MacroSolver.calculate(listOf(rice75g, chicken100g), target)

    assertEquals(res100.portions[0].amount, res75.portions[0].amount, 1.0)
    assertEquals(res100.portions[1].amount, res75.portions[1].amount, 1.0)
    assertEquals("75 g", rice75g.formatReferenceLabel())
  }

  @Test
  fun macroSolver_handlesLitersAndUnitsCleanly() {
    val target = MacroTriple(carbsG = 24.0, proteinG = 28.0, fatG = 22.0)

    val milk1L = MacroIngredient(
      id = "milk_1l",
      name = "Leche entera",
      referenceQuantity = 1.0,
      referenceUnit = "L",
      carbsPerRef = 48.0,
      proteinPerRef = 32.0,
      fatPerRef = 36.0
    )
    val eggs1Ud = MacroIngredient(
      id = "egg_1ud",
      name = "Huevo M",
      referenceQuantity = 1.0,
      referenceUnit = "ud",
      carbsPerRef = 0.5,
      proteinPerRef = 6.5,
      fatPerRef = 5.5
    )

    val result = MacroSolver.calculate(listOf(milk1L, eggs1Ud), target)
    val milkPortion = result.portions[0]
    val eggPortion = result.portions[1]

    assertTrue(milkPortion.unit == "ml" || milkPortion.unit == "L")
    assertTrue(milkPortion.amount > 0.0)
    assertEquals("ud", eggPortion.unit)
    assertTrue(eggPortion.amount > 0.0)
    assertTrue(MacroSolver.hasMacro(milk1L, MacroDominance.CARB))
    assertTrue(MacroSolver.hasMacro(eggs1Ud, MacroDominance.PROTEIN))
    assertTrue(MacroSolver.hasMacro(eggs1Ud, MacroDominance.FAT))
  }

  @Test
  fun macroIngredient_legacyDocWithoutPerRefFieldsStillWorks() {
    val legacy = MacroIngredient(
      id = "legacy",
      name = "Avena",
      carbsPer100g = 60.0,
      proteinPer100g = 13.5,
      fatPer100g = 7.0
    )
    val res = MacroSolver.calculate(listOf(legacy), MacroTriple(carbsG = 60.0, proteinG = 14.0, fatG = 7.0))
    assertEquals("g", res.portions[0].unit)
    assertEquals(100.0, res.portions[0].amount, 2.0)
  }

  @Test
  fun shoppingDefaults_useCarrefourAndMealPlanSupportsAdditionalDish() {
    assertEquals("Carrefour", ShoppingItem().supermarket)
    assertEquals("Carrefour", ProductHistoryEntry().lastSupermarket)
    assertEquals(Supermarket.CARREFOUR, ShoppingItemDraft(name = "Leche").supermarket)

    val mealPlan = MealPlan(dishName = "Pollo con arroz", additionalDishName = "Ensalada de atún")
    assertEquals("Pollo con arroz", mealPlan.dishName)
    assertEquals("Ensalada de atún", mealPlan.additionalDishName)
    assertNull(mealPlan.additionalRecipeId)
  }
}
