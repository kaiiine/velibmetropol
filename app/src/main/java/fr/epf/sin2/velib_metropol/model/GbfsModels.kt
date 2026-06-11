package fr.epf.sin2.velib_metropol.model

import com.google.gson.annotations.SerializedName

/**
 * Modèles bruts du flux open-data GBFS Vélib Métropole.
 * https://www.velib-metropole.fr/donnees-open-data-gbfs-du-service-velib-metropole
 */

data class GbfsInformationResponse(
    @SerializedName("data") val data: StationInformationData
)

data class StationInformationData(
    @SerializedName("stations") val stations: List<StationInformation>
)

data class StationInformation(
    @SerializedName("station_id") val stationId: Long,
    @SerializedName("stationCode") val stationCode: String?,
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("capacity") val capacity: Int
)

data class GbfsStatusResponse(
    @SerializedName("data") val data: StationStatusData
)

data class StationStatusData(
    @SerializedName("stations") val stations: List<StationStatus>
)

data class StationStatus(
    @SerializedName("station_id") val stationId: Long,
    @SerializedName("num_bikes_available") val numBikesAvailable: Int,
    @SerializedName("num_docks_available") val numDocksAvailable: Int,
    // Liste de maps : [{"mechanical": 3}, {"ebike": 5}]
    @SerializedName("num_bikes_available_types") val numBikesAvailableTypes: List<Map<String, Int>>?,
    @SerializedName("is_installed") val isInstalled: Int,
    @SerializedName("is_renting") val isRenting: Int,
    @SerializedName("is_returning") val isReturning: Int,
    @SerializedName("last_reported") val lastReported: Long
) {
    val mechanical: Int
        get() = numBikesAvailableTypes?.firstNotNullOfOrNull { it["mechanical"] } ?: 0

    val ebike: Int
        get() = numBikesAvailableTypes?.firstNotNullOfOrNull { it["ebike"] } ?: 0
}
