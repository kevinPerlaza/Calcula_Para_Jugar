package com.example.calculaparajugar.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculaparajugar.R
import com.example.calculaparajugar.databinding.ActivityAppSelectorBinding
import com.example.calculaparajugar.util.AppConfig

/**
 * Pantalla donde el padre selecciona qué apps se van a monitorear
 * con preguntas matemáticas.
 */
class AppSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectorBinding
    private lateinit var adapter: AppListAdapter
    private var installedApps = listOf<AppInfo>()
    private var currentQuery = ""
    private var visibleAppCount = 0

    data class AppInfo(
        val packageName: String,
        val name: String,
        val icon: android.graphics.drawable.Drawable,
        var isBlocked: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // adjustResize ya descuenta el teclado; aquí solo aplicamos barras del sistema.
        binding.root.applySystemBarInsets()

        binding.btnBackApps.setOnClickListener { finish() }

        loadApps()
        setupRecyclerView()
        setupSearch()
        setupBulkControls()
        setupMicroInteractions()

        binding.btnSave.setOnClickListener {
            saveSelection()
        }
    }

    private fun setupMicroInteractions() {
        listOf(
            binding.btnSelectAll,
            binding.btnClearAll,
            binding.btnBackApps,
            binding.btnSave
        ).forEach { it.applyPressFeedback() }
    }

    private fun loadApps() {
        val pm = packageManager
        val blockedApps = AppConfig.getBlockedApps(this)

        // Obtener solo apps con launcher (que el usuario puede abrir)
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)

        installedApps = resolveInfos
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                AppInfo(
                    packageName = appInfo.packageName,
                    name = appInfo.loadLabel(pm).toString(),
                    icon = appInfo.loadIcon(pm),
                    isBlocked = appInfo.packageName in blockedApps
                )
            }
            .filter { it.packageName != packageName } // Excluir nuestra app
            .sortedWith(compareByDescending<AppInfo> { it.isBlocked }.thenBy { it.name })

        visibleAppCount = installedApps.size
        binding.switchMonitorAll.isChecked = AppConfig.isMonitorAllAppsEnabled(this)
        updateCount()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(installedApps) { app, isChecked ->
            app.isBlocked = isChecked
            updateCount()
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchApps.doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty().trim()
            applySearchFilter()
        }
        binding.etSearchApps.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                view.clearFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun applySearchFilter() {
        val query = currentQuery.lowercase()
        val visibleApps = if (query.isBlank()) {
            installedApps
        } else {
            installedApps.filter { app ->
                app.name.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query)
            }
        }
        visibleAppCount = visibleApps.size
        adapter.submitApps(visibleApps)
        binding.tvNoApps.visibility = if (visibleApps.isEmpty()) View.VISIBLE else View.GONE
        binding.rvApps.visibility = if (visibleApps.isEmpty()) View.GONE else View.VISIBLE
        updateCount(visibleApps.size)
    }

    private fun updateCount(visibleCount: Int = visibleAppCount) {
        val blockedCount = installedApps.count { it.isBlocked }
        val availableText = if (currentQuery.isBlank()) {
            "${installedApps.size} apps disponibles"
        } else {
            "$visibleCount de ${installedApps.size} apps"
        }
        binding.tvAppCount.text = if (binding.switchMonitorAll.isChecked) {
            "$availableText · todas monitoreadas"
        } else {
            "$availableText · $blockedCount monitoreadas"
        }
    }

    private fun setupBulkControls() {
        binding.switchMonitorAll.setOnCheckedChangeListener { _, _ ->
            updateCount()
        }

        binding.btnSelectAll.setOnClickListener {
            installedApps.forEach { it.isBlocked = true }
            adapter.notifyDataSetChanged()
            updateCount()
        }

        binding.btnClearAll.setOnClickListener {
            installedApps.forEach { it.isBlocked = false }
            adapter.notifyDataSetChanged()
            updateCount()
        }
    }

    private fun saveSelection() {
        val blockedPackages = installedApps.filter { it.isBlocked }.map { it.packageName }.toSet()
        AppConfig.setBlockedApps(this, blockedPackages)
        AppConfig.setMonitorAllAppsEnabled(this, binding.switchMonitorAll.isChecked)
        setResult(RESULT_OK)
        finish()
    }

    /**
     * Adapter para la lista de apps.
     */
    class AppListAdapter(
        apps: List<AppInfo>,
        private val onCheckedChange: (AppInfo, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

        private var apps: List<AppInfo> = apps

        fun submitApps(filteredApps: List<AppInfo>) {
            apps = filteredApps
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.imgAppIcon)
            val name: TextView = view.findViewById(R.id.tvAppName)
            val packageName: TextView = view.findViewById(R.id.tvPackageName)
            val checkbox: com.google.android.material.switchmaterial.SwitchMaterial = view.findViewById(R.id.cbBlocked)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name
            holder.packageName.text = app.packageName

            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = app.isBlocked
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                holder.itemView.isSelected = isChecked
                onCheckedChange(app, isChecked)
            }
            holder.itemView.isSelected = app.isBlocked
            holder.itemView.applyPressFeedback(0.985f)

            holder.itemView.setOnClickListener {
                holder.checkbox.isChecked = !holder.checkbox.isChecked
            }
        }

        override fun getItemCount() = apps.size
    }
}
