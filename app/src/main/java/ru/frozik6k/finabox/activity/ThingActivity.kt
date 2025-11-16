package ru.frozik6k.finabox.activity

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
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
    private var pendingCameraUri: Uri? = null
    private var pendingCameraFile: File? = null

    private lateinit var nameInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var expirationInput: TextInputEditText
    private lateinit var typeGroup: RadioGroup
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button
    private lateinit var photoPager: ViewPager2
    private lateinit var photoPlaceholder: View

    private var elementId: Long? = null
    private var selectedType: CatalogType = CatalogType.THING
    private var expirationDate: LocalDate = LocalDate.now().plusDays(30)
    private var parentBox: String? = null

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { result ->
        if (result.isNullOrEmpty()) return@registerForActivityResult
        grantPersistablePermissions(result)
        photoUris.addAll(result.map(Uri::toString))
        notifyPhotosChanged()
    }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                grantPersistablePermissions(listOf(uri))
                photoUris.add(uri.toString())
                notifyPhotosChanged()
            }
        } else {
            pendingCameraFile?.delete()
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_thing_editor)

        initViews()
        readExtras()
        updateToolbarTitle()
        setupPhotoPager()
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
        expirationInput = findViewById(R.id.etExpiration)
        typeGroup = findViewById(R.id.typeGroup)
        deleteButton = findViewById(R.id.btnDelete)
        saveButton = findViewById(R.id.btnSave)
        photoPager = findViewById(R.id.photoPager)
        photoPlaceholder = findViewById(R.id.photoPlaceholder)

        updateExpirationField()
    }

    private fun readExtras() {
        val incomingId = intent.getLongExtra(EXTRA_ELEMENT_ID, -1L)
        elementId = incomingId.takeIf { it > 0 }
        val typeName = intent.getStringExtra(EXTRA_ELEMENT_TYPE)
        selectedType = typeName?.let { CatalogType.valueOf(it) } ?: CatalogType.THING
        parentBox = intent.getStringExtra(EXTRA_PARENT_BOX)
    }

    private fun updateToolbarTitle() {
        val titleRes = if (elementId == null) {
            R.string.editor_title
        } else {
            R.string.editor_title_edit
        }
        supportActionBar?.title = getString(titleRes)
    }

    private fun setupPhotoPager() {
        photoPager.adapter = photoAdapter
        photoAdapter.onRemove = { uri ->
            photoUris.remove(uri)
            notifyPhotosChanged()
        }
        notifyPhotosChanged()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.photoContainer).setOnClickListener { showPhotoPickerDialog() }

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
        parentBoxName: String?,
        expiration: Instant,
        photos: List<String>,
    ) {
        withContext(Dispatchers.Main) {
            nameInput.setText(name)
            descriptionInput.setText(description)
            parentBox = parentBoxName
            expirationDate = LocalDate.ofInstant(expiration, ZoneId.systemDefault())
            updateExpirationField()
            photoUris.clear()
            photoUris.addAll(photos)
            notifyPhotosChanged()
            toggleExpirationVisibility()
        }
    }

    private suspend fun updateUiFromBox(
        name: String,
        description: String,
        parentBoxName: String?,
        photos: List<String>,
    ) {
        withContext(Dispatchers.Main) {
            nameInput.setText(name)
            descriptionInput.setText(description)
            parentBox = parentBoxName
            photoUris.clear()
            photoUris.addAll(photos)
            notifyPhotosChanged()
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
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when (selectedType) {
                    CatalogType.THING -> saveThing(name, description, parentBox)
                    CatalogType.BOX -> saveBox(name, description, parentBox)
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
            if (uri.scheme == "content") {
                try {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // ignore if cannot persist permission
                }
            }
        }
    }

    private fun openGalleryPicker() {
        pickImages.launch("image/*")
    }

    private fun handleCameraClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val (file, uri) = createCameraFileAndUri() ?: run {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        pendingCameraFile = file
        pendingCameraUri = uri
        capturePhoto.launch(uri)
    }

    private fun createCameraFileAndUri(): Pair<File, Uri>? {
        val storageDir = File(filesDir, "photos").apply { if (!exists()) mkdirs() }
        return try {
            val file = File.createTempFile("thing_photo_${System.currentTimeMillis()}_", ".jpg", storageDir)
            val authority = "${applicationContext.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, file)
            file to uri
        } catch (io: IOException) {
            null
        }
    }

    private fun showPhotoPickerDialog() {
        val options = arrayOf(
            getString(R.string.photo_picker_camera),
            getString(R.string.photo_picker_gallery)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.photo_picker_title)
            .setItems(options) { _, index ->
                when (index) {
                    0 -> handleCameraClick()
                    1 -> openGalleryPicker()
                }
            }
            .show()
    }

    private fun notifyPhotosChanged() {
        photoAdapter.submitList(photoUris.toList())
        updatePhotoSectionVisibility()
    }

    private fun updatePhotoSectionVisibility() {
        val hasPhotos = photoUris.isNotEmpty()
        photoPager.visibility = if (hasPhotos) View.VISIBLE else View.GONE
        photoPlaceholder.visibility = if (hasPhotos) View.GONE else View.VISIBLE
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