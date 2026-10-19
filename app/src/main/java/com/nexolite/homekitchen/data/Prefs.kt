package com.nexolite.homekitchen.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val kitchenName: String = "Home Kitchen",
    val cutoffMinutes: Int = 11 * 60,
    val menuFooter: String = "Please send your order in this group.",
    val lastBackupAt: Long = 0L
)

/** Small key-value settings kept in SharedPreferences. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("home_kitchen_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun read() = Settings(
        kitchenName = sp.getString(KEY_NAME, null) ?: "Home Kitchen",
        cutoffMinutes = sp.getInt(KEY_CUTOFF, 11 * 60),
        menuFooter = sp.getString(KEY_FOOTER, null) ?: "Please send your order in this group.",
        lastBackupAt = sp.getLong(KEY_BACKUP, 0L)
    )

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        sp.edit()
            .putString(KEY_NAME, next.kitchenName)
            .putInt(KEY_CUTOFF, next.cutoffMinutes)
            .putString(KEY_FOOTER, next.menuFooter)
            .putLong(KEY_BACKUP, next.lastBackupAt)
            .apply()
        _settings.value = next
    }

    private companion object {
        const val KEY_NAME = "kitchen_name"
        const val KEY_CUTOFF = "cutoff_minutes"
        const val KEY_FOOTER = "menu_footer"
        const val KEY_BACKUP = "last_backup_at"
    }
}
