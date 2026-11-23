package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val avatarEmoji: String,
    val colorHex: String
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "ud",
    val supermarket: String = "Mercadona",
    val priority: String = "NORMAL", // NORMAL, URGENTE, CLUB
    val hasOffer: Boolean = false,
    val offerDetails: String = "",
    val category: String = "Despensa",
    val isChecked: Boolean = false,
    val addedByMemberName: String = "Familia",
    val notes: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val dateEpochMillis: Long,
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val assignedMemberId: Long? = null,
    val assignedMemberName: String = "Todos",
    val memberColorHex: String = "#3B82F6",
    val category: String = "GENERAL",
    val reminderMinutesBefore: Int = 15, // 0 = sin aviso, 15, 30, 60, etc.
    val isNotified: Boolean = false
)

@Entity(tableName = "family_tasks")
data class FamilyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val assignedMemberId: Long? = null,
    val assignedMemberName: String = "Cualquiera",
    val assignedMemberEmoji: String = "⭐",
    val dueDate: String = "Hoy",
    val priority: String = "MEDIA", // ALTA, MEDIA, BAJA
    val isCompleted: Boolean = false,
    val category: String = "Hogar"
)

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeekIndex: Int, // 0 = Lunes, 1 = Martes ... 6 = Domingo
    val mealType: String, // DESAYUNO, COMIDA, MERIENDA, CENA
    val dishName: String,
    val recipeId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "General",
    val prepTimeMinutes: Int = 30,
    val servings: Int = 4,
    val ingredients: String, // multiline ingredients
    val instructions: String,
    val emoji: String = "🍳",
    val isFavorite: Boolean = false
)
