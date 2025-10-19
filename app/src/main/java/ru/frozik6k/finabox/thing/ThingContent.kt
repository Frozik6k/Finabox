package ru.frozik6k.finabox.thing

import android.content.Context
import android.os.Environment
import android.util.Log
import ru.frozik6k.finabox.DB
import java.io.File
import java.io.FilenameFilter
import java.util.ArrayList
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
class ThingContent private constructor(private val context: Context) {

    private val LOG_TAG = "ThingContent"

    private val things: MutableList<Thing> = ArrayList()
    private val thingsMap: MutableMap<Long, Thing> = HashMap()

    private val db: DB = DB(context).apply { open() }

    private var parent: Long = 0L

    private var thingEdit: Thing? = null

    private var actionSearch: Boolean = false

    private val MAIN_DIR = "Finabox"
    private val BACKUP_DIR: String =
        Environment.getExternalStoragePublicDirectory(MAIN_DIR).absolutePath

    fun getThings(): List<Thing> = things

    fun getThing(id: Long): Thing? {
        for (thing in things) {
            if (thing.id == id) return thing
        }
        return null
    }

    private fun addItem(item: Thing) {
        if (thingsMap[item.id] == null) {
            things.add(item)
            thingsMap[item.id] = item
        } else {
            for (i in things.indices) {
                if (item.id == things[i].id) {
                    things[i] = item
                }
            }
            thingsMap[item.id] = item
        }
    }

    private fun delItem(item: Thing) {
        if (thingsMap[item.id] != null) {
            for (i in things.indices) {
                if (item.id == things[i].id) {
                    things.removeAt(i)
                    break
                }
            }
            thingsMap.remove(item.id)
        }
    }

    private fun delItem(id: Long) {
        if (thingsMap[id] != null) {
            for (i in things.indices) {
                if (id == things[i].id) {
                    things.removeAt(i)
                    break
                }
            }
            thingsMap.remove(id)
        }
    }

    private fun clear() {
        things.clear()
        thingsMap.clear()
    }

    // parantParent - установить родителя parent иначе установить parent
    fun setParent(parent: Long, parentParent: Boolean) {
        this.parent = if (parentParent) db.getRec(parent).parent else parent
        clear()
        val list = db.getParentData(parent)
        for (thing in list) addItem(thing)
        actionSearch = false
    }

    fun setParent(parentParent: Boolean) {
        if (parentParent) {
            this.parent = db.getRec(this.parent).parent
        }
        clear()
        val list = db.getParentData(this.parent)
        for (thing in list) addItem(thing)
        actionSearch = false
    }

    fun setParent(parent: Long) {
        this.parent = parent
        clear()
        val list = db.getParentData(parent)
        for (thing in list) addItem(thing)
        actionSearch = false
    }

    fun setParent() {
        clear()
        val list = db.getParentData(this.parent)
        for (thing in list) addItem(thing)
        actionSearch = false
    }

    fun getParent(): Long = parent

    fun setSearchText(searchText: String) {
        clear()
        if (searchText.isNotEmpty()) {
            val list = db.getWhereData(searchText)
            for (thing in list) addItem(thing)
            actionSearch = true
        } else {
            actionSearch = false
        }
    }

    fun getActionSearch(): Boolean = actionSearch

    val path: String
        get() = if (actionSearch) "" else db.getPath(parent)

    fun addThing(thing: Thing?) {
        if (thing != null) {
            Log.d(LOG_TAG, "thingContent foto.size = ${thing.mFotos.size}")
            Log.d(LOG_TAG, "thingContent thing.ThingName = ${thing.thingName}")
            val id = db.addOrSetRec(thing)
            if (id != -1L && thing.thingName.trim().isNotEmpty()) {
                thing.id = id
                addItem(thing)
            }
        }
    }

    fun delThing(thing: Thing) {
        db.delRec(thing.id)
        delItem(thing)
    }

    fun delThings(setIdThings: Set<Long>) {
        for (id in setIdThings) {
            db.delRec(id)
            delItem(id)
        }
    }

    fun delThings(arrayIdThings: LongArray) {
        for (id in arrayIdThings) {
            db.delRec(id)
            delItem(id)
        }
    }

    fun updateThings(listIdThings: Set<Long>): Boolean {
        for (id in listIdThings) {
            if (db.getRec(id).parent == parent) {
                return false
            }
            db.updateParent(id, parent)
        }
        setParent()
        return true
    }

    fun saveZip(zipFile: File): Boolean = db.saveZip(zipFile)

    fun saveZip(name: String): Boolean = db.saveZip(File(BACKUP_DIR, name))

    fun loadZip(zipFile: File): Boolean {
        val result = db.loadZip(zipFile)
        setParent(0)
        return result
    }

    fun loadZip(name: String): Boolean {
        val result = db.loadZip(File(BACKUP_DIR, name))
        setParent(0)
        return result
    }

    fun getBackupDir(): File = File(BACKUP_DIR)

    fun getFotoDir(): File = db.fotoDir

    fun getBackupList(): List<File> {
        val filesBackup = getBackupDir().listFiles(
            FilenameFilter { _, s -> s.contains(".zip") }
        )
        val result: MutableList<File> = ArrayList()
        if (filesBackup == null) return result
        for (file in filesBackup) {
            result.add(file)
        }
        return result
    }

    fun getThingEdit(): Thing = thingEdit ?: Thing().also { thingEdit = it }

    fun setThingEdit(isBox: Int) {
        thingEdit = Thing(parent, isBox)
    }

    fun setThingEdit(id: Long) {
        thingEdit = getThing(id)
    }

    companion object {
        @Volatile
        private var sThingContent: ThingContent? = null

        @JvmStatic
        fun get(context: Context): ThingContent {
            return sThingContent ?: synchronized(this) {
                sThingContent ?: ThingContent(context.applicationContext).also { sThingContent = it }
            }
        }
    }
}











ChatGPT может допускать ошибки. Проверьте важную ин