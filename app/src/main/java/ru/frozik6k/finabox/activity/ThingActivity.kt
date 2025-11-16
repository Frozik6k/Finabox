package ru.frozik6k.finabox.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.frozik6k.finabox.R
import ru.frozik6k.finabox.adapter.PhotoPreviewAdapter
import ru.frozik6k.finabox.data.entities.BoxDb
import ru.frozik6k.finabox.data.entities.ThingDb
import ru.frozik6k.finabox.data.storage.dao.BoxDao
import ru.frozik6k.finabox.data.storage.dao.ThingDao
import ru.frozik6k.finabox.dto.CatalogType

@AndroidEntryPoint
class ThingActivity : AppCompatActivity() {

    @Inject
    lateinit var thingDao: ThingDao

    @Inject
    lateinit var boxDao: BoxDao

    private val photoAdapter = PhotoPreviewAdapter()
    private val photoUris = mutableListOf<String>()

    private lateinit var nameInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var parentInput: TextInputEditText
    private lateinit var expirationInput: TextInputEditText
    private lateinit var typeGroup: RadioGroup
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button

    private var elementId: Long? = null
    private var selectedType: CatalogType = CatalogType.THING
    private var expirationDate: LocalDate = LocalDate.now().plusDays(30)

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { result ->
        if (result.isNullOrEmpty()) return@registerForActivityResult
        grantPersistablePermissions(result)
        photoUris.addAll(result.map(Uri::toString))
        photoAdapter.submitList(photoUris.toList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_thing_editor)

        initViews()
        readExtras()
        updateToolbarTitle()
        setupRecycler()
        setupListeners()
        loadDataIfNeeded()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.editorToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        nameInput = findViewById(R.id.etName)
        descriptionInput = findViewById(R.id.etDescription)
        parentInput = findViewById(R.id.etParent)
        expirationInput = findViewById(R.id.etExpiration)
        typeGroup = findViewById(R.id.typeGroup)
        deleteButton = findViewById(R.id.btnDelete)
        saveButton = findViewById(R.id.btnSave)

        updateExpirationField()
    }

    private fun readExtras() {
        val incomingId = intent.getLongExtra(EXTRA_ELEMENT_ID, -1L)
        elementId = incomingId.takeIf { it > 0 }
        val typeName = intent.getStringExtra(EXTRA_ELEMENT_TYPE)
        selectedType = typeName?.let { CatalogType.valueOf(it) } ?: CatalogType.THING
        parentInput.setText(intent.getStringExtra(EXTRA_PARENT_BOX) ?: "")
    }

    private fun updateToolbarTitle() {
        val titleRes = if (elementId == null) {
            R.string.editor_title
        } else {
            R.string.editor_title_edit
        }
        supportActionBar?.title = getString(titleRes)
    }

    private fun setupRecycler() {
        val recycler = findViewById<RecyclerView>(R.id.rvPhotos)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recycler.adapter = photoAdapter
        photoAdapter.onRemove = { uri ->
            photoUris.remove(uri)
            photoAdapter.submitList(photoUris.toList())
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnAddPhoto).setOnClickListener {
            pickImages.launch("image/*")
        }

        expirationInput.setOnClickListener { showDatePicker() }
        expirationInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
        }

        typeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedType = if (checkedId == R.id.rbThing) {
                CatalogType.THING
            } else {
                CatalogType.BOX
            }
            toggleExpirationVisibility()
        }

        saveButton.setOnClickListener { saveElement() }
        deleteButton.setOnClickListener { deleteElement() }
    }

    private fun toggleExpirationVisibility() {
        val expirationContainer = findViewById<View>(R.id.expirationContainer)
        expirationContainer.visibility = if (selectedType == CatalogType.THING) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateExpirationField() {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        expirationInput.setText(formatter.format(expirationDate))
    }

    private fun showDatePicker() {
        val current = expirationDate
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                expirationDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateExpirationField()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }

    private fun loadDataIfNeeded() {
        val currentId = elementId ?: run {
            syncTypeGroup()
            deleteButton.visibility = View.GONE
            return
        }
        deleteButton.visibility = View.VISIBLE
        syncTypeGroup()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when (selectedType) {
                    CatalogType.THING -> {
                        val thing = thingDao.getThingWithFotos(currentId)
                        updateUiFromThing(
                            thing.thing.name,
                            thing.thing.description,
                            thing.thing.box,
                            thing.thing.expirationDate,
                            thing.fotos.map { it.path }
                        )
                    }
                    CatalogType.BOX -> {
                        val box = boxDao.getBoxWithFotos(currentId)
                        updateUiFromBox(
                            box.box.name,
                            box.box.description,
                            box.box.box,
                            box.fotos.map { it.path }
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateUiFromThing(
        name: String,
        description: String,
        parentBox: String?,
        expiration: Instant,
        photos: List<String>,
    ) {
        withContext(Dispatchers.Main) {
            nameInput.setText(name)
            descriptionInput.setText(description)
            parentInput.setText(parentBox ?: "")
            expirationDate = LocalDate.ofInstant(expiration, ZoneId.systemDefault())
            updateExpirationField()
            photoUris.clear()
            photoUris.addAll(photos)
            photoAdapter.submitList(photoUris.toList())
            toggleExpirationVisibility()
        }
    }

    private suspend fun updateUiFromBox(
        name: String,
        description: String,
        parentBox: String?,
        photos: List<String>,
    ) {
        withContext(Dispatchers.Main) {
            nameInput.setText(name)
            descriptionInput.setText(description)
            parentInput.setText(parentBox ?: "")
            photoUris.clear()
            photoUris.addAll(photos)
            photoAdapter.submitList(photoUris.toList())
            toggleExpirationVisibility()
        }
    }

    private fun syncTypeGroup() {
        typeGroup.check(
            if (selectedType == CatalogType.THING) R.id.rbThing else R.id.rbBox
        )
        if (elementId != null) {
            typeGroup.isEnabled = false
            for (i in 0 until typeGroup.childCount) {
                typeGroup.getChildAt(i).isEnabled = false
            }
        }
        toggleExpirationVisibility()
    }

    private fun saveElement() {
        val name = nameInput.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_SHORT).show()
            return
        }
        val description = descriptionInput.text?.toString()?.trim().orEmpty()
        val parent = parentInput.text?.toString()?.trim().takeIf { it?.isNotBlank() == true }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when (selectedType) {
                    CatalogType.THING -> saveThing(name, description, parent)
                    CatalogType.BOX -> saveBox(name, description, parent)
                }
            }
            finish()
        }
    }

    private suspend fun saveThing(name: String, description: String, parent: String?) {
        val instant = expirationDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        if (elementId == null) {
            thingDao.createThingWithFotos(
                ThingDb(
                    name = name,
                    description = description,
                    box = parent,
                    expirationDate = instant
                ),
                photoUris
            )
        } else {
            thingDao.updateThingWithFotos(
                ThingDb(
                    id = elementId!!,
                    name = name,
                    description = description,
                    box = parent,
                    expirationDate = instant
                ),
                photoUris
            )
        }
    }

    private suspend fun saveBox(name: String, description: String, parent: String?) {
        if (elementId == null) {
            boxDao.createBoxWithFotos(
                BoxDb(
                    name = name,
                    description = description,
                    box = parent,
                ),
                photoUris
            )
        } else {
            boxDao.updateBoxWithFotos(
                BoxDb(
                    id = elementId!!,
                    name = name,
                    description = description,
                    box = parent
                ),
                photoUris
            )
        }
    }

    private fun deleteElement() {
        val currentId = elementId ?: return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when (selectedType) {
                    CatalogType.THING -> {
                        val thing = thingDao.getThingWithFotos(currentId)
                        thingDao.deleteThing(thing.thing)
                    }
                    CatalogType.BOX -> {
                        val box = boxDao.getBoxWithFotos(currentId)
                        deleteBoxRecursively(box.box)
                    }
                }
            }
            finish()
        }
    }

    private suspend fun deleteBoxRecursively(box: BoxDb) {
        val children = boxDao.getBoxesByParent(box.name)
        children.forEach { child -> deleteBoxRecursively(child) }
        val things = thingDao.getThingsByParent(box.name)
        things.forEach { thing -> thingDao.deleteThing(thing) }
        boxDao.deleteBox(box)
    }

    private fun grantPersistablePermissions(uris: List<Uri>) {
        val resolver = contentResolver
        uris.forEach { uri ->
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // ignore if cannot persist permission
            }
        }
    }

    companion object {
        private const val EXTRA_ELEMENT_ID = "extra_element_id"
        private const val EXTRA_ELEMENT_TYPE = "extra_element_type"
        private const val EXTRA_PARENT_BOX = "extra_parent_box"

        fun createIntent(
            context: Context,
            type: CatalogType?,
            elementId: Long?,
            parentBox: String?,
        ): Intent {
            return Intent(context, ThingActivity::class.java).apply {
                if (elementId != null) {
                    putExtra(EXTRA_ELEMENT_ID, elementId)
                }
                if (type != null) {
                    putExtra(EXTRA_ELEMENT_TYPE, type.name)
                }
                putExtra(EXTRA_PARENT_BOX, parentBox)
            }
        }
    }
}