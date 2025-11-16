package ru.frozik6k.finabox.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import ru.frozik6k.finabox.R

class PhotoPreviewAdapter : RecyclerView.Adapter<PhotoPreviewAdapter.PhotoViewHolder>() {

    private var data: List<String> = emptyList()
    var onRemove: (String) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_preview, parent, false)
        return PhotoViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = data[position]
        holder.photo.setImageURI(Uri.parse(uri))
        holder.removeButton.setOnClickListener { onRemove(uri) }
    }

    fun submitList(newData: List<String>) {
        data = newData
        notifyDataSetChanged()
    }

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photo: ImageView = view.findViewById(R.id.ivPhoto)
        val removeButton: ImageButton = view.findViewById(R.id.btnRemovePhoto)
    }
}