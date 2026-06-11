package fr.epf.sin2.velib_metropol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import fr.epf.sin2.velib_metropol.data.FavoritesStore
import fr.epf.sin2.velib_metropol.data.SettingsStore
import fr.epf.sin2.velib_metropol.data.StationRepository
import fr.epf.sin2.velib_metropol.databinding.ActivityMainBinding
import fr.epf.sin2.velib_metropol.model.Station
import fr.epf.sin2.velib_metropol.util.BlurUtils
import fr.epf.sin2.velib_metropol.util.MarkerIconFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOCUS_STATION_ID = "extra_focus_station_id"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var markerIconFactory: MarkerIconFactory
    private lateinit var searchAdapter: SearchResultAdapter

    private val stationMarkers = mutableListOf<Marker>()
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var autoRefreshJob: Job? = null
    private var selectedStation: Station? = null
    private var pendingFocusStationId: Long = -1L

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) {
                enableMyLocation(centerOnFix = true)
            } else {
                Toast.makeText(this, R.string.location_needed, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obligatoire pour osmdroid : identifie l'app auprès des serveurs de tuiles
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        markerIconFactory = MarkerIconFactory(this)

        // Flou "verre dépoli" derrière les éléments flottants
        BlurUtils.apply(
            this,
            binding.searchBar,
            binding.searchResultsCard,
            binding.statsPillCard,
            binding.stationCard,
            binding.myLocationCard,
            binding.bottomBar
        )

        setupMap()
        setupSearch()
        setupButtons()

        pendingFocusStationId = intent.getLongExtra(EXTRA_FOCUS_STATION_ID, -1L)
        loadStations(showSpinner = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val stationId = intent.getLongExtra(EXTRA_FOCUS_STATION_ID, -1L)
        if (stationId != -1L) focusStation(stationId)
    }

    private fun focusStation(stationId: Long) {
        val station = StationRepository.findById(stationId) ?: return
        binding.mapView.controller.animateTo(GeoPoint(station.lat, station.lon), 17.0, 600L)
        showStationCard(station)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.searchBar.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + dp(12)
            }
            binding.bottomBar.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + dp(12)
            }
            insets
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // ----------------------------------------------------------------- Carte

    private fun setupMap() = with(binding.mapView) {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        minZoomLevel = 10.0
        controller.setZoom(14.0)
        controller.setCenter(GeoPoint(48.8566, 2.3522)) // Paris

        // Tuiles assombries pour coller au thème sombre
        overlayManager.tilesOverlay.setColorFilter(buildDarkTileFilter())

        addMapListener(DelayedMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                updateMarkers()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                updateMarkers()
                return true
            }
        }, 150))
    }

    /** Inverse les couleurs des tuiles puis réduit la saturation : carte sombre. */
    private fun buildDarkTileFilter(): ColorMatrixColorFilter {
        val inverse = ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val desaturate = ColorMatrix().apply { setSaturation(0.3f) }
        inverse.postConcat(desaturate)
        return ColorMatrixColorFilter(inverse)
    }

    /**
     * N'affiche que les markers de la zone visible (et seulement à partir
     * d'un certain zoom) pour que la carte reste parfaitement fluide
     * malgré les ~1 500 stations.
     */
    private fun updateMarkers() {
        val map = binding.mapView
        val stations = StationRepository.stations
        if (stations.isEmpty()) return

        map.overlays.removeAll(stationMarkers)
        stationMarkers.clear()

        if (map.zoomLevelDouble >= 13.0) {
            val bbox = map.boundingBox.increaseByScale(1.2f)
            stations.asSequence()
                .filter { bbox.contains(it.lat, it.lon) }
                .take(400)
                .forEach { station ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(station.lat, station.lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = markerIconFactory.iconFor(station.bikesAvailable, station.isOperating)
                        infoWindow = null
                        setOnMarkerClickListener { _, _ ->
                            showStationCard(station)
                            true
                        }
                    }
                    stationMarkers.add(marker)
                    map.overlays.add(marker)
                }
        }
        map.invalidate()
    }

    // ----------------------------------------------------------- Chargement

    private fun loadStations(showSpinner: Boolean) {
        lifecycleScope.launch {
            if (showSpinner) binding.loadingIndicator.visibility = View.VISIBLE
            try {
                StationRepository.refresh()
                // Les favoris gardent un instantané à jour pour le mode hors ligne
                FavoritesStore.refreshSnapshots(this@MainActivity, StationRepository.stations)
                updateStats()
                updateMarkers()
                if (pendingFocusStationId != -1L) {
                    focusStation(pendingFocusStationId)
                    pendingFocusStationId = -1L
                }
            } catch (e: Exception) {
                Snackbar.make(binding.mainRoot, R.string.error_network, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomBar)
                    .show()
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun updateStats() {
        val stations = StationRepository.stations
        if (stations.isEmpty()) return
        val totalBikes = stations.sumOf { it.bikesAvailable }
        binding.statsPill.text =
            "${stations.size} stations · $totalBikes vélos disponibles"
        binding.statsPillCard.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------ Recherche

    private fun setupSearch() {
        searchAdapter = SearchResultAdapter { station ->
            hideKeyboard()
            binding.searchResultsCard.visibility = View.GONE
            binding.searchInput.clearFocus()
            binding.mapView.controller.animateTo(GeoPoint(station.lat, station.lon), 17.0, 600L)
            showStationCard(station)
        }
        binding.searchResults.layoutManager = LinearLayoutManager(this)
        binding.searchResults.adapter = searchAdapter

        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString().orEmpty()
            val results = StationRepository.search(query).take(8)
            if (query.isBlank() || results.isEmpty()) {
                binding.searchResultsCard.visibility = View.GONE
            } else {
                searchAdapter.submit(results)
                binding.searchResultsCard.visibility = View.VISIBLE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    // -------------------------------------------------------------- Actions

    private fun setupButtons() {
        binding.buttonRefresh.setOnClickListener {
            it.animate().rotationBy(360f).setDuration(600).start()
            loadStations(showSpinner = false)
        }
        binding.navFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        binding.navNearby.setOnClickListener {
            startActivity(Intent(this, NearbyActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.buttonMyLocation.setOnClickListener {
            if (hasLocationPermission()) {
                enableMyLocation(centerOnFix = true)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        binding.cardClose.setOnClickListener { hideStationCard() }
        binding.cardDetailsButton.setOnClickListener {
            selectedStation?.let { station ->
                val intent = Intent(this, StationDetailActivity::class.java)
                intent.putExtra(StationDetailActivity.EXTRA_STATION_ID, station.stationId)
                startActivity(intent)
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun enableMyLocation(centerOnFix: Boolean) {
        if (!hasLocationPermission()) return

        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(this),
                binding.mapView
            ).apply { enableMyLocation() }
            binding.mapView.overlays.add(myLocationOverlay)
        }

        if (centerOnFix) {
            LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        binding.mapView.controller.animateTo(
                            GeoPoint(location.latitude, location.longitude), 16.5, 600L
                        )
                    }
                }
        }
    }

    // ------------------------------------------------- Carte station (fiche)

    private fun showStationCard(station: Station) {
        selectedStation = station
        binding.cardStationName.text = station.name
        binding.cardMechanical.text = station.mechanical.toString()
        binding.cardEbike.text = station.ebikes.toString()
        binding.cardDocks.text = station.docksAvailable.toString()

        if (binding.stationCard.visibility != View.VISIBLE) {
            binding.stationCard.alpha = 0f
            binding.stationCard.translationY = dp(24).toFloat()
            binding.stationCard.visibility = View.VISIBLE
            binding.stationCard.animate().alpha(1f).translationY(0f).setDuration(220).start()
        }
    }

    private fun hideStationCard() {
        selectedStation = null
        binding.stationCard.animate()
            .alpha(0f)
            .translationY(dp(24).toFloat())
            .setDuration(180)
            .withEndAction { binding.stationCard.visibility = View.GONE }
            .start()
    }

    // ------------------------------------------------------------ Lifecycle

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        // Une station peut avoir changé d'état favori / les données ont pu bouger
        selectedStation?.let { current ->
            StationRepository.findById(current.stationId)?.let { showStationCard(it) }
        }

        // Actualisation automatique des disponibilités
        if (SettingsStore.isAutoRefreshEnabled(this)) {
            autoRefreshJob = lifecycleScope.launch {
                while (isActive) {
                    delay(60_000)
                    loadStations(showSpinner = false)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }
}
