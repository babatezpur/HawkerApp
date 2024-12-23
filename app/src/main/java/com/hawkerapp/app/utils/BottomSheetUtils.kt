package com.hawkerapp.app.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.R

class BottomSheetUtils(private val context: Context) {
    fun showHawkerDetails(
        hawkerInfo: HawkerInfo,
        layoutInflater: LayoutInflater,
        onCallButtonClick: (HawkerInfo) -> Unit
    ): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_info, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        setupBottomSheetUI(bottomSheetView, hawkerInfo, bottomSheetDialog, onCallButtonClick)
        return bottomSheetDialog
    }

    private fun setupBottomSheetUI(
        view: View,
        hawkerInfo: HawkerInfo,
        dialog: BottomSheetDialog,
        onCallButtonClick: (HawkerInfo) -> Unit
    ) {
        view.apply {
            findViewById<TextView>(R.id.titleTextView).apply {
                text = hawkerInfo.name
                setTextColor(Color.BLUE)
            }

            setupHawkerImage(findViewById(R.id.hawkerImageView), hawkerInfo.imageUrl)
            setupItemsList(findViewById(R.id.itemsListView), hawkerInfo)
            setupCallButton(findViewById(R.id.callButton)) { onCallButtonClick(hawkerInfo) }
        }

        dialog.apply {
            window?.setDimAmount(0.5f)
            setCancelable(true)
            behavior.apply {
                state = BottomSheetBehavior.STATE_HALF_EXPANDED
                peekHeight = 600
            }
            show()
        }
    }

    private fun setupHawkerImage(imageView: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)
        }
    }

    private fun setupItemsList(listView: ListView, hawkerInfo: HawkerInfo) {
        val itemNamesAndPrices = hawkerInfo.items.map { "${it.name}: ${it.price}" }
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemNamesAndPrices)
        listView.adapter = adapter
    }

    private fun setupCallButton(button: ImageButton, onClick: () -> Unit) {
        button.setOnClickListener { onClick() }
    }

    fun showCallDialog(
        hawkerInfo: HawkerInfo,
        layoutInflater: LayoutInflater,
        onCallRequest: (HawkerInfo, String, String) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_call_hawker, null)
        val alertDialog = createCallDialog(dialogView)

        setupCallDialogButton(
            dialog = alertDialog,
            dialogView = dialogView,
            hawkerInfo = hawkerInfo,
            onCallRequest = onCallRequest
        )

        alertDialog.show()
    }

    private fun createCallDialog(dialogView: View): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle("CALL HAWKER")
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("SEND REQUEST", null)
            .create()
    }

    private fun setupCallDialogButton(
        dialog: AlertDialog,
        dialogView: View,
        hawkerInfo: HawkerInfo,
        onCallRequest: (HawkerInfo, String, String) -> Unit
    ) {
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val noteEditText = dialogView.findViewById<EditText>(R.id.noteEditText)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.GREEN)

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Call") { dialogInterface, _ ->
            val name = nameEditText.text.toString().trim()
            val note = noteEditText.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(context, "Name is mandatory", Toast.LENGTH_SHORT).show()
            } else {
                onCallRequest(hawkerInfo, name, note)
                dialogInterface.dismiss()
            }
        }
    }
}