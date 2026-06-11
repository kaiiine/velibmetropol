package fr.epf.sin2.velib_metropol

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import fr.epf.sin2.velib_metropol.data.FavoritesStore
import fr.epf.sin2.velib_metropol.data.StationRepository
import fr.epf.sin2.velib_metropol.databinding.ActivityFavoritesBinding
import fr.epf.sin2.velib_metropol.util.BlurUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Liste des stations favorites, accessible hors connexion :
 * chaque favori embarque un instantané complet de la station.
 */
class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: StationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.favoritesRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        BlurUtils.apply(this, binding.headerBlur, binding.offlineBannerCard)

        binding.buttonBack.setOnClickListener { finish() }

        adapter = StationAdapter(
            onClick = { station ->
                val intent = Intent(this, StationDetailActivity::class.java)
                intent.putExtra(StationDetailActivity.EXTRA_STATION_ID, station.stationId)
                startActivity(intent)
            },
            onFavoriteToggle = { station ->
                FavoritesStore.remove(this, station.stationId)
                showFavorites()
            }
        )
        binding.favoritesList.layoutManager = LinearLayoutManager(this)
        binding.favoritesList.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refreshOnline() }

        refreshOnline()
    }

    override fun onResume() {
        super.onResume()
        showFavorites()
    }

    /** Affiche les favoris stockés localement (fonctionne sans réseau). */
    private fun showFavorites() {
        val format = SimpleDateFormat("dd/MM à HH:mm", Locale.FRANCE)
        val rows = FavoritesStore.getAll(this)
            .sortedBy { it.station.name }
            .map { favorite ->
                StationRow(
                    station = favorite.station,
                    subtitle = "Synchronisé le ${format.format(Date(favorite.savedAtMillis))}",
                    isFavorite = true
                )
            }
        adapter.submit(rows)
        binding.emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Tente une mise à jour réseau des instantanés ; silencieux si hors ligne. */
    private fun refreshOnline() {
        lifecycleScope.launch {
            try {
                StationRepository.refresh()
                FavoritesStore.refreshSnapshots(this@FavoritesActivity, StationRepository.stations)
                binding.offlineBannerCard.visibility = View.GONE
            } catch (e: Exception) {
                binding.offlineBanner.text = getString(R.string.error_network)
                binding.offlineBannerCard.visibility = View.VISIBLE
            } finally {
                binding.swipeRefresh.isRefreshing = false
                showFavorites()
            }
        }
    }
}
