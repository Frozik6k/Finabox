package ru.frozik6k.finabox.presentation.main

import android.os.Bundle
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
import ru.frozik6k.finabox.adapter.ThingAdapter

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ThingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val thingsRecycler: RecyclerView = findViewById(R.id.recycler)
        val thingsAdapter = ThingAdapter()

        thingsRecycler.layoutManager = LinearLayoutManager(this)
        thingsRecycler.adapter = thingsAdapter

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        observeThings(thingsAdapter)

    }

    private fun observeThings(adapter: ThingAdapter) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.things.collect { things ->
                    adapter.data = things
                }
            }
        }
    }
}