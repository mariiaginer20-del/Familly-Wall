package com.example.data.repository

import com.example.data.local.CalendarEventEntity
import com.example.data.local.FamilyDao
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.FamilyTaskEntity
import com.example.data.local.MealPlanEntity
import com.example.data.local.RecipeEntity
import com.example.data.local.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

class FamilyRepository(private val dao: FamilyDao) {

    // Family Members
    val allMembers: Flow<List<FamilyMemberEntity>> = dao.getAllMembers()

    suspend fun insertMember(member: FamilyMemberEntity): Long = dao.insertMember(member)

    // Shopping List
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = dao.getAllShoppingItems()

    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long = dao.insertShoppingItem(item)

    suspend fun updateShoppingItem(item: ShoppingItemEntity) = dao.updateShoppingItem(item)

    suspend fun deleteShoppingItem(item: ShoppingItemEntity) = dao.deleteShoppingItem(item)

    suspend fun setShoppingItemChecked(id: Long, isChecked: Boolean) = dao.setShoppingItemChecked(id, isChecked)

    suspend fun deleteCheckedShoppingItems() = dao.deleteCheckedShoppingItems()

    // Calendar Events
    val allEvents: Flow<List<CalendarEventEntity>> = dao.getAllEvents()

    suspend fun insertEvent(event: CalendarEventEntity): Long = dao.insertEvent(event)

    suspend fun updateEvent(event: CalendarEventEntity) = dao.updateEvent(event)

    suspend fun deleteEvent(event: CalendarEventEntity) = dao.deleteEvent(event)

    suspend fun setEventNotified(id: Long, isNotified: Boolean) = dao.setEventNotified(id, isNotified)

    // Family Tasks
    val allTasks: Flow<List<FamilyTaskEntity>> = dao.getAllTasks()

    suspend fun insertTask(task: FamilyTaskEntity): Long = dao.insertTask(task)

    suspend fun updateTask(task: FamilyTaskEntity) = dao.updateTask(task)

    suspend fun deleteTask(task: FamilyTaskEntity) = dao.deleteTask(task)

    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) = dao.setTaskCompleted(id, isCompleted)

    // Meal Plans
    val allMealPlans: Flow<List<MealPlanEntity>> = dao.getAllMealPlans()

    suspend fun insertMealPlan(mealPlan: MealPlanEntity): Long = dao.insertMealPlan(mealPlan)

    suspend fun updateMealPlan(mealPlan: MealPlanEntity) = dao.updateMealPlan(mealPlan)

    suspend fun deleteMealPlan(mealPlan: MealPlanEntity) = dao.deleteMealPlan(mealPlan)

    // Recipes
    val allRecipes: Flow<List<RecipeEntity>> = dao.getAllRecipes()

    suspend fun insertRecipe(recipe: RecipeEntity): Long = dao.insertRecipe(recipe)

    suspend fun updateRecipe(recipe: RecipeEntity) = dao.updateRecipe(recipe)

    suspend fun deleteRecipe(recipe: RecipeEntity) = dao.deleteRecipe(recipe)

    // Helper: transfer recipe ingredients into shopping list
    suspend fun addRecipeIngredientsToShoppingList(recipe: RecipeEntity, supermarket: String = "Mercadona") {
        val lines = recipe.ingredients.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
        addSelectedIngredientsToShoppingList(recipe.title, lines, supermarket)
    }

    suspend fun addSelectedIngredientsToShoppingList(
        recipeTitle: String,
        ingredients: List<String>,
        supermarket: String = "Mercadona"
    ) {
        val items = ingredients.map { it.trim() }.filter { it.isNotBlank() }.map { line ->
            ShoppingItemEntity(
                name = line,
                quantity = 1.0,
                unit = "ud",
                supermarket = supermarket,
                priority = "NORMAL",
                hasOffer = false,
                offerDetails = "",
                category = "Recetas ($recipeTitle)",
                isChecked = false,
                addedByMemberName = "Receta",
                notes = "Para: $recipeTitle"
            )
        }
        if (items.isNotEmpty()) {
            dao.insertShoppingItems(items)
        }
    }
}
