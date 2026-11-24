package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Supermarket(
    val displayName: String,
    val colorHex: String,
    val emoji: String
) {
    MERCADONA("Mercadona", "#007A3D", "🛒"),
    CARREFOUR("Carrefour", "#004E98", "🛍️"),
    LIDL("Lidl", "#0050AA", "🟡"),
    OTRO("Otro", "#64748B", "🏪");

    companion object {
        fun fromString(value: String): Supermarket {
            if (value.equals("Alcampo", ignoreCase = true)) return MERCADONA
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true)
            } ?: MERCADONA
        }
    }
}

enum class ItemPriority(
    val displayName: String,
    val badgeText: String,
    val colorHex: String
) {
    NORMAL("Normal", "Normal", "#475569"),
    URGENTE("Urgente", "⚡ Urgente", "#DC2626"),
    CLUB("Club", "⭐ Club", "#7C3AED");

    companion object {
        fun fromString(value: String): ItemPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }
    }
}

enum class MealType(val displayName: String, val emoji: String) {
    DESAYUNO("Desayuno", "🥐"),
    COMIDA("Comida", "🍲"),
    MERIENDA("Merienda", "🍎"),
    CENA("Cena", "🥗");

    companion object {
        val plannerTypes: List<MealType> = listOf(COMIDA, CENA)

        fun fromString(value: String): MealType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMIDA
        }
    }
}

enum class TaskPriority(val displayName: String, val colorHex: String) {
    ALTA("Alta", "#DC2626"),
    MEDIA("Media", "#F59E0B"),
    BAJA("Baja", "#10B981");

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIA
        }
    }
}

enum class EventCategory(val displayName: String, val emoji: String, val colorHex: String) {
    GENERAL("General", "📌", "#3B82F6"),
    ESCUELA("Colegio", "🎒", "#8B5CF6"),
    TRABAJO("Trabajo", "💼", "#0284C7"),
    SALUD("Médico", "🩺", "#EF4444"),
    EXTRAESCOLAR("Extraescolar", "⚽", "#10B981"),
    OCIO("Ocio familiar", "🎉", "#F59E0B"),
    HOGAR("Hogar", "🏠", "#6366F1");

    companion object {
        fun fromString(value: String): EventCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true)
            } ?: GENERAL
        }
    }
}

enum class CategoryScope(val displayName: String, val emoji: String) {
    EVENT("Agenda", "📅"),
    TASK("Tareas", "✅"),
    SHOPPING("Compra", "🛒"),
    RECIPE("Recetas", "📖")
}

data class DefaultCategoryTemplate(
    val idSuffix: String,
    val scope: CategoryScope,
    val name: String,
    val emoji: String,
    val colorHex: String
)

object DefaultCategories {
    val all: List<DefaultCategoryTemplate> = listOf(
        // Agenda
        DefaultCategoryTemplate("ev_general", CategoryScope.EVENT, "General", "📌", "#3B82F6"),
        DefaultCategoryTemplate("ev_colegio", CategoryScope.EVENT, "Colegio", "🎒", "#8B5CF6"),
        DefaultCategoryTemplate("ev_trabajo", CategoryScope.EVENT, "Trabajo", "💼", "#0284C7"),
        DefaultCategoryTemplate("ev_medico", CategoryScope.EVENT, "Médico", "🩺", "#EF4444"),
        DefaultCategoryTemplate("ev_extraescolar", CategoryScope.EVENT, "Extraescolar", "⚽", "#10B981"),
        DefaultCategoryTemplate("ev_ocio", CategoryScope.EVENT, "Ocio familiar", "🎉", "#F59E0B"),
        DefaultCategoryTemplate("ev_hogar", CategoryScope.EVENT, "Hogar", "🏠", "#6366F1"),
        // Tareas
        DefaultCategoryTemplate("tk_hogar", CategoryScope.TASK, "Hogar", "🏠", "#1B4D3E"),
        DefaultCategoryTemplate("tk_colegio", CategoryScope.TASK, "Colegio", "🎒", "#8B5CF6"),
        DefaultCategoryTemplate("tk_coche", CategoryScope.TASK, "Coche", "🚗", "#0284C7"),
        DefaultCategoryTemplate("tk_deporte", CategoryScope.TASK, "Deporte", "⚽", "#10B981"),
        DefaultCategoryTemplate("tk_compras", CategoryScope.TASK, "Compras", "🛒", "#C25934"),
        DefaultCategoryTemplate("tk_mascotas", CategoryScope.TASK, "Mascotas", "🐾", "#D97706"),
        DefaultCategoryTemplate("tk_salud", CategoryScope.TASK, "Salud", "🩺", "#EF4444"),
        // Compra
        DefaultCategoryTemplate("sh_despensa", CategoryScope.SHOPPING, "Despensa", "🥫", "#8C6239"),
        DefaultCategoryTemplate("sh_lacteos", CategoryScope.SHOPPING, "Lácteos", "🥛", "#0284C7"),
        DefaultCategoryTemplate("sh_frutas", CategoryScope.SHOPPING, "Frutas y Verduras", "🥬", "#157A4B"),
        DefaultCategoryTemplate("sh_carniceria", CategoryScope.SHOPPING, "Carnicería", "🥩", "#B9382C"),
        DefaultCategoryTemplate("sh_pescaderia", CategoryScope.SHOPPING, "Pescadería", "🐟", "#1D4E89"),
        DefaultCategoryTemplate("sh_limpieza", CategoryScope.SHOPPING, "Limpieza", "🧼", "#0D9488"),
        DefaultCategoryTemplate("sh_panaderia", CategoryScope.SHOPPING, "Panadería", "🥖", "#D97706"),
        DefaultCategoryTemplate("sh_bebidas", CategoryScope.SHOPPING, "Bebidas", "🧃", "#6D3FB8"),
        DefaultCategoryTemplate("sh_congelados", CategoryScope.SHOPPING, "Congelados", "🧊", "#2847D6"),
        // Recetas
        DefaultCategoryTemplate("rc_principal", CategoryScope.RECIPE, "Plato principal", "🍽️", "#1B4D3E"),
        DefaultCategoryTemplate("rc_primeros", CategoryScope.RECIPE, "Primeros Platos", "🥣", "#0D9488"),
        DefaultCategoryTemplate("rc_guisos", CategoryScope.RECIPE, "Guisos", "🍲", "#C25934"),
        DefaultCategoryTemplate("rc_pastas", CategoryScope.RECIPE, "Pastas", "🍝", "#D97706"),
        DefaultCategoryTemplate("rc_cenas", CategoryScope.RECIPE, "Cenas Rápidas", "🍳", "#2847D6"),
        DefaultCategoryTemplate("rc_postres", CategoryScope.RECIPE, "Postres", "🧁", "#6D3FB8"),
        DefaultCategoryTemplate("rc_ensaladas", CategoryScope.RECIPE, "Ensaladas", "🥗", "#157A4B")
    )

    fun forScope(scope: CategoryScope): List<FamilyCategory> {
        return all.filter { it.scope == scope }.map {
            FamilyCategory(
                id = "default_${it.idSuffix}",
                scope = it.scope.name,
                name = it.name,
                emoji = it.emoji,
                colorHex = it.colorHex
            )
        }
    }
}

object ShoppingOfferPresets {
    val presets: List<String> = listOf(
        "2x1",
        "3x2",
        "2ª ud al 70%",
        "2ª ud al 50%",
        "-20% dto.",
        "-30% dto.",
        "-50% dto.",
        "Cupón Club",
        "Pack ahorro"
    )
}

object EventReminderOptions {
    val options: List<Pair<Int, String>> = listOf(
        0 to "A la hora",
        15 to "15 min",
        30 to "30 min",
        60 to "1 hora",
        1440 to "1 día",
        2880 to "2 días",
        4320 to "3 días",
        10080 to "1 semana"
    )

    fun formatShort(minutes: Int): String = when (minutes) {
        0 -> "A la hora"
        15 -> "15 min"
        30 -> "30 min"
        60 -> "1 hora"
        1440 -> "1 día"
        2880 -> "2 días"
        4320 -> "3 días"
        10080 -> "1 sem."
        else -> if (minutes >= 1440) "${minutes / 1440} días" else "$minutes min"
    }
}

data class RecipeIngredient(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "ud"
)

object RecipeFormatters {
    val units: List<String> = listOf("ud", "g", "kg", "L", "ml", "cdas", "cdtas", "tazas", "pizca")

    fun formatIngredients(items: List<RecipeIngredient>): String {
        return items
            .map { it.copy(name = it.name.trim(), quantity = it.quantity.trim(), unit = it.unit.trim().ifBlank { "ud" }) }
            .filter { it.name.isNotBlank() }
            .joinToString("\n") { ing ->
                val qtyPart = ing.quantity.ifBlank { "1" }
                "$qtyPart ${ing.unit} · ${ing.name}"
            }
    }

    fun parseIngredients(raw: String): List<RecipeIngredient> {
        val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return listOf(RecipeIngredient())
        return lines.map { line ->
            val dotIdx = line.indexOf(" · ")
            if (dotIdx > 0) {
                val left = line.substring(0, dotIdx).trim()
                val name = line.substring(dotIdx + 3).trim()
                val parts = left.split(Regex("\\s+"), limit = 2)
                val qty = parts.getOrNull(0) ?: "1"
                val unit = parts.getOrNull(1)?.takeIf { it in units } ?: parts.getOrNull(1) ?: "ud"
                RecipeIngredient(name = name, quantity = qty, unit = unit)
            } else {
                val m = Regex("^(\\d+(?:[.,]\\d+)?)\\s*(kg|ml|ud|g|l|cdas?|tazas?|pizca)?\\s+(.+)$", RegexOption.IGNORE_CASE)
                    .find(line)
                if (m != null) {
                    val qty = m.groupValues[1]
                    val rawUnit = m.groupValues[2]
                    val normalizedUnit = when (rawUnit.lowercase()) {
                        "l" -> "L"
                        "g", "kg", "ml", "ud", "pizca" -> rawUnit.lowercase()
                        "cda", "cdas" -> "cdas"
                        "taza", "tazas" -> "tazas"
                        else -> "ud"
                    }
                    RecipeIngredient(
                        name = m.groupValues[3].trim(),
                        quantity = qty,
                        unit = normalizedUnit
                    )
                } else {
                    RecipeIngredient(name = line, quantity = "1", unit = "ud")
                }
            }
        }
    }

    fun parseIngredientForShopping(raw: String): Triple<String, Double, String> {
        val trimmed = raw.trim()
        val dotIdx = trimmed.indexOf(" · ")
        if (dotIdx > 0) {
            val left = trimmed.substring(0, dotIdx).trim()
            val name = trimmed.substring(dotIdx + 3).trim().ifBlank { trimmed }
            val parts = left.split(Regex("\\s+"), limit = 2)
            val qty = parts.getOrNull(0)?.replace(",", ".")?.toDoubleOrNull()?.coerceIn(0.1, 10000.0) ?: 1.0
            val unit = parts.getOrNull(1)?.trim()?.take(20)?.ifBlank { "ud" } ?: "ud"
            return Triple(name.take(120), qty, unit)
        }
        return Triple(trimmed.take(120), 1.0, "ud")
    }

    fun formatSteps(steps: List<String>): String {
        return steps
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { idx, step ->
                val clean = step.replaceFirst(Regex("^\\d+[.)]\\s*"), "").trim()
                "${idx + 1}. $clean"
            }
            .joinToString("\n")
    }

    fun parseSteps(raw: String): List<String> {
        val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return listOf("")
        return lines.map { it.replaceFirst(Regex("^\\d+[.)]\\s*"), "").trim() }
    }
}

object EventDateUtils {
    fun dayKey(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.get(Calendar.YEAR) * 10000L +
            (cal.get(Calendar.MONTH) + 1) * 100L +
            cal.get(Calendar.DAY_OF_MONTH)
    }

    fun normalizeNoonMillis(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun shortDate(epochMillis: Long): String {
        return SimpleDateFormat("d MMM", Locale("es", "ES")).format(Date(epochMillis))
    }
}

fun CalendarEvent.coversDayKey(targetKey: Long): Boolean {
    val startKey = EventDateUtils.dayKey(dateEpochMillis)
    val endKey = if (endDateEpochMillis > 0L) EventDateUtils.dayKey(endDateEpochMillis) else startKey
    if (targetKey in minOf(startKey, endKey)..maxOf(startKey, endKey)) return true
    return extraDatesEpochMillis.any { EventDateUtils.dayKey(it) == targetKey }
}

fun CalendarEvent.resolvedReminders(): List<Int> {
    return if (reminderMinutesList.isNotEmpty()) reminderMinutesList.distinct().sorted()
    else listOf(reminderMinutesBefore)
}

fun CalendarEvent.formatSpanSummary(baseFormatted: String): String {
    val startKey = EventDateUtils.dayKey(dateEpochMillis)
    val endKey = if (endDateEpochMillis > 0L) EventDateUtils.dayKey(endDateEpochMillis) else startKey
    val hasRange = endKey > startKey
    val extraCount = extraDatesEpochMillis.count { EventDateUtils.dayKey(it) != startKey }
    return when {
        hasRange && extraCount > 0 ->
            "$baseFormatted → ${EventDateUtils.shortDate(endDateEpochMillis)} (+$extraCount)"
        hasRange ->
            "$baseFormatted → ${EventDateUtils.shortDate(endDateEpochMillis)}"
        extraCount > 0 ->
            "$baseFormatted (+$extraCount días)"
        else -> baseFormatted
    }
}
