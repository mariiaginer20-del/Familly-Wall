package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String? = null,
    val familyId: String? = null,
    val avatarEmoji: String = "👤",
    val colorHex: String = "#1B4D3E",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyUnit(
    @DocumentId val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyMember(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val linkedUid: String? = null,
    val name: String = "",
    val role: String = "Adulto",
    val avatarEmoji: String = "👤",
    val colorHex: String = "#1B4D3E",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "ud",
    val supermarket: String = "Mercadona",
    val priority: String = "NORMAL",
    val hasOffer: Boolean = false,
    val offerDetails: String = "",
    val category: String = "Despensa",
    val checked: Boolean = false,
    val addedByMemberName: String = "Familia",
    val assignedMemberName: String = "Todos",
    val notes: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomList(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val emoji: String = "📝",
    val colorHex: String = "#1B4D3E",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomListItem(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val listId: String = "",
    val title: String = "",
    val quantity: String = "",
    val notes: String = "",
    val assignedMemberName: String = "Todos",
    val checked: Boolean = false,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val dateEpochMillis: Long = 0L,
    val endDateEpochMillis: Long = 0L,
    val extraDatesEpochMillis: List<Long> = emptyList(),
    val allDay: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val assignedMemberName: String = "Todos",
    val memberColorHex: String = "#1B4D3E",
    val category: String = "General",
    val reminderMinutesBefore: Int = 15,
    val reminderMinutesList: List<Int> = emptyList(),
    val notified: Boolean = false,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyTask(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedMemberName: String = "Cualquiera",
    val assignedMemberEmoji: String = "⭐",
    val dueDate: String = "Hoy",
    val priority: String = "MEDIA",
    val completed: Boolean = false,
    val category: String = "Hogar",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MealPlan(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val dayOfWeekIndex: Int = 0,
    val mealType: String = "COMIDA",
    val dishName: String = "",
    val recipeId: String? = null,
    val notes: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Recipe(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val category: String = "Plato principal",
    val prepTimeMinutes: Int = 30,
    val servings: Int = 4,
    val ingredients: String = "",
    val instructions: String = "",
    val emoji: String = "🍳",
    val favorite: Boolean = false,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyCategory(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val scope: String = "EVENT",
    val name: String = "",
    val emoji: String = "📌",
    val colorHex: String = "#1B4D3E",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductHistoryEntry(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val nameLower: String = "",
    val lastUnit: String = "ud",
    val lastCategory: String = "Despensa",
    val lastSupermarket: String = "Mercadona",
    val useCount: Int = 1,
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PetFeedingStatus(
    @DocumentId val id: String = "",
    val familyId: String = "",
    val petName: String = "Oliva",
    val dateKey: String = "",
    val fed: Boolean = false,
    val fedByMemberName: String = "",
    val fedTimeLabel: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
