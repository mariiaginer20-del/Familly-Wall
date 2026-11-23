package com.example.data.model

enum class Supermarket(
    val displayName: String,
    val colorHex: String,
    val emoji: String
) {
    MERCADONA("Mercadona", "#007A3D", "🛒"),
    CARREFOUR("Carrefour", "#004E98", "🛍️"),
    LIDL("Lidl", "#0050AA", "🟡"),
    ALCAMPO("Alcampo", "#D32F2F", "🐦"),
    OTRO("Otro", "#64748B", "🏪");

    companion object {
        fun fromString(value: String): Supermarket {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: MERCADONA
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
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL
        }
    }
}
