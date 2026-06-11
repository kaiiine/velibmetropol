package fr.epf.sin2.velib_metropol.data

import android.content.Context

/**
 * Préférences de l'application (périmètre de recherche, auto-refresh).
 */
object SettingsStore {

    private const val PREFS_NAME = "velib_settings"
    private const val KEY_RADIUS = "search_radius_meters"
    private const val KEY_AUTO_REFRESH = "auto_refresh"

    const val DEFAULT_RADIUS_METERS = 500
    const val MIN_RADIUS_METERS = 100
    const val MAX_RADIUS_METERS = 3000

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRadiusMeters(context: Context): Int =
        prefs(context).getInt(KEY_RADIUS, DEFAULT_RADIUS_METERS)

    fun setRadiusMeters(context: Context, radius: Int) {
        prefs(context).edit().putInt(KEY_RADIUS, radius).apply()
    }

    fun isAutoRefreshEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_REFRESH, true)

    fun setAutoRefreshEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
    }
}
