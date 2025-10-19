package ru.frozik6k.finabox

import android.content.DialogInterface
import android.media.tv.AdRequest
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    ThingsFragment.OnListFragmentInteractionListener,
    OnDialogListener,
    GroupButtonAddFragment.OnAddListener,
    DialogInterface.OnDismissListener {


        companion object {
            private const val ACTIVATION_ADVIEW = true
            private const val ADVIEW_DEMO = false
            private var countTransitionActivityAdd = 0 // Сколько раз окно открывалось добавление вещи
            private const val freqStartAdv = 8         // с какой частотой запускать межэкранную рекламу
            private const val LOG_TAG = "MainActivity"
        }

        private var adView: AdView? = null
        private var interstitial: InterstitialAd? = null

        private lateinit var drawer: DrawerLayout
        private lateinit var fab: FloatingActionButton
        private lateinit var mSearchItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        fab = findViewById(R.id.fab)

        ThingContent.get(applicationContext).setParent()

        fab.setOnClickListener {
            val fm = supportFragmentManager
            var fragment = fm.findFragmentById(R.id.containerDialog)
            val transaction = fm.beginTransaction()
            if (fragment == null) {
                fragment = GroupButtonAddFragment.newInstance()
                transaction.add(R.id.containerDialog, fragment)
            }
            transaction.commit()
        }

        drawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        if (ACTIVATION_ADVIEW) {
            adView = findViewById(R.id.adView)
            val adRequest: AdRequest =
                if (ADVIEW_DEMO)
                    AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .addTestDevice("0123456789ABCDEF")
                        .build()
                else AdRequest.Builder().build()
            adView?.loadAd(adRequest)

            interstitial = InterstitialAd(this)
            interstitial?.adUnitId = resources.getString(R.string.ad_unit_id_interval)
            val adRequestInterstitial: AdRequest =
                if (ADVIEW_DEMO)
                    AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .addTestDevice("0123456789ABCDEF")
                        .build()
                else AdRequest.Builder().build()
            interstitial?.loadAd(adRequestInterstitial)
        }
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            if (getDialogFragment() != null) {
                destroyDialogFragment()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ThingContent.get(this).setParent()
        restartListFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        mSearchItem = menu.findItem(R.id.action_search)

        MenuItemCompat.setOnActionExpandListener(mSearchItem,
            object : MenuItemCompat.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    val thingContent = ThingContent.get(applicationContext)
                    thingContent.setSearchText("")
                    restartListFragment()
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    val thingContent = ThingContent.get(applicationContext)
                    thingContent.setParent()
                    restartListFragment()
                    return true
                }
            })

        val searchView = mSearchItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val thingContent = ThingContent.get(applicationContext)
                if (newText.isNullOrEmpty()) {
                    thingContent.setParent()
                } else {
                    thingContent.setSearchText(newText)
                }
                restartListFragment()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_share) {
            startShareActivity()
            return true
        }

        if (id == R.id.action_rate_app) {
            startStarActivity()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @Synchronized
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            // no-op like in Java
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Logger.getLogger(MainActivity::class.java.name)
                .log(Level.WARNING, "file not selected")
        }
    }

    @Suppress("StatementWithEmptyBody")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.nav_home -> {
                val thingContent = ThingContent.get(this)
                thingContent.setParent(0)
                restartListFragment()
            }
            R.id.nav_search -> {
                mSearchItem.expandActionView()
            }
            R.id.nav_create_archive -> {
                SaveArchiveDialogFragment().show(supportFragmentManager, "saveDialog")
            }
            R.id.nav_restore_from_archive -> {
                var fragment = getDialogFragment()
                if (fragment != null) destroyDialogFragment()
                fragment = LoadArchiveDialogFragment.newInstance(1)
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.containerDialog, fragment)
                transaction.commit()
            }
            R.id.nav_share -> startShareActivity()
            R.id.nav_star -> startStarActivity()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onListFragmentInteraction(item: Thing) {
        if (item.isBox == Thing.AS_THING) {
            val thingContent = ThingContent.get(this)
            thingContent.setThingEdit(item.id)
            thingContent.setParent(item.parent)
            startActivity(ThingActivity.newIntent(this, false))
        } else {
            if (mSearchItem.isActionViewExpanded) mSearchItem.collapseActionView()
        }
    }

    override fun onDialog(name: String, isDialog: Int) {
        if (isDialog == OnDialogListener.DIALOG_SAVE) {
            val task = SaveArchiveTask()
            task.execute(name)
        }
        if (isDialog == OnDialogListener.DIALOG_LOAD) {
            val fragment = getDialogFragment()
            if (fragment != null) destroyDialogFragment()
            if (name.isNotEmpty()) {
                val task = LoadArchiveTask()
                task.execute(name)
            }
        }
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        destroyDialogFragment()
        restartListFragment()
    }

    override fun onAdd(isBox: Int) {
        if (isBox == Thing.AS_THING || isBox == Thing.AS_BOX) {
            val thingContent = ThingContent.get(this)
            Thing(thingContent.parent, isBox)
            thingContent.setThingEdit(isBox)
            startThingActivity(ThingActivity.newIntent(applicationContext, true))
        }
        destroyDialogFragment()
    }

    private fun startProgressBar() {
        val pb = findViewById<ProgressBar>(R.id.progressBar)
        val viewGroup = findViewById<ViewGroup>(R.id.containerDialog)
        pb.visibility = View.VISIBLE
        viewGroup.setBackgroundResource(R.color.colorBackgroundDialog)
    }

    private fun stopProgressBar() {
        val pb = findViewById<ProgressBar>(R.id.progressBar)
        val viewGroup = findViewById<ViewGroup>(R.id.containerDialog)
        pb.visibility = View.GONE
        viewGroup.setBackgroundResource(0)
    }

    fun displayInterstitial() {
        if (interstitial?.isLoaded == true) {
            interstitial?.show()
        }
    }

    // **************************** WORK FRAGMENTS *********************************
    private fun restartListFragment() {
        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(R.id.containerMain)

        if (fragment == null) {
            val transaction = fm.beginTransaction()
            fragment = ThingsFragment.newInstance(1)
            transaction.add(R.id.containerMain, fragment)
            transaction.commit()
        } else {
            fragment.onResume()
        }
    }

    private fun destroyDialogFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        val fragment = getDialogFragment()
        if (fragment != null) {
            transaction.remove(fragment)
        }
        transaction.commit()
    }

    private fun getDialogFragment(): Fragment? {
        val fm = supportFragmentManager
        return fm.findFragmentById(R.id.containerDialog)
    }

    // ************************** START ACTIVITY ********************************************
    private fun startShareActivity() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val textToSend = resources.getString(R.string.descriptionApplication)
        intent.putExtra(
            Intent.EXTRA_TEXT,
            textToSend + "https://play.google.com/store/apps/details?id=ru.frozik6k.lohouse"
        )
        try {
            val share = resources.getString(R.string.share)
            startActivity(Intent.createChooser(intent, share))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(applicationContext, "Share error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startStarActivity() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=ru.frozik6k.lohouse")
        startActivity(intent)
    }

    private fun startThingActivity(intent: Intent) {
        if (++countTransitionActivityAdd % freqStartAdv == 0) {
            if (ACTIVATION_ADVIEW) displayInterstitial()
        }
        startActivity(intent)
    }

    // ************************** ASYNCTASK **************************************
    private inner class SaveArchiveTask : AsyncTask<String, Void, Boolean>() {
        override fun doInBackground(vararg strings: String): Boolean {
            val thingContent = ThingContent.get(baseContext)
            if (strings[0].isEmpty()) return false
            return thingContent.saveZip(File(thingContent.backupDir, strings[0] + ".zip"))
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Toast.makeText(
                    baseContext,
                    R.string.message_true_save_archive,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    baseContext,
                    R.string.message_false_save_archive,
                    Toast.LENGTH_LONG
                ).show()
            }
            super.onPostExecute(result)
        }
    }

    private inner class LoadArchiveTask : AsyncTask<String, Void, Boolean>() {
        override fun onPreExecute() {
            startProgressBar()
            super.onPreExecute()
        }

        override fun doInBackground(vararg strings: String): Boolean {
            return ThingContent.get(applicationContext).loadZip(strings[0])
        }

        override fun onPostExecute(aBoolean: Boolean) {
            stopProgressBar()
            destroyDialogFragment()
            restartListFragment()
            if (aBoolean) {
                Toast.makeText(
                    applicationContext,
                    R.string.message_true_load_archive,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    R.string.message_false_load_archive,
                    Toast.LENGTH_LONG
                ).show()
            }
            super.onPostExecute(aBoolean)
        }
    }
}