package fr.epf.sin2.velib_metropol.data

import fr.epf.sin2.velib_metropol.api.VelibApiService
import fr.epf.sin2.velib_metropol.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Source de vérité unique pour les stations.
 * Garde la dernière liste chargée en mémoire pour que toutes
 * les activités y accèdent sans re-télécharger.
 */
object StationRepository {

    @Volatile
    var stations: List<Station> = emptyList()
        private set

    var lastSyncMillis: Long = 0L
        private set

    /**
     * Télécharge informations + disponibilités en parallèle puis les fusionne.
     * @throws Exception en cas d'erreur réseau (gérée par l'appelant).
     */
    suspend fun refresh(): List<Station> = withContext(Dispatchers.IO) {
        coroutineScope {
            val infoDeferred = async { VelibApiService.instance.getStationInformation() }
            val statusDeferred = async { VelibApiService.instance.getStationStatus() }

            val infos = infoDeferred.await().data.stations
            val statusById = statusDeferred.await().data.stations.associateBy { it.stationId }

            val merged = infos.mapNotNull { info ->
                val status = statusById[info.stationId] ?: return@mapNotNull null
                Station(
                    stationId = info.stationId,
                    stationCode = info.stationCode ?: "",
                    name = info.name,
                    lat = info.lat,
                    lon = info.lon,
                    capacity = info.capacity,
                    mechanical = status.mechanical,
                    ebikes = status.ebike,
                    docksAvailable = status.numDocksAvailable,
                    isInstalled = status.isInstalled == 1,
                    isRenting = status.isRenting == 1,
                    isReturning = status.isReturning == 1,
                    lastReported = status.lastReported
                )
            }

            stations = merged
            lastSyncMillis = System.currentTimeMillis()
            merged
        }
    }

    fun findById(stationId: Long): Station? =
        stations.find { it.stationId == stationId }

    fun search(query: String): List<Station> {
        if (query.isBlank()) return emptyList()
        return stations.filter { it.name.contains(query, ignoreCase = true) }
    }
}
