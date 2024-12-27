package com.hawkerapp.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.hawkerapp.app.models.Item

class ManageItemsAdapter(
    private val onUpdateClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : ListAdapter<Item, ManageItemsAdapter.ItemViewHolder>(ItemDiffCallback()) {

    class ItemViewHolder(
        itemView: View,
        private val onUpdateClick: (Item) -> Unit,
        private val onDeleteClick: (Item) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val updateButton: Button = itemView.findViewById(R.id.updateButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(item: Item) {
            itemName.text = item.name
            itemPrice.text = "₹${item.price}"
            itemQuantity.text = "Qty: ${item.quantity}"

            updateButton.setOnClickListener { onUpdateClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_item, parent, false)
        return ItemViewHolder(view, onUpdateClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item) =
        oldItem.name == newItem.name

    override fun areContentsTheSame(oldItem: Item, newItem: Item) =
        oldItem == newItem
}