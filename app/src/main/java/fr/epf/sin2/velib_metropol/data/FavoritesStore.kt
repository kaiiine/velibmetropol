package fr.epf.sin2.velib_metropol.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.epf.sin2.velib_metropol.model.Station

/**
 * Une station favorite = un instantané complet de la station
 * au moment de la dernière synchronisation, consultable hors connexion.
 */
data class FavoriteStation(
    val station: Station,
    val savedAtMillis: Long
)

/**
 * Stockage local des favoris en JSON (SharedPreferences).
 * Permet d'accéder à la liste et au détail des stations hors connexion.
 */
object FavoritesStore {

    private const val PREFS_NAME = "velib_favorites"
    private const val KEY_FAVORITES = "favorites_json"

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<FavoriteStation>>() {}.type

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): MutableList<FavoriteStation> {
        val json = prefs(context).getString(KEY_FAVORITES, null) ?: return mutableListOf()
        return gson.fromJson(json, listType) ?: mutableListOf()
    }

    private fun save(context: Context, favorites: List<FavoriteStation>) {
        prefs(context).edit()
            .putString(KEY_FAVORITES, gson.toJson(favorites))
            .apply()
    }

    fun isFavorite(context: Context, stationId: Long): Boolean =
        getAll(context).any { it.station.stationId == stationId }

    fun add(context: Context, station: Station) {
        val favorites = getAll(context)
        favorites.removeAll { it.station.stationId == station.stationId }
        favorites.add(FavoriteStation(station, System.currentTimeMillis()))
        save(context, favorites)
    }

    fun remove(context: Context, stationId: Long) {
        val favorites = getAll(context)
        favorites.removeAll { it.station.stationId == stationId }
        save(context, favorites)
    }

    fun find(context: Context, stationId: Long): FavoriteStation? =
        getAll(context).find { it.station.stationId == stationId }

    /** Met à jour les instantanés des favoris avec des données fraîches. */
    fun refreshSnapshots(context: Context, freshStations: List<Station>) {
        val byId = freshStations.associateBy { it.stationId }
        val updated = getAll(context).map { favorite ->
            byId[favorite.station.stationId]
                ?.let { FavoriteStation(it, System.currentTimeMillis()) }
                ?: favorite
        }
        save(context, updated)
    }
}
