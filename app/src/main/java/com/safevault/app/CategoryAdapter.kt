package com.safevault.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.safevault.app.databinding.ItemCategoryChipBinding

class CategoryAdapter(
    private val categories: List<String>,
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val isSelected = position == selectedIndex
        val context = holder.itemView.context

        holder.binding.tvChip.text = category
        if (isSelected) {
            holder.binding.tvChip.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.binding.tvChip.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            holder.binding.tvChip.setBackgroundResource(R.drawable.bg_chip_unselected)
            holder.binding.tvChip.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.itemView.setOnClickListener {
            val prev = selectedIndex
            selectedIndex = holder.bindingAdapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(val binding: ItemCategoryChipBinding) :
        RecyclerView.ViewHolder(binding.root)
}
