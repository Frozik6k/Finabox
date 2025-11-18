package ru.frozik6k.finabox.presentation.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.frozik6k.finabox.R
import ru.frozik6k.finabox.activity.ThingActivity
import ru.frozik6k.finabox.adapter.CatalogAdapter
import ru.frozik6k.finabox.databinding.ActivityMainBinding
import ru.frozik6k.finabox.dto.CatalogDto
import ru.frozik6k.finabox.dto.CatalogType
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ThingsViewModel by viewModels()
    private var isFabMenuVisible: Boolean = false
    private var bannerAd: BannerAdView? = null
    private lateinit var binding: ActivityMainBinding

    // вычисляем размер баннера под ширину экрана/контейнера
    private val adSize: BannerAdSize
        get() {
            var adWidthPixels = binding.adContainerView.width
            if (adWidthPixels == 0) {
                // если ещё не разложился layout — берём ширину экрана
                adWidthPixels = resources.displayMetrics.widthPixels
            }
            val density = resources.displayMetrics.density
            val adWidth = (adWidthPixels / density).roundToInt()
            return BannerAdSize.stickySize(this, adWidth)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Finabox)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        enableEdgeToEdge()

        // ждём, пока контейнер отрисуется, чтобы знать его ширину
        binding.adContainerView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.adContainerView.viewTreeObserver
                        .removeOnGlobalLayoutListener(this)
                    bannerAd = loadBannerAd(adSize)
                }
            }
        )

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as? SearchView

        searchView?.apply {
            queryHint = getString(R.string.nav_search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.updateSearchQuery(query.orEmpty())
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.updateSearchQuery(newText.orEmpty())
                    return true
                }
            })

            val currentQuery = viewModel.searchQuery.value
            if (currentQuery.isNotEmpty()) {
                searchItem.expandActionView()
                setQuery(currentQuery, false)
                clearFocus()
            }
        }

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.updateSearchQuery("")
                return true
            }

        })
        return true
    }


    private fun loadBannerAd(adSize: BannerAdSize): BannerAdView =
        binding.banner.apply {
            // размер и ID блока
            setAdSize(adSize)
            setAdUnitId(getString(R.string.yandex_token)) // тут твой реальный ID из кабинета

            // слушатель событий (по желанию можно упростить)
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    if (isDestroyed) {
                        bannerAd?.destroy()
                        return
                    }
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    // логируем ошибку; не спамим перезагрузками
                }

                override fun onAdClicked() {}
                override fun onLeftApplication() {}
                override fun onReturnedToApplication() {}
                override fun onImpression(impressionData: ImpressionData?) {}
            })

            // запрос рекламы
            loadAd(
                AdRequest.Builder()
                    // можно добавить таргетинг-параметры
                    .build()
            )
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