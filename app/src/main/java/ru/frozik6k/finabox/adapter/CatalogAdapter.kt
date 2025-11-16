package ru.frozik6k.finabox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ru.frozik6k.finabox.R
import ru.frozik6k.finabox.dto.CatalogDto
import ru.frozik6k.finabox.dto.CatalogType

class CatalogAdapter : RecyclerView.Adapter<CatalogAdapter.CatalogEntryViewHolder>() {

    var data: List<CatalogDto> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    var onItemClick: (CatalogDto) -> Unit = {}
    var onItemLongClick: (CatalogDto) -> Unit = {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CatalogEntryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.thing_item, parent, false)
        return CatalogEntryViewHolder(itemView)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(
        holder: CatalogEntryViewHolder,
        position: Int
    ) {
        val entry = data[position]
        holder.thingLetter.text = entry.letter
        holder.thingName.text = entry.name
        val colorRes = if (entry.type == CatalogType.THING) {
            R.color.catalog_item_thing
        } else {
            R.color.catalog_item_box
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorRes)
        holder.thingLetter.setTextColor(color)
        holder.thingName.setTextColor(color)
        holder.itemView.setOnClickListener { onItemClick(entry) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(entry)
            true
        }
    }

    class CatalogEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thingLetter: TextView = itemView.findViewById(R.id.tvLetter)
        val thingName: TextView = itemView.findViewById(R.id.tvName)
    }
}