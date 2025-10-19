package ru.frozik6k.finabox.thing

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Not Test : mAttributes
 */
class Thing : Parcelable {

    companion object {
        // Константы для определения хранилище или вещь
        const val AS_BOX = 1
        const val AS_THING = 0

        const val NEW_THING = 0

        // Строка для формирования уникального имени файла фотографии вещи
        const val DATE_FORMAT_FILE = "yyyyMMdd_HHmmss"

        // Переменная хранит формат даты
        //  "dd.MM.yyyy HH:mm:ss" - старый формат
        const val DATE_FORMAT = "yyyy-MM-DD HH:MM:SS.SSS"

        @JvmField
        val CREATOR: Parcelable.Creator<Thing> = object : Parcelable.Creator<Thing> {
            override fun createFromParcel(source: Parcel): Thing = Thing(source)
            override fun newArray(size: Int): Array<Thing?> = arrayOfNulls(size)
        }

        @JvmStatic
        fun dateToString(date: Date): String {
            val formatterDate = SimpleDateFormat(DATE_FORMAT)
            return formatterDate.format(date)
        }

        @JvmStatic
        fun dateToString(date: Date, format: String): String {
            val formatterDate = SimpleDateFormat(format)
            return formatterDate.format(date)
        }

        private fun stringToDate(date: String): Date? {
            val formatterDate = SimpleDateFormat(DATE_FORMAT)
            return try {
                formatterDate.parse(date)
            } catch (_: ParseException) {
                null
            }
        }
    }

    private val LOG_TAG = "myLogs"

    var id: Long = NEW_THING // 0 — не сохранена в БД
    var parent: Long = 0
    var thingName: String = ""
    var description: String? = null
    /** список фотографий (String - путь к фотографии) */
    val mFotos: MutableList<String>
    val mAttributes: MutableMap<String, String>
    var date: String? = null
    var isBox: Int = AS_THING
        set(value) {
            field = if (value != 0) 1 else 0
        }

    constructor(
        id: Long,
        parent: Long,
        thingName: String,
        description: String?,
        fotos: List<String>?,
        attributes: Map<String, String>?,
        date: String?,
        isBox: Int
    ) {
        this.id = id
        this.parent = parent
        this.thingName = thingName
        this.description = description
        this.mFotos = if (fotos == null) FotoList() else FotoList(fotos)
        this.mAttributes = if (attributes == null) HashMap() else HashMap(attributes)
        this.date = date
        this.isBox = isBox
    }

    // Конструктор используется для создание кнопки ".."
    constructor(id: Long, parent: Long, thingName: String) {
        this.id = id
        this.parent = parent
        this.thingName = thingName
        this.isBox = AS_BOX
        this.mFotos = FotoList()
        this.mAttributes = HashMap()
    }

    constructor() {
        parent = 0
        thingName = ""
        isBox = AS_THING
        mFotos = FotoList()
        mAttributes = HashMap()
    }

    constructor(parent: Long, isBox: Int) {
        this.parent = parent
        this.thingName = ""
        this.isBox = isBox
        this.mFotos = FotoList()
        this.mAttributes = HashMap()
    }

    private constructor(`in`: Parcel) {
        id = `in`.readLong()
        parent = `in`.readLong()
        thingName = `in`.readString() ?: ""
        description = `in`.readString()
        mFotos = FotoList(`in`.readString() ?: "{}") // через json
        `in`.readStringList(mFotos)

        mAttributes = HashMap()
        val keys = ArrayList<String>()
        val values = ArrayList<String>()
        `in`.readStringList(keys)
        `in`.readStringList(values)
        for (i in keys.indices) {
            mAttributes[keys[i]] = values.getOrNull(i) ?: ""
        }
        date = `in`.readString()
        isBox = `in`.readInt()
    }

    override fun toString(): String {
        return "id = $id parent = $parent name = $thingName"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeLong(parent)
        dest.writeString(thingName)
        dest.writeString(description)
        dest.writeString((mFotos as FotoList).outputJson())
        val keys = ArrayList<String>()
        val values = ArrayList<String>()
        for ((k, v) in mAttributes) {
            keys.add(k)
            values.add(v)
        }
        dest.writeStringList(keys)
        dest.writeStringList(values)
        dest.writeString(date)
        dest.writeInt(isBox)
    }

    fun setID(id: Long) {
        this.id = id
    }

    fun getID(): Long = id
    fun getParent(): Long = parent
    fun setParent(parent: Long) {
        this.parent = parent
    }

    fun getThingName(): String = thingName
    fun setThingName(thingName: String) {
        this.thingName = thingName
    }

    fun getDescription(): String? = description
    fun setDescription(description: String?) {
        this.description = description
    }

    fun getDate(): String? = date
    fun setDate(date: String?) {
        this.date = date
    }

    fun getIsBox(): Int = isBox
    fun setIsBox(isBox: Int) {
        this.isBox = isBox
    }

    fun isNewThing(): Boolean = id == 0L

    /** Возвращает список новых фотографий, требующих сохранения в БД (имена файлов). */
    fun getNewFotos(): List<String> = (mFotos as FotoList).newPhotos

    /** Фиксирует новые/удалённые фото (после сохранения в БД). */
    fun saveListFoto(id: Long) {
        if (this.id == 0L) this.id = id
        (mFotos as FotoList).saveListFoto()
    }

    /** Отменяет новые фото и возвращает основной список к исходному состоянию. */
    fun cancelListFoto() {
        (mFotos as FotoList).cancelListFoto()
    }

    /** очистка списка фоток и удаление всех файлов */
    fun clearListFoto() {
        (mFotos as FotoList).clear()
    }

    /** Внутренний список фотографий с трекингом новых/удалённых. */
    private inner class FotoList : ArrayList<String> {

        private val KEY_NEW_PHOTOS = "key_new_photos"
        private val KEY_REMOVE_PHOTOS = "key_remote_photos"
        private val KEY_PHOTOS = "key_photos"

        val mNewPhotos: MutableList<String> = ArrayList()
        val mRemovePhotos: MutableList<String> = ArrayList()

        constructor() : super()

        constructor(collection: Collection<String>) : super() {
            for (foto in collection) {
                super.add(foto)
            }
        }

        constructor(json: String) : super() {
            try {
                val dataJsonObj = JSONObject(json)
                val thisJson = dataJsonObj.optJSONArray(KEY_PHOTOS) ?: JSONArray()
                val newPhotosJson = dataJsonObj.optJSONArray(KEY_NEW_PHOTOS) ?: JSONArray()
                val removePhotosJson = dataJsonObj.optJSONArray(KEY_REMOVE_PHOTOS) ?: JSONArray()

                for (i in 0 until thisJson.length()) {
                    val photo = thisJson.getString(i)
                    super.add(photo)
                }
                for (i in 0 until newPhotosJson.length()) {
                    val newPhoto = newPhotosJson.getString(i)
                    mNewPhotos.add(newPhoto)
                }
                for (i in 0 until removePhotosJson.length()) {
                    val removePhoto = removePhotosJson.getString(i)
                    mRemovePhotos.add(removePhoto)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        fun outputJson(): String {
            val resultJson = JSONObject()
            val thisJson = JSONArray()
            val newPhotosJson = JSONArray()
            val removePhotosJson = JSONArray()

            for (i in indices) thisJson.put(get(i))
            for (newPhoto in mNewPhotos) newPhotosJson.put(newPhoto)
            for (removePhoto in mRemovePhotos) removePhotosJson.put(removePhoto)

            try {
                resultJson.put(KEY_PHOTOS, thisJson)
                resultJson.put(KEY_NEW_PHOTOS, newPhotosJson)
                resultJson.put(KEY_REMOVE_PHOTOS, removePhotosJson)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return resultJson.toString()
        }

        override fun add(element: String): Boolean {
            val file = File(element)
            return if (file.isFile) {
                mNewPhotos.add(file.absolutePath)
                super.add(element)
            } else {
                false
            }
        }

        override fun remove(element: Any?): Boolean {
            element as String
            val file = File(element)
            val foto = file.absolutePath
            if (mNewPhotos.contains(foto)) {
                mNewPhotos.remove(foto)
                if (file.isFile) file.delete()
            } else {
                mRemovePhotos.add(foto)
            }
            return super.remove(element)
        }

        /** Возвращает список новых фотографий (только имена) */
        val newPhotos: List<String>
            get() {
                val result: MutableList<String> = ArrayList()
                for (nameFoto in mNewPhotos) {
                    val file = File(nameFoto)
                    result.add(file.name)
                }
                return result
            }

        /** сохраняет текущее состояние (удаляет с диска удалённые фотографии) */
        fun saveListFoto() {
            for (removePhoto in mRemovePhotos) {
                val file = File(removePhoto)
                if (file.isFile) file.delete()
            }
            mRemovePhotos.clear()
            mNewPhotos.clear()
        }

        /** Удаляет с диска новые фотографии и откатывает состояние списка */
        fun cancelListFoto(): Boolean {
            for (newPhoto in mNewPhotos) {
                val file = File(newPhoto)
                super.remove(newPhoto)
                if (file.isFile) file.delete()
            }
            mNewPhotos.clear()
            for (removePhoto in mRemovePhotos) {
                super.add(removePhoto)
            }
            mRemovePhotos.clear()
            return true
        }

        override fun clear() {
            val files = super.toTypedArray()
            for (name in files) {
                val file = File(name)
                if (file.isFile) file.delete()
            }
            for (name in mRemovePhotos) {
                val file = File(name)
                if (file.isFile) file.delete()
            }
            mRemovePhotos.clear()
            mNewPhotos.clear()
            super.clear()
        }
    }
}