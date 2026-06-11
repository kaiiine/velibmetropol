package fr.epf.sin2.velib_metropol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import fr.epf.sin2.velib_metropol.data.FavoritesStore
import fr.epf.sin2.velib_metropol.data.StationRepository
import fr.epf.sin2.velib_metropol.databinding.ActivityStationDetailBinding
import fr.epf.sin2.velib_metropol.model.Station
import fr.epf.sin2.velib_metropol.util.BlurUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StationDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATION_ID = "extra_station_id"
    }

    private lateinit var binding: ActivityStationDetailBinding
    private var station: Station? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        BlurUtils.apply(this, binding.headerBlur)

        val stationId = intent.getLongExtra(EXTRA_STATION_ID, -1L)

        // 1) Données fraîches en mémoire si disponibles,
        // 2) sinon instantané local du favori (mode hors connexion).
        val live = StationRepository.findById(stationId)
        val snapshot = FavoritesStore.find(this, stationId)

        when {
            live != null -> bindStation(live, offlineSince = null)
            snapshot != null -> bindStation(snapshot.station, offlineSince = snapshot.savedAtMillis)
            else -> {
                finish()
                return
            }
        }

        setupButtons(stationId)
    }

    private fun bindStation(station: Station, offlineSince: Long?) {
        this.station = station

        binding.detailStationName.text = station.name
        binding.detailStationCode.text = "Station n° ${station.stationCode}"
        binding.detailMechanical.text = station.mechanical.toString()
        binding.detailEbike.text = station.ebikes.toString()
        binding.detailDocks.text = station.docksAvailable.toString()
        binding.detailCapacity.text = station.capacity.toString()

        // Statut de la station
        if (station.isOperating) {
            binding.detailStatusChip.text = getString(R.string.station_open)
            binding.detailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.velib_green))
        } else {
            binding.detailStatusChip.text = getString(R.string.station_closed)
            binding.detailStatusChip.setTextColor(ContextCompat.getColor(this, R.color.danger_red))
        }

        // Jauge de remplissage
        val capacity = station.capacity.coerceAtLeast(1)
        binding.detailFillIndicator.max = capacity
        binding.detailFillIndicator.setProgress(station.bikesAvailable, true)
        binding.detailFillLabel.text =
            "${station.bikesAvailable} vélos sur ${station.capacity} emplacements"

        // Dernière mise à jour connue
        val format = SimpleDateFormat("dd/MM à HH:mm", Locale.FRANCE)
        binding.detailLastUpdate.text =
            getString(R.string.last_update, format.format(Date(station.lastReported * 1000)))

        // Bandeau hors connexion
        if (offlineSince != null) {
            binding.offlineBanner.text =
                getString(R.string.offline_banner, format.format(Date(offlineSince)))
            binding.offlineBanner.visibility = View.VISIBLE
        } else {
            binding.offlineBanner.visibility = View.GONE
        }

        updateFavoriteIcon(station.stationId)
    }

    private fun updateFavoriteIcon(stationId: Long) {
        isFavorite = FavoritesStore.isFavorite(this, stationId)
        binding.buttonFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        binding.buttonFavorite.contentDescription =
            getString(if (isFavorite) R.string.remove_favorite else R.string.add_favorite)
    }

    private fun setupButtons(stationId: Long) {
        binding.buttonBack.setOnClickListener { finish() }

        binding.buttonFavorite.setOnClickListener {
            val current = station ?: return@setOnClickListener
            if (isFavorite) {
                FavoritesStore.remove(this, current.stationId)
            } else {
                FavoritesStore.add(this, current)
            }
            it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()
            updateFavoriteIcon(current.stationId)
        }

        binding.buttonGoThere.setOnClickListener {
            val current = station ?: return@setOnClickListener
            val uri = Uri.parse(
                "geo:${current.lat},${current.lon}?q=${current.lat},${current.lon}(${Uri.encode(current.name)})"
            )
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.buttonShare.setOnClickListener {
            val current = station ?: return@setOnClickListener
            val text = "Station Vélib « ${current.name} » : " +
                    "${current.mechanical} vélos mécaniques, ${current.ebikes} électriques, " +
                    "${current.docksAvailable} places libres.\n" +
                    "https://www.google.com/maps?q=${current.lat},${current.lon}"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(share, getString(R.string.share)))
        }

        binding.buttonSeeOnMap.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_FOCUS_STATION_ID, stationId)
            }
            startActivity(intent)
        }
    }
}
