package com.hawkerapp.app.views

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.hawkerapp.app.adapters.ManageItemsAdapter
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.Item
import com.hawkerapp.app.viewmodels.ManageItemsViewModel
// ManageItemsActivity.kt
class ManageItemsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ManageItemsAdapter
    private lateinit var viewModel: ManageItemsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_items)

        viewModel = ViewModelProvider(this)[ManageItemsViewModel::class.java]
        setupRecyclerView()
        setupObservers()
        viewModel.loadItems() // Initial load
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

    private fun setupObservers() {
        viewModel.items.observe(this) { items ->
            adapter.submitList(items)
        }

        viewModel.updateStatus.observe(this) { success ->
            if (!success) {
                Toast.makeText(this, "Failed to update items", Toast.LENGTH_SHORT).show()
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
                val updatedItem = item.copy(price = newPrice, quantity = newQuantity)
                viewModel.updateItem(item = updatedItem)
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
                viewModel.deleteItem(item)
            }
            .setNegativeButton("No", null)
            .show()
    }

//    private fun updateItem(updatedItems: List<Item>) {
//        lifecycleScope.launch(Dispatchers.IO) {  // Change this line to specify Dispatchers.IO
//            try {
//                val updatedHawker = hawkerManager.updateHawkerItems(this@ManageItemsActivity, updatedItems)
//                updatedHawker?.let {
//                    hawkerManager.updateItem(it.items)
//                    withContext(Dispatchers.Main) {  // Switch back to Main thread for UI updates
//                        loadItems() // Reload the list
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {  // Switch to Main thread for error handling
//                    // Handle error
//                }
//            }
//        }
//    }

//    private fun deleteItem(item: Item) {
//        lifecycleScope.launch {
//            hawkerManager.deleteItem(item)
//            loadItems() // Reload the list
//        }
//    }
}
