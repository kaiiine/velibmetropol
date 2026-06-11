package fr.epf.sin2.velib_metropol.model

/**
 * Modèle "métier" d'une station : fusion des données
 * de station_information.json et station_status.json (flux GBFS).
 */
data class Station(
    val stationId: Long,
    val stationCode: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int,
    val mechanical: Int,
    val ebikes: Int,
    val docksAvailable: Int,
    val isInstalled: Boolean,
    val isRenting: Boolean,
    val isReturning: Boolean,
    val lastReported: Long
) {
    val bikesAvailable: Int
        get() = mechanical + ebikes

    val isOperating: Boolean
        get() = isInstalled && (isRenting || isReturning)
}
