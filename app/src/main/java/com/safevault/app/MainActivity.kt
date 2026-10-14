package com.safevault.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.safevault.app.databinding.ActivityMainBinding

class MainActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: VaultDatabaseHelper
    private lateinit var itemAdapter: VaultItemAdapter
    private lateinit var tableAdapter: VaultTableAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var currentCategory = "All Items"
    private var currentSearchQuery = ""
    private var isTableViewActive = false
    private var pendingExportCsvContent: String? = null

    private val openCsvLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            importCsvFromUri(uri)
        }
    }

    private val createCsvDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null && pendingExportCsvContent != null) {
            writeCsvToUri(uri, pendingExportCsvContent!!)
        }
    }

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

        // Vault Table Adapter (Tabular Grid View)
        tableAdapter = VaultTableAdapter(
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

        binding.rvVaultTable.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = tableAdapter
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

        // View Mode Switcher: Cards vs Table
        binding.btnViewCards.setOnClickListener {
            setTableViewMode(false)
        }

        binding.btnViewTable.setOnClickListener {
            setTableViewMode(true)
        }

        binding.btnImportCsv.setOnClickListener {
            showImportCsvDialog()
        }

        binding.btnExportCsv.setOnClickListener {
            showExportCsvDialog()
        }

        binding.btnEmptyImportCsv.setOnClickListener {
            showImportCsvDialog()
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

    private fun setTableViewMode(active: Boolean) {
        isTableViewActive = active
        if (active) {
            binding.btnViewTable.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnViewTable.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnViewCards.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnViewCards.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.btnViewCards.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnViewCards.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnViewTable.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnViewTable.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
        refreshVaultList()
    }

    private fun showImportCsvDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.import_csv_title)
            .setMessage(R.string.import_csv_desc)
            .setPositiveButton(R.string.select_csv_file) { _, _ ->
                try {
                    openCsvLauncher.launch(
                        arrayOf(
                            "text/comma-separated-values",
                            "text/csv",
                            "text/plain",
                            "application/csv",
                            "application/vnd.ms-excel",
                            "*/*"
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(R.string.copy_sample_template) { _, _ ->
                val sample = CsvImporter.getSampleCsvTemplate()
                ClipboardHelper.copyToClipboard(this, "CSV Template", sample, isSensitive = false)
                Toast.makeText(this, "Sample CSV template copied to clipboard!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importCsvFromUri(uri: Uri) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Importing Credentials…")
            .setMessage("Reading and encrypting records with AES-256 GCM…")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to open selected CSV file.", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val result = inputStream.use { CsvImporter.parseCsv(it) }

                if (result.validItems.isNotEmpty()) {
                    val insertedCount = dbHelper.insertBatch(result.validItems)
                    runOnUiThread {
                        progressDialog.dismiss()
                        refreshVaultList()
                        showImportSummaryDialog(insertedCount, result)
                    }
                } else {
                    runOnUiThread {
                        progressDialog.dismiss()
                        val errorMsg = if (result.errorMessages.isNotEmpty()) {
                            result.errorMessages.joinToString("\n")
                        } else {
                            "No valid credential records found in the selected CSV file."
                        }
                        AlertDialog.Builder(this)
                            .setTitle("CSV Import Failed")
                            .setMessage(errorMsg)
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Copy Template") { _, _ ->
                                val sample = CsvImporter.getSampleCsvTemplate()
                                ClipboardHelper.copyToClipboard(this, "CSV Template", sample, isSensitive = false)
                                Toast.makeText(this, "Sample CSV template copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error during CSV import: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showImportSummaryDialog(insertedCount: Int, result: CsvImporter.CsvImportResult) {
        val sb = StringBuilder()
        sb.append("✅ Successfully imported and encrypted $insertedCount credentials with AES-256 GCM!\n\n")

        if (result.skippedCount > 0) {
            sb.append("⚠️ ${result.skippedCount} rows were skipped due to missing data.\n\n")
        }

        if (result.sampleItemTitles.isNotEmpty()) {
            sb.append("Imported records include:\n")
            for (title in result.sampleItemTitles.take(5)) {
                sb.append("• ").append(title).append("\n")
            }
            if (insertedCount > 5) {
                sb.append("• … and ").append(insertedCount - 5).append(" more.\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("CSV Import Complete")
            .setMessage(sb.toString().trim())
            .setPositiveButton("View Vault", null)
            .show()
    }

    private fun showExportCsvDialog() {
        val allItems = dbHelper.getAllItems()
        if (allItems.isEmpty()) {
            Toast.makeText(this, "No credentials found in vault to export.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.export_csv_title)
            .setMessage(getString(R.string.export_csv_warning))
            .setPositiveButton(R.string.export_save_file) { _, _ ->
                val csvContent = CsvExporter.exportToCsv(allItems)
                pendingExportCsvContent = csvContent
                val defaultFileName = CsvExporter.generateExportFileName()
                try {
                    createCsvDocumentLauncher.launch(defaultFileName)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open file saver: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(R.string.export_share_file) { _, _ ->
                val csvContent = CsvExporter.exportToCsv(allItems)
                shareCsvContent(csvContent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun writeCsvToUri(uri: Uri, csvContent: String) {
        Thread {
            try {
                contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                runOnUiThread {
                    pendingExportCsvContent = null
                    AlertDialog.Builder(this)
                        .setTitle("CSV Export Successful")
                        .setMessage("Vault credentials exported and saved to CSV successfully!\n\nRemember to keep the exported file confidential.")
                        .setPositiveButton("Done", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to save CSV file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun shareCsvContent(csvContent: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "SafeVault Credentials Export")
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Vault CSV")
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share CSV: ${e.message}", Toast.LENGTH_SHORT).show()
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
        tableAdapter.submitList(items)

        // Stats update
        val stats = dbHelper.getVaultStats()
        binding.tvVaultStats.text = "${stats.totalCount} Records • 100% AES-256 Encrypted"
        binding.tvFilteredCount.text = "Showing ${items.size} credential${if (items.size != 1) "s" else ""}"

        // Empty state visibility & view mode management
        if (items.isEmpty()) {
            binding.rvVaultItems.visibility = View.GONE
            binding.layoutTableView.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            if (currentSearchQuery.isNotEmpty()) {
                binding.tvEmptyTitle.text = "No Matches Found"
                binding.tvEmptySubtitle.text = "No credentials match \"$currentSearchQuery\""
            } else {
                binding.tvEmptyTitle.text = "Vault is Empty"
                binding.tvEmptySubtitle.text = "Tap + below to store your passwords and sensitive notes safely"
            }
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            if (isTableViewActive) {
                binding.rvVaultItems.visibility = View.GONE
                binding.layoutTableView.visibility = View.VISIBLE
            } else {
                binding.rvVaultItems.visibility = View.VISIBLE
                binding.layoutTableView.visibility = View.GONE
            }
        }
    }
}
