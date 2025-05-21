package com.codzure.cryptalk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codzure.cryptalk.R
import com.codzure.cryptalk.data.User
import com.codzure.cryptalk.data.displayName
import com.codzure.cryptalk.data.initials
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView

/**
 * Adapter for displaying user search results
 */
class UserSearchAdapter(
    private val onUserSelected: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return UserViewHolder(view, onUserSelected)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        itemView: View,
        private val onUserSelected: (User) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ShapeableImageView = itemView.findViewById(R.id.userAvatar)
        private val userInitials: TextView = itemView.findViewById(R.id.userInitials)
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val userPhone: TextView = itemView.findViewById(R.id.userPhone)
        private val startChatButton: MaterialButton = itemView.findViewById(R.id.startChatButton)

        fun bind(user: User) {
            userName.text = user.displayName()
            userPhone.text = user.phoneNumber
            userInitials.text = user.initials()
            
            // Set avatar image if available, otherwise show initials
            if (user.avatarUrl != null) {
                userInitials.visibility = View.GONE
                // Load image with your preferred image loading library, e.g.:
                // Glide.with(itemView.context).load(user.avatarUrl).into(userAvatar)
            } else {
                userInitials.visibility = View.VISIBLE
            }

            // Set click listeners
            startChatButton.setOnClickListener {
                onUserSelected(user)
            }
            
            itemView.setOnClickListener {
                onUserSelected(user)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
