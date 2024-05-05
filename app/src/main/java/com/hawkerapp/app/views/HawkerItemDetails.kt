package com.hawkerapp.app.views

import com.hawkerapp.app.models.Item
import ItemAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fairmatic.hawkerapp.R
// TODO: Rename parameter arguments, choose names that match


private lateinit var recyclerView: RecyclerView
private lateinit var addButton: Button
private lateinit var itemAdapter: ItemAdapter
val itemsList = ArrayList<Item>()

class HawkerItemDetails : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hawker_item_details, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        addButton = view.findViewById(R.id.add_button)

        itemAdapter = ItemAdapter(itemsList)
        recyclerView.adapter = itemAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        addItem()

        addButton.setOnClickListener {
            addItem()
        }

        return view
    }

    private fun addItem() {
        val itemName = view?.findViewById<EditText>(R.id.itemName)
        val itemPrice = view?.findViewById<EditText>(R.id.itemPrice)
        val itemQuantity = view?.findViewById<EditText>(R.id.itemQuantity)

        val itemNameValue = itemName?.text.toString()
        val itemPriceValue = itemPrice?.text.toString().toIntOrNull() ?: 0
        val itemQuantityValue = itemQuantity?.text.toString().toIntOrNull() ?: 0

        val emptyItemIndex = itemsList.indexOfFirst { it.name.isNullOrEmpty() }

        if (emptyItemIndex != -1) {
            // If an empty item is found, update its values
            Log.d("HawkerItemDetails", "Updating empty item")
            val emptyItem = itemsList[emptyItemIndex]
            emptyItem.name = itemNameValue
            emptyItem.price = itemPriceValue
            emptyItem.quantity = itemQuantityValue
            itemAdapter.notifyItemChanged(emptyItemIndex)
        } else {
            if (itemsList.size == 1 && itemsList[0].name.isNullOrEmpty()) {
                Log.d("HawkerItemDetails", "Removing empty item")
                itemsList.removeAt(0)
                itemAdapter.notifyItemRemoved(0)
            }
            Log.d("HawkerItemDetails", "Adding new item")
            // Otherwise, add a new item to the list
            itemsList.add(Item(itemNameValue, itemPriceValue, itemQuantityValue))
            itemAdapter.notifyItemInserted(itemsList.size - 1)
            recyclerView.smoothScrollToPosition(itemsList.size - 1)
        }
        itemAdapter.notifyItemInserted(itemsList.size - 1)
        recyclerView.smoothScrollToPosition(itemsList.size - 1)


        itemName?.text?.clear()
        itemPrice?.text?.clear()
        itemQuantity?.text?.clear()
    }

    fun getItemsList(): List<Item> {
        return itemsList
    }
}