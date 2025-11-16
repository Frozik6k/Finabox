package ru.frozik6k.finabox.presentation.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.frozik6k.finabox.R
import ru.frozik6k.finabox.activity.ThingActivity
import ru.frozik6k.finabox.adapter.CatalogAdapter
import ru.frozik6k.finabox.dto.CatalogDto
import ru.frozik6k.finabox.dto.CatalogType

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ThingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val thingsRecycler: RecyclerView = findViewById(R.id.recycler)
        val thingsAdapter = CatalogAdapter()

        thingsRecycler.layoutManager = LinearLayoutManager(this)
        thingsRecycler.adapter = thingsAdapter

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (!viewModel.navigateUp()) {
                finish()
            }
        }

        findViewById<View>(R.id.fabAdd).setOnClickListener {
            openEditorForCreation()
        }

        thingsAdapter.onItemClick = { entry -> handleItemClick(entry) }
        thingsAdapter.onItemLongClick = { entry -> openEditorForEditing(entry) }

        observeThings(thingsAdapter)
        observeToolbar(toolbar)

    }

    override fun onBackPressed() {
        if (!viewModel.navigateUp()) {
            super.onBackPressed()
        }
    }

    private fun openEditorForCreation() {
        val intent = ThingActivity.createIntent(
            context = this,
            type = null,
            elementId = null,
            parentBox = viewModel.currentCatalogName
        )
        startActivity(intent)
    }

    private fun handleItemClick(entry: CatalogDto) {
        when (entry.type) {
            CatalogType.BOX -> viewModel.enterCatalog(entry)
            CatalogType.THING -> openEditorForEditing(entry)
        }
    }

    private fun openEditorForEditing(entry: CatalogDto) {
        val intent = ThingActivity.createIntent(
            context = this,
            type = entry.type,
            elementId = entry.id,
            parentBox = entry.parentBox
        )
        startActivity(intent)
    }

    private fun observeToolbar(toolbar: Toolbar) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.path.collect { path ->
                    val title = if (path.size == 1 && path.firstOrNull() == null) {
                        getString(R.string.root_catalog)
                    } else {
                        path.joinToString(separator = " / ") { it ?: getString(R.string.root_catalog) }
                    }
                    toolbar.title = title
                }
            }
        }
    }

    private fun observeThings(adapter: CatalogAdapter) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { things ->
                    adapter.data = things
                }
            }
        }
    }
}