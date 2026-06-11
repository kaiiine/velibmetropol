package fr.epf.sin2.velib_metropol.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Distance en mètres entre deux points GPS (formule de Haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /** "350 m" ou "1,2 km" selon la distance. */
    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.toInt()} m"
        else String.format("%.1f km", meters / 1000)
}
