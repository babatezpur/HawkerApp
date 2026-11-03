package com.hawkerapp.app.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class PlaceSuggestion(val mainText: String, val secondaryText: String, val fullText: String)

class PlaceSuggestionAdapter(
    private val onItemClick: (PlaceSuggestion) -> Unit
) : RecyclerView.Adapter<PlaceSuggestionAdapter.ViewHolder>() {

    private val suggestions = mutableListOf<PlaceSuggestion>()

    fun updateSuggestions(newSuggestions: List<PlaceSuggestion>) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = suggestions[position]
        holder.primary.text = item.mainText
        holder.secondary.text = item.secondaryText
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = suggestions.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val primary: TextView = view.findViewById(android.R.id.text1)
        val secondary: TextView = view.findViewById(android.R.id.text2)
    }
}
