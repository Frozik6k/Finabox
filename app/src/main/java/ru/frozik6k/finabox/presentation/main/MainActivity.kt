package ru.frozik6k.finabox.presentation.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
    private var isFabMenuVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val thingsRecycler: RecyclerView = findViewById(R.id.recycler)
        val thingsAdapter = CatalogAdapter()

        thingsRecycler.layoutManager = LinearLayoutManager(this)
        thingsRecycler.adapter = thingsAdapter


        val root = findViewById<View>(R.id.root)
        val appBar = findViewById<View>(R.id.appBar)
        val fabMenu = findViewById<View>(R.id.fabMenu)
        val initialRecyclerPaddingBottom = thingsRecycler.paddingBottom
        val initialRecyclerPaddingTop = thingsRecycler.paddingTop
        val initialFabPaddingBottom = fabMenu.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBar.updatePadding(top = systemBars.top)
            thingsRecycler.updatePadding(
                top = initialRecyclerPaddingTop,
                bottom = initialRecyclerPaddingBottom + systemBars.bottom,
            )
            fabMenu.updatePadding(bottom = initialFabPaddingBottom + systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (!viewModel.navigateUp()) {
                finish()
            }
        }

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val fabAddThing = findViewById<ExtendedFloatingActionButton>(R.id.fabAddThing)
        val fabAddCatalog = findViewById<ExtendedFloatingActionButton>(R.id.fabAddCatalog)

        fabAdd.setOnClickListener {
            toggleFabMenu(fabAddThing, fabAddCatalog, fabAdd)
        }

        fabAddThing.setOnClickListener {
            openEditorForCreation(CatalogType.THING)
            hideFabMenu(fabAddThing, fabAddCatalog, fabAdd)
        }

        fabAddCatalog.setOnClickListener {
            openEditorForCreation(CatalogType.BOX)
            hideFabMenu(fabAddThing, fabAddCatalog, fabAdd)
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

    private fun openEditorForCreation(type: CatalogType) {
        val intent = ThingActivity.createIntent(
            context = this,
            type = type,
            elementId = null,
            parentBox = viewModel.currentCatalogName
        )
        startActivity(intent)
    }

    private fun toggleFabMenu(
        thingButton: ExtendedFloatingActionButton,
        catalogButton: ExtendedFloatingActionButton,
        mainFab: FloatingActionButton,
    ) {
        isFabMenuVisible = !isFabMenuVisible
        val visibility = if (isFabMenuVisible) View.VISIBLE else View.GONE
        thingButton.visibility = visibility
        catalogButton.visibility = visibility
        mainFab.animate().rotation(if (isFabMenuVisible) 45f else 0f).start()
    }

    private fun hideFabMenu(
        thingButton: ExtendedFloatingActionButton,
        catalogButton: ExtendedFloatingActionButton,
        mainFab: FloatingActionButton,
    ) {
        if (!isFabMenuVisible) return
        isFabMenuVisible = false
        thingButton.visibility = View.GONE
        catalogButton.visibility = View.GONE
        mainFab.animate().rotation(0f).start()
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