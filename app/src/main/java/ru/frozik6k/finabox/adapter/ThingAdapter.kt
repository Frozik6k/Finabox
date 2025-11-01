package ru.frozik6k.finabox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.frozik6k.finabox.dto.ThingDto
import ru.frozik6k.finabox.R

class ThingAdapter : RecyclerView.Adapter<ThingAdapter.ThingViewHolder>() {

    var data: List<ThingDto> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ThingViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.thing_item, parent, false)
        return ThingViewHolder(itemView)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(
        holder: ThingViewHolder,
        position: Int
    ) {
        holder.thingLetter.text = data[position].letter
        holder.thingName.text = data[position].name
    }

    class ThingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thingLetter: TextView = itemView.findViewById(R.id.tvLetter)
        val thingName: TextView = itemView.findViewById(R.id.tvName)
    }

}