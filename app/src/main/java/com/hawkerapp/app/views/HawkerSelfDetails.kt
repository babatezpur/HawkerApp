package com.hawkerapp.app.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.fairmatic.hawkerapp.R

class HawkerSelfDetails : Fragment() {


    var editTextName : EditText? = null
    var editTextCategory : EditText? = null
    var editTextPhone : EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_hawker_self_details, container, false)
        editTextName = view?.findViewById(R.id.name_edit_text)
        editTextCategory = view?.findViewById(R.id.category_edit_text)
        editTextPhone = view?.findViewById(R.id.phone_edit_text)

        return view

    }

    fun areAllDataFilled(): Boolean {
        val a = editTextName?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val b = editTextCategory?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val c = editTextPhone?.text?.toString()?.trim()?.isNotEmpty() ?: false
        return a && b && c
    }
}