package ru.frozik6k.finabox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import ru.frozik6k.lohouse.thing.Thing
import ru.frozik6k.lohouse.thing.ThingContent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*

class FotosFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "FotosFragment"

        private const val TAKE_PICTURE = 1

        private const val FOTO_ABSOLUT_PATH = "foto_absolut_path"
        private const val FOTO_TYPE = "foto_type"
        private const val FOTO_NUMBER = "foto_number"
        private const val FOTO_TAG = "foto_tag"

        // типы файлов
        const val FOTO_FILE = 0
        const val FOTO_ADD = 1
        const val FOTO_NO = 2

        // static, чтобы не терялась при повороте
        private var mFotoTempUri: Uri? = null

        fun newInstance(
            fotoAbsolutPath: String,
            fotoType: Int,
            fotoNumber: Int,
            fotoTag: String
        ): FotosFragment {
            val fragment = FotosFragment()
            val args = Bundle().apply {
                putString(FOTO_ABSOLUT_PATH, fotoAbsolutPath)
                putInt(FOTO_TYPE, fotoType)
                putInt(FOTO_NUMBER, fotoNumber)
                putString(FOTO_TAG, fotoTag)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var mFotoAbsolutPath: String? = null
    private var mFotoType = 0
    private var mFotoNumber = 0
    private var mTag: String? = null
    private var mFotoAddActivity = false // костыль из исходника (не используется сейчас)

    private var ivFoto: ImageView? = null
    private var pbLoadFoto: ProgressBar? = null
    private lateinit var mView: View
    private var mViewGroup: ViewGroup? = null
    private lateinit var mInflater: LayoutInflater

    private var mListener: OnFotoListener? = null
    private lateinit var mContext: Context

    private var mLoadFotoTask: LoadFotoTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate $this")

        arguments?.let { args ->
            mFotoAbsolutPath = args.getString(FOTO_ABSOLUT_PATH)
            mFotoType = args.getInt(FOTO_TYPE)
            mFotoNumber = args.getInt(FOTO_NUMBER)
            mTag = args.getString(FOTO_TAG)
        }
    }

    private fun getBitmapSmall(file: File): Bitmap? {
        val metrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
        if (file.exists()) {
            val bmOriginal = BitmapFactory.decodeFile(file.absolutePath)
            val width = bmOriginal.width
            val height = bmOriginal.height

            val ratioWidth = width.toFloat() / metrics.widthPixels
            val ratioHeight = height.toFloat() / metrics.heightPixels
            val ratio = if (ratioHeight < ratioWidth) ratioHeight else ratioWidth

            return if (ratio > 1.5f) {
                val halfWidth = (width / ratio).toInt()
                val halfHeight = (height / ratio).toInt()
                val bmHalf = Bitmap.createScaledBitmap(bmOriginal, halfWidth, halfHeight, false)
                savePicture(bmHalf, file)
                bmOriginal.recycle()
                bmHalf
            } else {
                bmOriginal
            }
        }
        return null
    }

    private fun savePicture(bitmap: Bitmap, file: File): String {
        var fOut: OutputStream? = null
        val nameFile = file.name
        val tempNameFile = "~$nameFile"
        val tempFile = File(file.parent, tempNameFile)
        if (!file.renameTo(tempFile)) return "Ошибка: не удалось переименовать файл"
        Log.d(LOG_TAG, "file.getAbsolutPath = ${file.absolutePath}")
        return try {
            fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut)
            fOut.flush()
            fOut.close()
            Log.d(LOG_TAG, "file = ${file.absolutePath}")
            tempFile.delete()
            ""
        } catch (e: Exception) {
            tempFile.renameTo(file)
            e.message ?: ""
        }
    }

    private val mOnClickListener = View.OnClickListener {
        when (mFotoType) {
            FOTO_ADD -> {
                mListener?.let {
                    val thingContent = ThingContent.get(mContext)
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    var file = thingContent.fotoDir
                    if (!file.exists()) file.mkdirs()
                    file = File(
                        file.absolutePath,
                        "IMG_" + Thing.dateToString(Calendar.getInstance().time, Thing.DATE_FORMAT_FILE) + ".jpg"
                    )
                    mFotoTempUri = Uri.fromFile(file)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFotoTempUri)
                    startActivityForResult(cameraIntent, TAKE_PICTURE)
                }
            }

            FOTO_FILE -> {
                startActivity(ViewFotoActivity.newIntent(mContext, mFotoAbsolutPath))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(LOG_TAG, "onCreateView $this")
        mViewGroup = container
        mInflater = inflater
        mView = updateUI()
        mTag?.let { mView.tag = it }
        return mView
    }

    private fun updateUI(): View {
        val view = when (mFotoType) {
            FOTO_FILE -> {
                val v = mInflater.inflate(R.layout.fragment_fotos, null)
                ivFoto = v.findViewById(R.id.ivFoto)
                pbLoadFoto = v.findViewById(R.id.pbLoadFoto)
                mLoadFotoTask = LoadFotoTask().also { it.execute(mFotoAbsolutPath) }
                v
            }

            FOTO_ADD, FOTO_NO -> mInflater.inflate(R.layout.fragment_fotos_add, null)
            else -> mInflater.inflate(R.layout.fragment_fotos_add, null)
        }
        Log.d(LOG_TAG, "updateUI $this")
        Log.d(LOG_TAG, "mFotoType = $mFotoType")
        ivFoto = view.findViewById(R.id.ivFoto)
        view.setOnClickListener(mOnClickListener)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(LOG_TAG, "onAttach $this")
        mContext = context
        if (context is OnFotoListener) mListener = context
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(LOG_TAG, "onDetach $this")
        mListener = null
        mLoadFotoTask?.let {
            it.cancel(false)
            Log.d(LOG_TAG, "Отмена потока")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(LOG_TAG, "onActivityResult $this")
        if (requestCode == TAKE_PICTURE && resultCode == Activity.RESULT_OK) {
            mFotoType = FOTO_FILE
            val path = mFotoTempUri?.encodedPath
            if (path != null) {
                mFotoAbsolutPath = path
                val fileFoto = File(path)
                val dcim = "DCIM"
                try {
                    val sh = StorageHelper()
                    val listDev = sh.allMountedDevices
                    for (mountDevice in listDev) {
                        val file = findFile(fileFoto.lastModified(), File(mountDevice.path, dcim))
                        if (file != null) {
                            file.delete()
                            break
                        }
                    }
                } catch (_: Exception) {
                }
                mListener?.onClickFoto(mFotoAbsolutPath!!, mFotoNumber)
            }
        } else {
            mFotoTempUri = null
        }
    }

    private fun findFile(lastModified: Long, dir: File): File? {
        val listFile = dir.listFiles() ?: return null
        for (file in listFile) {
            Log.d(LOG_TAG, "findFile = ${file.lastModified()}")
            if (file.isDirectory) {
                val result = findFile(lastModified, file)
                if (result != null) return result
            } else {
                if (lastModified == file.lastModified()) {
                    return file
                }
            }
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "onResume $this")
        mFotoAbsolutPath?.let { Log.d(LOG_TAG, it) }
    }

    interface OnFotoListener {
        fun onClickFoto(fotoAbsolutPath: String, position: Int)
    }

    private inner class LoadFotoTask : AsyncTask<String, Void, Void>() {
        private var mFotoBitmap: Bitmap? = null

        override fun onPreExecute() {
            pbLoadFoto?.visibility = View.VISIBLE
            super.onPreExecute()
        }

        override fun onPostExecute(result: Void?) {
            try {
                pbLoadFoto?.visibility = View.GONE
                val anim = AnimationUtils.loadAnimation(mContext, R.anim.alpha_foto)
                ivFoto?.startAnimation(anim)
                ivFoto?.setImageBitmap(mFotoBitmap)
            } catch (e: Resources.NotFoundException) {
                e.printStackTrace()
            }
            super.onPostExecute(result)
        }

        override fun doInBackground(vararg params: String): Void? {
            val path = params.firstOrNull() ?: return null
            val fileFoto = File(path)
            if (fileFoto.exists()) {
                mFotoBitmap = getBitmapSmall(fileFoto)
            }
            return null
        }
    }
}