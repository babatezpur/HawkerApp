package com.hawkerapp.app.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageItemsViewModel(application: Application) : AndroidViewModel(application) {
    private val hawkerManager = HawkerManager(application)

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    fun loadItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hawkerId = hawkerManager.getActiveHawkerId()
                hawkerId?.let {
                    val hawkerInfo = hawkerManager.getHawkerInfo(it)
                    _items.postValue(hawkerInfo.items ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("ManageItemsViewModel", "Error loading items: ${e.message}")
                _updateStatus.postValue(false)
            }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentItems = _items.value?.toMutableList() ?: mutableListOf()
                val index = currentItems.indexOfFirst { it.name == item.name }
                if (index != -1) {
                    currentItems[index] = item
                    val updatedHawker = hawkerManager.updateHawkerItems(getApplication(), currentItems)
                    updatedHawker?.let {
                        hawkerManager.updateItem(it.items)
                        loadItems() // Refresh the list
                        _updateStatus.postValue(true)
                    } ?: _updateStatus.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("ManageItemsViewModel", "Error updating item: ${e.message}")
                _updateStatus.postValue(false)
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentItems = _items.value?.toMutableList() ?: mutableListOf()
                currentItems.removeAll { it.name == item.name }
                val updatedHawker = hawkerManager.updateHawkerItems(getApplication(), currentItems)
                updatedHawker?.let {
                    hawkerManager.updateItem(it.items)
                    loadItems() // Refresh the list
                    _updateStatus.postValue(true)
                } ?: _updateStatus.postValue(false)
            } catch (e: Exception) {
                Log.e("ManageItemsViewModel", "Error deleting item: ${e.message}")
                _updateStatus.postValue(false)
            }
        }
    }

}