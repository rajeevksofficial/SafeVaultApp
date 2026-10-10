package com.safevault.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.safevault.app.databinding.ItemVaultCardBinding
import java.util.Locale

class VaultItemAdapter(
    private val onItemClick: (VaultItem) -> Unit,
    private val onFavoriteToggle: (VaultItem) -> Unit,
    private val onCopyUsername: (VaultItem) -> Unit,
    private val onCopyPassword: (VaultItem) -> Unit
) : RecyclerView.Adapter<VaultItemAdapter.VaultViewHolder>() {

    private val items = mutableListOf<VaultItem>()
    private val revealedIds = mutableSetOf<Long>()

    fun submitList(newItems: List<VaultItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition] == newItems[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val binding = ItemVaultCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VaultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VaultViewHolder(private val binding: ItemVaultCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VaultItem) {
            val context = binding.root.context

            // Title & Portal
            binding.tvTitle.text = item.title
            binding.tvPortal.text = item.portal
            binding.tvUsername.text = item.username

            // Avatar initial & background color
            val initial = if (item.title.isNotEmpty()) item.title.first().uppercase() else "V"
            binding.tvAvatarInitial.text = initial
            val avatarColor = getPortalColor(item.portal, item.category)
            binding.layoutAvatar.background.setTint(avatarColor)

            // Favorite star
            if (item.isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_star_filled)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_star_outline)
            }

            // Password Reveal / Mask
            val isRevealed = revealedIds.contains(item.id)
            if (isRevealed) {
                val decrypted = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
                binding.tvPassword.text = decrypted
                binding.btnToggleReveal.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.tvPassword.text = "••••••••••••"
                binding.btnToggleReveal.setImageResource(R.drawable.ic_eye)
            }

            binding.btnToggleReveal.setOnClickListener {
                if (revealedIds.contains(item.id)) {
                    revealedIds.remove(item.id)
                } else {
                    revealedIds.add(item.id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            // Category Badge
            binding.tvCategoryBadge.text = item.category

            // Strength Badge
            when (item.passwordStrength.uppercase(Locale.ROOT)) {
                "VERY_STRONG" -> {
                    binding.tvStrengthBadge.text = "Very Strong"
                    binding.tvStrengthBadge.setBackgroundResource(R.drawable.bg_pill_strength_strong)
                    binding.tvStrengthBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                }
                "STRONG" -> {
                    binding.tvStrengthBadge.text = "Strong"
                    binding.tvStrengthBadge.setBackgroundResource(R.drawable.bg_pill_strength_strong)
                    binding.tvStrengthBadge.setTextColor(ContextCompat.getColor(context, R.color.security_emerald))
                }
                "MEDIUM" -> {
                    binding.tvStrengthBadge.text = "Medium"
                    binding.tvStrengthBadge.setBackgroundResource(R.drawable.bg_pill_strength_medium)
                    binding.tvStrengthBadge.setTextColor(ContextCompat.getColor(context, R.color.security_amber))
                }
                else -> {
                    binding.tvStrengthBadge.text = "Weak"
                    binding.tvStrengthBadge.setBackgroundResource(R.drawable.bg_pill_strength_weak)
                    binding.tvStrengthBadge.setTextColor(ContextCompat.getColor(context, R.color.security_crimson))
                }
            }

            // Notes Indicator
            if (item.notesEncrypted.isNotEmpty()) {
                binding.tvNotesIndicator.visibility = View.VISIBLE
            } else {
                binding.tvNotesIndicator.visibility = View.GONE
            }

            // Click listeners
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnFavorite.setOnClickListener { onFavoriteToggle(item) }
            binding.btnCopyUsername.setOnClickListener { onCopyUsername(item) }
            binding.btnCopyPassword.setOnClickListener { onCopyPassword(item) }
        }

        private fun getPortalColor(portal: String, category: String): Int {
            val p = portal.lowercase(Locale.ROOT)
            return when {
                p.contains("facebook") -> Color.parseColor("#1877F2")
                p.contains("gmail") || p.contains("google") -> Color.parseColor("#EA4335")
                p.contains("github") -> Color.parseColor("#24292E")
                p.contains("netflix") -> Color.parseColor("#E50914")
                p.contains("hdfc") || p.contains("bank") -> Color.parseColor("#004C8F")
                p.contains("amazon") -> Color.parseColor("#FF9900")
                category == "Banking" -> Color.parseColor("#10B981")
                category == "Social" -> Color.parseColor("#3B82F6")
                category == "Work" -> Color.parseColor("#8B5CF6")
                else -> Color.parseColor("#6366F1")
            }
        }
    }
}
