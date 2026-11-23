package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppDesignStyle(val title: String, val subtitle: String) {
    EDITORIAL("Editorial Cálido", "Tipografía serif clásica, tonos salvia y papel cálido"),
    NORDIC_BENTO("Bento Nórdico", "Geometría moderna, tarjetas amplias y azul cobalto")
}

enum class ThemeDarkMode(val title: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Oscuro")
}

data class AppearanceSettings(
    val designStyle: AppDesignStyle = AppDesignStyle.EDITORIAL,
    val darkMode: ThemeDarkMode = ThemeDarkMode.SYSTEM
)

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "family_wall_appearance")

class ThemePreferences(private val context: Context) {
    private val keyStyle = stringPreferencesKey("design_style")
    private val keyDarkMode = stringPreferencesKey("dark_mode")

    val settingsFlow: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs ->
        val styleRaw = prefs[keyStyle]
        val darkRaw = prefs[keyDarkMode]
        val style = AppDesignStyle.entries.firstOrNull { it.name == styleRaw } ?: AppDesignStyle.EDITORIAL
        val dark = ThemeDarkMode.entries.firstOrNull { it.name == darkRaw } ?: ThemeDarkMode.SYSTEM
        AppearanceSettings(designStyle = style, darkMode = dark)
    }

    suspend fun setDesignStyle(style: AppDesignStyle) {
        context.appearanceDataStore.edit { prefs ->
            prefs[keyStyle] = style.name
        }
    }

    suspend fun setDarkMode(mode: ThemeDarkMode) {
        context.appearanceDataStore.edit { prefs ->
            prefs[keyDarkMode] = mode.name
        }
    }
}
