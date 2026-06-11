package fr.epf.sin2.velib_metropol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fr.epf.sin2.velib_metropol.databinding.ItemStationBinding
import fr.epf.sin2.velib_metropol.model.Station
import fr.epf.sin2.velib_metropol.util.GeoUtils

/**
 * Ligne de liste : une station, une distance optionnelle,
 * un sous-titre optionnel et son état favori.
 */
data class StationRow(
    val station: Station,
    val distanceMeters: Double? = null,
    val subtitle: String? = null,
    val isFavorite: Boolean = false
)

class StationAdapter(
    private val onClick: (Station) -> Unit,
    private val onFavoriteToggle: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.ViewHolder>() {

    private val items = mutableListOf<StationRow>()

    fun submit(rows: List<StationRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemStationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = items[position]
        val station = row.station

        with(holder.binding) {
            itemStationName.text = station.name
            itemStationMechanical.text = station.mechanical.toString()
            itemStationEbike.text = station.ebikes.toString()
            itemStationDocks.text = station.docksAvailable.toString()

            if (row.distanceMeters != null) {
                itemStationDistance.text = GeoUtils.formatDistance(row.distanceMeters)
                itemStationDistance.visibility = View.VISIBLE
            } else {
                itemStationDistance.visibility = View.GONE
            }

            if (row.subtitle != null) {
                itemStationSubtitle.text = row.subtitle
                itemStationSubtitle.visibility = View.VISIBLE
            } else {
                itemStationSubtitle.visibility = View.GONE
            }

            itemStationFavorite.setImageResource(
                if (row.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )

            root.setOnClickListener { onClick(station) }
            itemStationFavorite.setOnClickListener { onFavoriteToggle(station) }
        }
    }

    override fun getItemCount(): Int = items.size
}
