package com.safevault.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.safevault.app.databinding.ItemVaultTableRowBinding
import java.util.Locale

class VaultTableAdapter(
    private val onItemClick: (VaultItem) -> Unit,
    private val onFavoriteToggle: (VaultItem) -> Unit,
    private val onCopyUsername: (VaultItem) -> Unit,
    private val onCopyPassword: (VaultItem) -> Unit
) : RecyclerView.Adapter<VaultTableAdapter.TableViewHolder>() {

    private val items = mutableListOf<VaultItem>()
    private val revealedItemIds = mutableSetOf<Long>()

    fun submitList(newItems: List<VaultItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val binding = ItemVaultTableRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    inner class TableViewHolder(private val binding: ItemVaultTableRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VaultItem, index: Int) {
            val context = binding.root.context

            // Alternating row background
            val bgColorRes = if (index % 2 == 0) R.color.vault_card else R.color.vault_surface
            binding.rowRoot.setBackgroundColor(ContextCompat.getColor(context, bgColorRes))

            // Index
            binding.tvRowIndex.text = index.toString()

            // Avatar initial & background
            val initial = if (item.title.isNotBlank()) item.title.first().uppercase() else "V"
            binding.tvRowAvatar.text = initial
            val avatarColor = getCategoryColor(item.category)
            binding.tvRowAvatar.background.setTint(ContextCompat.getColor(context, avatarColor))

            // Title & Portal
            binding.tvRowTitle.text = item.title
            binding.tvRowPortal.text = item.portal

            // Username
            binding.tvRowUsername.text = if (item.username.isNotBlank()) item.username else "—"

            // Password with eye reveal
            val isRevealed = revealedItemIds.contains(item.id)
            if (isRevealed) {
                val decrypted = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
                binding.tvRowPassword.text = decrypted
                binding.btnRowTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.tvRowPassword.text = "••••••••"
                binding.btnRowTogglePassword.setImageResource(R.drawable.ic_eye)
            }

            // Category badge
            binding.tvRowCategory.text = item.category
            binding.tvRowCategory.background.setTint(ContextCompat.getColor(context, avatarColor))

            // Strength badge
            val strengthLevel = try {
                PasswordUtils.StrengthLevel.valueOf(item.passwordStrength.uppercase(Locale.ROOT))
            } catch (_: Exception) {
                PasswordUtils.StrengthLevel.MEDIUM
            }
            binding.tvRowStrength.text = strengthLevel.label
            binding.tvRowStrength.setBackgroundResource(strengthLevel.bgRes)
            binding.tvRowStrength.setTextColor(ContextCompat.getColor(context, strengthLevel.colorRes))

            // Favorite star
            if (item.isFavorite) {
                binding.btnRowFavorite.setImageResource(R.drawable.ic_star_filled)
                binding.btnRowFavorite.setColorFilter(ContextCompat.getColor(context, R.color.security_amber))
            } else {
                binding.btnRowFavorite.setImageResource(R.drawable.ic_star_outline)
                binding.btnRowFavorite.setColorFilter(ContextCompat.getColor(context, R.color.text_muted))
            }

            // Click listeners
            binding.rowRoot.setOnClickListener { onItemClick(item) }
            binding.btnRowDetails.setOnClickListener { onItemClick(item) }

            binding.btnRowCopyUsername.setOnClickListener {
                if (item.username.isNotBlank()) onCopyUsername(item)
            }

            binding.btnRowCopyPassword.setOnClickListener {
                onCopyPassword(item)
            }

            binding.btnRowTogglePassword.setOnClickListener {
                if (revealedItemIds.contains(item.id)) {
                    revealedItemIds.remove(item.id)
                } else {
                    revealedItemIds.add(item.id)
                }
                val pos = bindingAdapterPosition
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }

            binding.btnRowFavorite.setOnClickListener {
                onFavoriteToggle(item)
            }
        }

        private fun getCategoryColor(category: String): Int {
            return when (category.lowercase(Locale.ROOT)) {
                "social" -> R.color.cat_social
                "email" -> R.color.cat_email
                "banking" -> R.color.cat_banking
                "work" -> R.color.cat_work
                "shopping" -> R.color.cat_shopping
                else -> R.color.cat_other
            }
        }
    }
}
