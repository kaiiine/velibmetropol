package fr.epf.sin2.velib_metropol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import fr.epf.sin2.velib_metropol.data.FavoritesStore
import fr.epf.sin2.velib_metropol.data.SettingsStore
import fr.epf.sin2.velib_metropol.data.StationRepository
import fr.epf.sin2.velib_metropol.databinding.ActivityNearbyBinding
import fr.epf.sin2.velib_metropol.util.BlurUtils
import fr.epf.sin2.velib_metropol.util.GeoUtils
import kotlinx.coroutines.launch

/**
 * Stations les plus proches de l'utilisateur, dans le périmètre
 * choisi dans les paramètres.
 */
class NearbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearbyBinding
    private lateinit var adapter: StationAdapter
    private var radiusMeters = SettingsStore.DEFAULT_RADIUS_METERS

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                locateAndShow()
            } else {
                Toast.makeText(this, R.string.location_needed, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNearbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.nearbyRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        BlurUtils.apply(this, binding.headerBlur)

        binding.buttonBack.setOnClickListener { finish() }

        radiusMeters = SettingsStore.getRadiusMeters(this)
        binding.radiusChip.text = getString(R.string.radius_value, radiusMeters)

        adapter = StationAdapter(
            onClick = { station ->
                val intent = Intent(this, StationDetailActivity::class.java)
                intent.putExtra(StationDetailActivity.EXTRA_STATION_ID, station.stationId)
                startActivity(intent)
            },
            onFavoriteToggle = { station ->
                if (FavoritesStore.isFavorite(this, station.stationId)) {
                    FavoritesStore.remove(this, station.stationId)
                } else {
                    FavoritesStore.add(this, station)
                }
                lastLocation?.let { showNearbyStations(it) }
            }
        )
        binding.nearbyList.layoutManager = LinearLayoutManager(this)
        binding.nearbyList.adapter = adapter

        if (hasLocationPermission()) {
            locateAndShow()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private var lastLocation: Location? = null

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun locateAndShow() {
        binding.loadingIndicator.visibility = View.VISIBLE
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    binding.loadingIndicator.visibility = View.GONE
                    Toast.makeText(this, R.string.location_needed, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                lastLocation = location
                ensureStationsThenShow(location)
            }
            .addOnFailureListener {
                binding.loadingIndicator.visibility = View.GONE
                Toast.makeText(this, R.string.location_needed, Toast.LENGTH_LONG).show()
            }
    }

    private fun ensureStationsThenShow(location: Location) {
        lifecycleScope.launch {
            try {
                if (StationRepository.stations.isEmpty()) {
                    StationRepository.refresh()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NearbyActivity, R.string.error_network, Toast.LENGTH_LONG)
                    .show()
            }
            binding.loadingIndicator.visibility = View.GONE
            showNearbyStations(location)
        }
    }

    private fun showNearbyStations(location: Location) {
        val rows = StationRepository.stations
            .map { station ->
                station to GeoUtils.distanceMeters(
                    location.latitude, location.longitude, station.lat, station.lon
                )
            }
            .filter { (_, distance) -> distance <= radiusMeters }
            .sortedBy { (_, distance) -> distance }
            .map { (station, distance) ->
                StationRow(
                    station = station,
                    distanceMeters = distance,
                    isFavorite = FavoritesStore.isFavorite(this, station.stationId)
                )
            }

        adapter.submit(rows)
        if (rows.isEmpty()) {
            binding.emptyView.text = buildEmptyMessage(location)
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.emptyView.visibility = View.GONE
        }
    }

    /**
     * Message d'aide : si l'utilisateur est très loin de toute station
     * (typiquement un émulateur localisé hors de Paris), on le lui dit
     * plutôt que d'afficher une liste vide inexpliquée.
     */
    private fun buildEmptyMessage(location: Location): String {
        val nearestMeters = StationRepository.stations.minOfOrNull { station ->
            GeoUtils.distanceMeters(
                location.latitude, location.longitude, station.lat, station.lon
            )
        } ?: return getString(R.string.error_network)

        return if (nearestMeters > 20_000) {
            "Vous êtes à ${GeoUtils.formatDistance(nearestMeters)} de la station " +
                    "Vélib la plus proche.\n\nSur un émulateur, définissez une position " +
                    "à Paris dans Extended Controls → Location."
        } else {
            getString(R.string.no_nearby, radiusMeters) +
                    "\nStation la plus proche : ${GeoUtils.formatDistance(nearestMeters)}"
        }
    }
}
