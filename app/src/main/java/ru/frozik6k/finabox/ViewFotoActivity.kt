package ru.frozik6k.finabox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.File

class ViewFotoActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "ViewFotoActivity"
        const val FOTO_KEY = "ru.frozik6k.lohouse.foto_key"

        @JvmStatic
        fun newIntent(context: Context, fotoAbsolutPath: String): Intent {
            return Intent(context, ViewFotoActivity::class.java).apply {
                putExtra(FOTO_KEY, fotoAbsolutPath)
            }
        }
    }

    private lateinit var mIvFoto: ImageView
    private var fotoPath: String? = null
    private lateinit var mAttacher: PhotoViewAttacher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_view_foto)

        mIvFoto = findViewById(R.id.ivFoto)
        fotoPath = intent.getStringExtra(FOTO_KEY)
        Log.d(LOG_TAG, "fotoPath = $fotoPath")

        fotoPath?.let { path ->
            val file = File(path)
            if (file.isFile) {
                mIvFoto.setImageURI(Uri.fromFile(file))
            }
        }

        val anim = AnimationUtils.loadAnimation(this, R.anim.alpha_foto).apply {
            duration = 500
        }
        mIvFoto.startAnimation(anim)

        mAttacher = PhotoViewAttacher(mIvFoto)
    }
}






}