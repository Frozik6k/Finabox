package ru.frozik6k.finabox.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import ru.frozik6k.finabox.R

class PhotoCarouselAdapter : RecyclerView.Adapter<PhotoCarouselAdapter.PhotoViewHolder>() {

    private var data: List<String> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = data[position]
        holder.photo.setImageURI(Uri.parse(uri))
    }

    fun submitList(newData: List<String>) {
        data = newData
        notifyDataSetChanged()
    }

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.ivCarouselPhoto)
    }
}