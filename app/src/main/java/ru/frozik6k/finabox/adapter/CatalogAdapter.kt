package ru.frozik6k.finabox.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
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
        val letterBackground = holder.thingLetter.background
        if (letterBackground is GradientDrawable) {
            letterBackground.mutate()
            letterBackground.setColor(color)
        } else {
            holder.thingLetter.setBackgroundColor(color)
        }

        val textColor = MaterialColors.getColor(
            holder.itemView,
            com.google.android.material.R.attr.colorOnSurface
        )
        holder.thingName.setTextColor(textColor)
        holder.itemView.setOnClickListener { onItemClick(entry) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(entry)
            true
        }
    }

    class CatalogEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thingLetter: TextView = itemView.findViewById(R.id.tvLetter)
        val thingName: TextView = itemView.findViewById(R.id.tvName)
        val photoCarouselAdapter = PhotoCarouselAdapter()

    }
}