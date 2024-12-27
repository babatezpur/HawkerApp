package com.hawkerapp.app.views

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.hawkerapp.app.adapters.ManageItemsAdapter
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ManageItemsActivity.kt
class ManageItemsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ManageItemsAdapter
    private lateinit var hawkerManager: HawkerManager
    private var currentItems: MutableList<Item> = mutableListOf()  // Track current items

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_items)

        hawkerManager = HawkerManager(application)
        setupRecyclerView()
        loadItems()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.itemsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ManageItemsAdapter(
            onUpdateClick = { item -> showUpdateDialog(item) },
            onDeleteClick = { item -> showDeleteConfirmation(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun loadItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val hawkerId = hawkerManager.getActiveHawkerId()
            hawkerId?.let {
                val hawkerInfo = hawkerManager.getHawkerInfo(it)
                withContext(Dispatchers.Main) {
                    currentItems.clear()
                    currentItems.addAll(hawkerInfo.items ?: emptyList()) // Provide empty list if null
                    adapter.submitList(currentItems.toList())
                }
            }
        }
    }

    private fun showUpdateDialog(item: Item) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_update_item)

        val priceEditText = dialog.findViewById<EditText>(R.id.priceEditText)
        val quantityEditText = dialog.findViewById<EditText>(R.id.quantityEditText)
        val updateButton = dialog.findViewById<Button>(R.id.updateButton)

        priceEditText.setText(item.price.toString())
        quantityEditText.setText(item.quantity.toString())

        updateButton.setOnClickListener {
            val newPrice = priceEditText.text.toString().toIntOrNull()
            val newQuantity = quantityEditText.text.toString().toIntOrNull()

            if (newPrice != null && newQuantity != null) {
                // Find item index and update in the list
                val index = currentItems.indexOfFirst { it.name == item.name }
                if (index != -1) {
                    currentItems[index] = item.copy(price = newPrice, quantity = newQuantity)
                    updateItem(currentItems.toList())
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(item: Item) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete ${item.name}?")
            .setPositiveButton("Yes") { _, _ ->
                val index = currentItems.indexOfFirst { it.name == item.name }
                if (index != -1) {
                    currentItems.removeAt(index)
                    updateItem(currentItems.toList())
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateItem(updatedItems: List<Item>) {
        lifecycleScope.launch {
            hawkerManager.updateItem(updatedItems)
            loadItems() // Reload the list
        }
    }

    private fun deleteItem(item: Item) {
        lifecycleScope.launch {
            hawkerManager.deleteItem(item)
            loadItems() // Reload the list
        }
    }
}
