package com.hawkerapp.app.utils

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText

class HawkerSearchUtils {
    fun setupSearchInput(
        searchInput: EditText,
        onSearch: (String) -> Unit,
        onEmptySearch: () -> Unit
    ) {
        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                handleSearch(searchInput.text.toString(), onSearch, onEmptySearch)
                true
            } else {
                false
            }
        }
    }

    private fun handleSearch(
        searchText: String,
        onSearch: (String) -> Unit,
        onEmptySearch: () -> Unit
    ) {
        if (searchText.isNotEmpty()) {
            onSearch(searchText)
        } else {
            onEmptySearch()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupSearchClear(
        searchInput: EditText,
        onClear: () -> Unit
    ) {
        searchInput.setOnTouchListener { v, event ->
            val drawableEnd = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchInput.right - searchInput.compoundDrawables[drawableEnd].bounds.width())) {
                    searchInput.text.clear()
                    onClear()
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}