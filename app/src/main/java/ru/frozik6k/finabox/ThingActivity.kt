package ru.frozik6k.finabox

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import ru.frozik6k.lohouse.thing.Thing
import ru.frozik6k.lohouse.thing.ThingContent

class ThingActivity : AppCompatActivity(),
    FotosFragment.OnFotoListener,
    ThingFragment.OnEditListener {

    companion object {
        private const val LOG_TAG = "ThingActivity"
        const val EXTRA_IS_EDIT = "ru.frozik6k.lohouse.isedit"

        @JvmStatic
        fun newIntent(packageContext: Context, isEdit: Boolean): Intent {
            return Intent(packageContext, ThingActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
            }
        }
    }

    private var isEdit: Boolean = false
    private lateinit var mThing: Thing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thing)

        // кнопка «назад»
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        Log.d(LOG_TAG, "onResume")
        super.onResume()

        mThing = ThingContent.get(applicationContext).thingEdit
        isEdit = intent.getBooleanExtra(EXTRA_IS_EDIT, false)

        // Заголовок (тип: вещь или хранилище)
        if (mThing.isBox == Thing.AS_BOX) {
            supportActionBar?.setTitle(R.string.box)
        } else {
            supportActionBar?.setTitle(R.string.thing)
        }

        restartThingFragment()
        Log.d(LOG_TAG, "Size foto = ${mThing.mFotos.size}")
    }

    override fun onPause() {
        if (mThing.thingName.trim().isNotEmpty()) {
            ThingContent.get(applicationContext).addThing(mThing)
        }
        super.onPause()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isEdit = savedInstanceState.getBoolean(EXTRA_IS_EDIT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(EXTRA_IS_EDIT, isEdit)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    // FotosFragment.OnFotoListener
    override fun onClickFoto(fotoAbsolutPath: String, position: Int) {
        val thingContent = ThingContent.get(this)
        val thing = thingContent.thingEdit
        thing.mFotos.add(fotoAbsolutPath)
    }

    // ThingFragment.OnEditListener
    override fun onEdit() {
        isEdit = true
        restartThingFragment()
    }

    private fun restartThingFragment() {
        val fm: FragmentManager = supportFragmentManager
        val current: Fragment? = fm.findFragmentById(R.id.containerThing)

        if (current == null) {
            val transaction: FragmentTransaction = fm.beginTransaction()
            val fragment: Fragment = ThingFragment.newInstance(isEdit)
            transaction.add(R.id.containerThing, fragment)
            transaction.commit()
        } else {
            val transaction: FragmentTransaction = fm.beginTransaction()
            transaction.remove(current)
            val fragment: Fragment = ThingFragment.newInstance(isEdit)
            transaction.add(R.id.containerThing, fragment)
            transaction.commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(LOG_TAG, "onActivityResult")
    }
}






