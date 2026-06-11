package fr.epf.sin2.velib_metropol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fr.epf.sin2.velib_metropol.databinding.ItemSearchResultBinding
import fr.epf.sin2.velib_metropol.model.Station

class SearchResultAdapter(
    private val onClick: (Station) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private val items = mutableListOf<Station>()

    fun submit(stations: List<Station>) {
        items.clear()
        items.addAll(stations)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = items[position]
        holder.binding.searchResultName.text = station.name
        holder.binding.searchResultBikes.text = "${station.bikesAvailable} vélos"
        holder.binding.root.setOnClickListener { onClick(station) }
    }

    override fun getItemCount(): Int = items.size
}
