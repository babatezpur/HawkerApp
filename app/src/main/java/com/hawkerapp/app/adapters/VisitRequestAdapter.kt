package com.hawkerapp.app.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.hawkerapp.app.models.UserRequestData

class VisitRequestAdapter(
    private var users: List<UserRequestData>,
    private val onItemClick: (UserRequestData) -> Unit
) : RecyclerView.Adapter<VisitRequestAdapter.UserViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<UserRequestData>) {
        users = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.visit_request_list_info_item_layout, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = users[position]
        holder.bind(currentUser)
        holder.itemView.setOnClickListener {
            onItemClick(currentUser)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val distanceTextView: TextView = itemView.findViewById(R.id.distanceTextView)
        private val otherInfoTextView: TextView = itemView.findViewById(R.id.otherInfoTextView)

        @SuppressLint("SetTextI18n")
        fun bind(user: UserRequestData) {
            userNameTextView.text = user.customerName
            distanceTextView.text = "${user.distance}" // Assuming distance is in kilometers, adjust accordingly
            otherInfoTextView.text = user.notes
        }
    }
}
