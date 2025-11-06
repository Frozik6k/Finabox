package ru.frozik6k.finabox.presentation.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.frozik6k.finabox.R
import ru.frozik6k.finabox.adapter.ThingAdapter
import ru.frozik6k.finabox.dto.ThingGenerator

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val thingsRecycler: RecyclerView = findViewById(R.id.recycler)
        val thingsAdapter = ThingAdapter()

        thingsRecycler.layoutManager = LinearLayoutManager(this)
        thingsRecycler.adapter = thingsAdapter

        thingsAdapter.data = ThingGenerator.generateThings(15)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

    }
}