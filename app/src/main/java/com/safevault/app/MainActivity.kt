package com.safevault.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.safevault.app.databinding.ActivityMainBinding

class MainActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: VaultDatabaseHelper
    private lateinit var itemAdapter: VaultItemAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var currentCategory = "All Items"
    private var currentSearchQuery = ""

    private val categories = listOf(
        "All Items",
        "⭐ Favorites",
        "Social",
        "Email",
        "Banking",
        "Work",
        "Shopping",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify authentication
        if (!SecurityManager.isMasterPinSet(this)) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VaultDatabaseHelper(this)
        dbHelper.seedStarterDataIfNeeded()

        setupRecyclerViews()
        setupSearch()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshVaultList()
    }

    private fun setupRecyclerViews() {
        // Vault Items Adapter
        itemAdapter = VaultItemAdapter(
            onItemClick = { item ->
                val intent = Intent(this, CredentialDetailActivity::class.java).apply {
                    putExtra("item_id", item.id)
                }
                startActivity(intent)
            },
            onFavoriteToggle = { item ->
                val newStatus = !item.isFavorite
                dbHelper.toggleFavorite(item.id, newStatus)
                refreshVaultList()
            },
            onCopyUsername = { item ->
                ClipboardHelper.copyToClipboard(this, "Username", item.username, isSensitive = false)
            },
            onCopyPassword = { item ->
                val decrypted = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
                ClipboardHelper.copyToClipboard(this, "Password", decrypted, isSensitive = true)
            }
        )

        binding.rvVaultItems.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = itemAdapter
        }

        // Category Filter Chips
        categoryAdapter = CategoryAdapter(categories) { selectedCat ->
            currentCategory = selectedCat
            refreshVaultList()
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                refreshVaultList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditCredentialActivity::class.java)
            startActivity(intent)
        }

        binding.btnChangePin.setOnClickListener {
            val intent = Intent(this, ChangePinActivity::class.java)
            startActivity(intent)
        }

        binding.btnLockVault.setOnClickListener {
            // Lock vault and require PIN to re-enter
            val intent = Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun refreshVaultList() {
        val favoritesOnly = currentCategory == "⭐ Favorites"
        val filterCategory = if (favoritesOnly || currentCategory == "All Items") null else currentCategory

        val items = dbHelper.getAllItems(
            query = currentSearchQuery.ifEmpty { null },
            category = filterCategory,
            favoritesOnly = favoritesOnly
        )

        itemAdapter.submitList(items)

        // Empty state visibility
        if (items.isEmpty()) {
            binding.rvVaultItems.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            if (currentSearchQuery.isNotEmpty()) {
                binding.tvEmptyTitle.text = "No Matches Found"
                binding.tvEmptySubtitle.text = "No credentials match \"$currentSearchQuery\""
            } else {
                binding.tvEmptyTitle.text = "Vault is Empty"
                binding.tvEmptySubtitle.text = "Tap + below to store your passwords and sensitive notes safely"
            }
        } else {
            binding.rvVaultItems.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }

        // Stats update
        val stats = dbHelper.getVaultStats()
        binding.tvVaultStats.text = "${stats.totalCount} Records • 100% AES-256 Encrypted"
    }
}
