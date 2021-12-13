package com.example.magazine

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(private val dataSet: List<Drawable>, private val onItemClickListener: OnItemClickListener) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: Drawable?)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: AppCompatImageView = view.findViewById(R.id.img)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.img.setImageDrawable(dataSet[position])
        viewHolder.img.setOnClickListener { onItemClickListener.onItemClick(dataSet[position]) }
    }

    override fun getItemCount() = dataSet.size

}