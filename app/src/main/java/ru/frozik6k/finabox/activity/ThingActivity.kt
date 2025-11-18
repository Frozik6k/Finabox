package ru.frozik6k.finabox.activity

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button
    private lateinit var photoPager: ViewPager2
    private lateinit var photoContainer: View
    private lateinit var photoPlaceholder: View

    private var elementId: Long? = null
    private var selectedType: CatalogType = CatalogType.THING
    private var expirationDate: LocalDate = LocalDate.now().plusDays(30)
    private var parentBox: String? = null

    private val pickImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { result ->
        if (result.isNullOrEmpty()) return@registerForActivityResult
        handleSelectedPhotos(result)
    }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        val file = pendingCameraFile
        if (success && uri != null) {
            handleSelectedPhotos(listOf(uri)) {
                file?.delete()
            }
        } else {
            file?.delete()
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
        setTheme(R.style.Theme_Finabox)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_thing_editor)

        initViews()
        readExtras()
        val restoredFromState = restoreState(savedInstanceState)
        updateToolbarTitle()
        setupPhotoPager()
        setupListeners()
        if (!restoredFromState) {
            loadDataIfNeeded()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_ELEMENT_ID, elementId ?: -1L)
        outState.putString(STATE_SELECTED_TYPE, selectedType.name)
        outState.putString(STATE_PARENT_BOX, parentBox)
        outState.putString(STATE_NAME_VALUE, nameInput.text?.toString())
        outState.putString(STATE_DESCRIPTION_VALUE, descriptionInput.text?.toString())
        outState.putLong(STATE_EXPIRATION_DATE, expirationDate.toEpochDay())
        outState.putStringArrayList(STATE_PHOTO_URIS, ArrayList(photoUris))
        pendingCameraUri?.toString()?.let { outState.putString(STATE_PENDING_CAMERA_URI, it) }
        pendingCameraFile?.absolutePath?.let { outState.putString(STATE_PENDING_CAMERA_FILE, it) }
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.editorToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        nameInput = findViewById(R.id.etName)
        descriptionInput = findViewById(R.id.etDescription)
        expirationInput = findViewById(R.id.etExpiration)
        deleteButton = findViewById(R.id.btnDelete)
        saveButton = findViewById(R.id.btnSave)
        photoPager = findViewById(R.id.photoPager)
        photoContainer = findViewById(R.id.photoContainer)
        photoPlaceholder = findViewById(R.id.photoPlaceholder)

        updateExpirationField()
    }

    private fun readExtras() {
        val incomingId = intent.getLongExtra(EXTRA_ELEMENT_ID, -1L)
        elementId = incomingId.takeIf { it > 0 }
        val typeName = intent.getStringExtra(EXTRA_ELEMENT_TYPE)
        selectedType = typeName?.let { CatalogType.valueOf(it) } ?: CatalogType.THING
        parentBox = intent.getStringExtra(EXTRA_PARENT_BOX)
        toggleExpirationVisibility()
        updateDeleteButtonVisibility()
    }

    private fun restoreState(state: Bundle?): Boolean {
        state ?: return false
        elementId = state.getLong(STATE_ELEMENT_ID).takeIf { it > 0 }
        selectedType = state.getString(STATE_SELECTED_TYPE)?.let { restoredType ->
            runCatching { CatalogType.valueOf(restoredType) }.getOrDefault(selectedType)
        } ?: selectedType
        parentBox = state.getString(STATE_PARENT_BOX)
        nameInput.setText(state.getString(STATE_NAME_VALUE).orEmpty())
        descriptionInput.setText(state.getString(STATE_DESCRIPTION_VALUE).orEmpty())
        val epochDay = state.getLong(STATE_EXPIRATION_DATE, expirationDate.toEpochDay())
        expirationDate = LocalDate.ofEpochDay(epochDay)
        val restoredPhotos = state.getStringArrayList(STATE_PHOTO_URIS)
        photoUris.clear()
        if (!restoredPhotos.isNullOrEmpty()) {
            photoUris.addAll(restoredPhotos)
        }
        pendingCameraUri = state.getString(STATE_PENDING_CAMERA_URI)?.let { Uri.parse(it) }
        pendingCameraFile = state.getString(STATE_PENDING_CAMERA_FILE)?.let { File(it) }
        toggleExpirationVisibility()
        updateExpirationField()
        updateDeleteButtonVisibility()
        return true
    }

    private fun updateDeleteButtonVisibility() {
        deleteButton.visibility = if (elementId == null) View.GONE else View.VISIBLE
    }

    private fun updateToolbarTitle() {
        val actionTitle = if (elementId == null) {
            R.string.editor_title
        } else {
            R.string.editor_title_edit
        }
        val typeTitle = if (selectedType == CatalogType.THING) {
            R.string.type_thing
        } else {
            R.string.type_box
        }
        supportActionBar?.title = getString(R.string.editor_title_with_type, getString(actionTitle), getString(typeTitle))
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
        photoContainer.setOnClickListener { showPhotoPickerDialog() }

        expirationInput.setOnClickListener { showDatePicker() }
        expirationInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
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
        toggleExpirationVisibility()
        val currentId = elementId ?: run {
            deleteButton.visibility = View.GONE
            return
        }
        deleteButton.visibility = View.VISIBLE
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

    private fun handleSelectedPhotos(uris: List<Uri>, onFinished: (() -> Unit)? = null) {
        lifecycleScope.launch {
            try {
                val processedPhotos = resizeAndStorePhotos(uris)
                when {
                    processedPhotos.isEmpty() -> {
                        Toast.makeText(this@ThingActivity, R.string.photo_resize_error, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        photoUris.addAll(processedPhotos)
                        notifyPhotosChanged()
                        if (processedPhotos.size < uris.size) {
                            Toast.makeText(this@ThingActivity, R.string.photo_resize_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } finally {
                onFinished?.invoke()
            }
        }
    }

    private suspend fun resizeAndStorePhotos(uris: List<Uri>): List<String> {
        val (targetWidth, targetHeight) = getPhotoTargetSize()
        return withContext(Dispatchers.IO) {
            uris.mapNotNull { uri -> resizePhotoToFile(uri, targetWidth, targetHeight) }

        }
    }

    private fun getPhotoTargetSize(): Pair<Int, Int> {
        val targetWidth = photoContainer.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val defaultHeight = resources.getDimensionPixelSize(R.dimen.photo_container_height)
        val targetHeight = photoContainer.height.takeIf { it > 0 } ?: defaultHeight
        return targetWidth to targetHeight
    }

    private fun resizePhotoToFile(uri: Uri, targetWidth: Int, targetHeight: Int): String? {
        val bitmap = decodeBitmap(uri, targetWidth, targetHeight) ?: return null
        val scaledBitmap = resizeBitmapToBounds(bitmap, targetWidth, targetHeight)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        val outputFile = try {
            createScaledPhotoFile()
        } catch (io: IOException) {
            scaledBitmap.recycle()
            return null
        }
        return try {
            FileOutputStream(outputFile).use { stream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            Uri.fromFile(outputFile).toString()
        } catch (io: IOException) {
            outputFile.delete()
            null
        } finally {
            scaledBitmap.recycle()
        }
    }

    private fun decodeBitmap(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        // 1. Просто проверяем, что смогли открыть поток
        val boundsStream = openInputStream(uri) ?: return null
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            // не смогли прочитать размеры
            return null
        }

        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false

        // 2. Второй раз реально декодируем
        val bitmapStream = openInputStream(uri) ?: return null
        return bitmapStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        val height = options.outHeight
        val width = options.outWidth
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun resizeBitmapToBounds(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }
        val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun createScaledPhotoFile(): File {
        val storageDir = File(filesDir, "photos").apply { if (!exists()) mkdirs() }
        return File.createTempFile("thing_photo_${System.currentTimeMillis()}_", ".jpg", storageDir)
    }

    private fun openInputStream(uri: Uri): InputStream? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> uri.path?.let { path ->
                try {
                    FileInputStream(File(path))
                } catch (e : IOException) {
                    null
                }
            }
            else -> try {
                contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                null
            }
        }
    }



    companion object {
        private const val EXTRA_ELEMENT_ID = "extra_element_id"
        private const val EXTRA_ELEMENT_TYPE = "extra_element_type"
        private const val EXTRA_PARENT_BOX = "extra_parent_box"
        private const val STATE_ELEMENT_ID = "state_element_id"
        private const val STATE_SELECTED_TYPE = "state_selected_type"
        private const val STATE_PARENT_BOX = "state_parent_box"
        private const val STATE_NAME_VALUE = "state_name_value"
        private const val STATE_DESCRIPTION_VALUE = "state_description_value"
        private const val STATE_EXPIRATION_DATE = "state_expiration_date"
        private const val STATE_PHOTO_URIS = "state_photo_uris"
        private const val STATE_PENDING_CAMERA_URI = "state_pending_camera_uri"
        private const val STATE_PENDING_CAMERA_FILE = "state_pending_camera_file"

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