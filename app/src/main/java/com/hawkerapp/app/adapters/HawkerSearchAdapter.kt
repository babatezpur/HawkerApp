package com.hawkerapp.app.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hawkerapp.app.R
import com.hawkerapp.app.models.HawkerInfo

class HawkerAdapter(private val onItemClick: (HawkerInfo) -> Unit) : RecyclerView.Adapter<HawkerViewHolder>() {
    private val hawkers = mutableListOf<HawkerInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HawkerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hawker, parent, false)
        return HawkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: HawkerViewHolder, position: Int) {
        val hawkerInfo = hawkers[position]
        holder.bind(hawkerInfo)
        holder.itemView.setOnClickListener { onItemClick(hawkerInfo) }
    }

    override fun getItemCount(): Int = hawkers.size

    fun updateHawkers(newHawkers: List<HawkerInfo>) {
        hawkers.clear()
        hawkers.addAll(newHawkers)
        Log.d("BottomSheet","Hawkers in updateHawkers of Adapter: ${hawkers}")
        notifyDataSetChanged()
    }
}

// HawkerViewHolder.kt
class HawkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val hawkerImageView: ImageView = itemView.findViewById(R.id.hawkerImageView)
    private val hawkerNameTextView: TextView = itemView.findViewById(R.id.hawkerNameTextView)
    private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)

    fun bind(hawkerInfo: HawkerInfo) {
        Glide.with(itemView.context)
            .load(hawkerInfo.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(hawkerImageView)

        hawkerNameTextView.text = hawkerInfo.name
        distanceTextView.text = "${hawkerInfo.distance} km"
    }
}