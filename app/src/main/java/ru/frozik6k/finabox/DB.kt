package ru.frozik6k.finabox

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.provider.BaseColumns
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import ru.frozik6k.finabox.thing.Thing
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.ParseException
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DB(private val mContext: Context) {
    companion object {
        // имя базы данных
        private const val DB_NAME = "LOHdatabase.sqlite"

        // имя с полным путем к базе данных (устанавливается в конструкторе)
        private var DB_FULL_NAME: String = DB_NAME

        // версия базы данных
        private const val DB_VERSION = 3

        // ТАБЛИЦА СО СПИСКОМ ВСЕХ ВЕЩЕЙ И ХРАНИЛИЩ
        private const val DB_TABLE_NAME = "listOfThings"
        const val COLUMN_PARENT = "parent"                 // Родитель
        const val COLUMN_THING_NAME = "thing_name"         // Имя вещи
        const val COLUMN_DESCRIPTION = "description"       // Описание вещи
        const val COLUMN_FOTO = "foto"                     // ссылка на фотографию (с БД v3 не используется)
        const val COLUMN_DATE = "date"                     // дата добавления записи
        const val COLUMN_ISBOX = "isBox"                   // 1 - каталог, 0 - вещь

        private const val DB_CREATE_SCRIPT = "create table " +
                DB_TABLE_NAME + " (" +
                BaseColumns._ID + " integer primary key autoincrement, " +
                "$COLUMN_PARENT integer not null, " +
                "$COLUMN_THING_NAME text not null, " +
                "$COLUMN_DESCRIPTION text, " +
                "$COLUMN_FOTO text, " +
                "$COLUMN_DATE text, " +
                "$COLUMN_ISBOX integer not null);"

        // ТАБЛИЦА СО СПИСКОМ ИМЕЮЩИХСЯ АТРИБУТОВ
        private const val DB_TABLE_NAME_ATTR = "attr"
        const val COLUMN_NAME_ATTR = "name" // Название атрибута

        private const val DB_CREATE_TABLE_ATTR_SCRIPT = "create table " +
                DB_TABLE_NAME_ATTR + " (" +
                BaseColumns._ID + " integer primary key autoincrement, " +
                "$COLUMN_NAME_ATTR text not null);"

        // ТАБЛИЦА СО СПИСКОМ ВСЕХ АТРИБУТОВ ВЕЩЕЙ
        private const val DB_TABLE_NAME_LIST_ATTR = "list_attr"
        const val COLUMN_ID_THING = "id_thing"                 // ИД вещи
        const val COLUMN_ID_ATTR = "id_attr"                   // ИД атрибута
        const val COLUMN_DESCRIPTION_ATTR = "description_attr" // Значение атрибута

        private const val DB_CREATE_TABLE_LIST_ATTR_SCRIPT = "create table " +
                DB_TABLE_NAME_LIST_ATTR + " (" +
                BaseColumns._ID + " integer primary key autoincrement, " +
                "$COLUMN_ID_THING integer not null, " +
                "$COLUMN_ID_ATTR integer not null, " +
                "$COLUMN_DESCRIPTION_ATTR text, " +
                "FOREIGN KEY($COLUMN_ID_THING) REFERENCES $DB_TABLE_NAME(${BaseColumns._ID}), " +
                "FOREIGN KEY($COLUMN_ID_ATTR) REFERENCES $DB_TABLE_NAME_ATTR(${BaseColumns._ID}));"

        // ТАБЛИЦА СО СПИСКОМ ФОТОГРАФИЙ
        private const val DB_TABLE_NAME_FOTOS = "fotos"
        const val COLUMN_NAME_FOTO = "name_file"               // Задается только имя файла
        const val COLUMN_DESCRIPTION_FOTO = "description_foto" // Описание фотографии

        private const val DB_CREATE_TABLE_FOTOS_SCRIPT = "create table " +
                DB_TABLE_NAME_FOTOS + " (" +
                BaseColumns._ID + " integer primary key autoincrement, " +
                "$COLUMN_ID_THING integer not null, " +
                "$COLUMN_NAME_FOTO text not null, " +
                "$COLUMN_DESCRIPTION_FOTO text, " +
                "$COLUMN_DATE text, " +
                "FOREIGN KEY($COLUMN_ID_THING) REFERENCES $DB_TABLE_NAME(${BaseColumns._ID}));"

        // Все имена таблиц
        private val TABLES = arrayOf(
            DB_TABLE_NAME, DB_TABLE_NAME_ATTR, DB_TABLE_NAME_LIST_ATTR, DB_TABLE_NAME_FOTOS
        )

        // Константа для определения перехода в верхний каталог
        const val BACK_DIR = ".."

        // Имя папки для архивов
        private const val ZIP_DIR = "backup"

        // Максимальное количество резервных копий
        private const val BACKUP_COUNT = 4

        // Формат имени файла резервной копии
        const val DATE_FORMAT_BACKUP = "yyyy_MM_dd_HH_mm_ss"

        // Префикс имени бэкапа
        const val BACKUP_NAME_PREFIX = "Finabox_"

        // Массив столбцов таблицы вещей
        private val COLUMNS = arrayOf(
            BaseColumns._ID,
            COLUMN_PARENT,
            COLUMN_THING_NAME,
            COLUMN_DESCRIPTION,
            COLUMN_FOTO,
            COLUMN_DATE,
            COLUMN_ISBOX
        )

        // Специальный курсор (пункт "..")
        private val BACK_DIR_HEADER_CURSOR = MatrixCursor(COLUMNS).apply {
            addRow(arrayOf("0", "0", BACK_DIR, null, null, null, "1"))
        }

        // Сохранять данные во внешнюю память
        private const val isSD = false

        private const val LOG_TAG = "DB"
    }

    // Путь к данным на внешнем/внутреннем носителе
    val pathData: String

    // Каталог с фотками
    private var fotoDir: File = File("")

    fun getFotoDir(): File = fotoDir

    private var openDB = false
    fun isOpenDB(): Boolean = openDB

    private var mDBHelper: DBHelper? = null
    private lateinit var mDB: SQLiteDatabase

    init {
        pathData = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val p = mContext.getExternalFilesDir(null)!!.absolutePath
            val path = File(p)
            if (!path.exists()) path.mkdirs()
            p
        } else {
            mContext.filesDir.absolutePath
        }
        createFotoDir()

        DB_FULL_NAME = if (isSD) "$pathData/$DB_NAME" else DB_NAME
    }

    // открыть подключение
    fun open() {
        mDBHelper = DBHelper(mContext, DB_FULL_NAME, null, DB_VERSION)
        mDB = mDBHelper!!.writableDatabase
        openDB = true
    }

    // закрыть подключение
    fun close() {
        mDBHelper?.close()
        openDB = false
    }

    // получить все данные из таблицы
    fun getAllData(tableName: String): Cursor =
        mDB.query(tableName, null, null, null, null, null, null)

    fun getParentData(parent: Long): List<Thing> {
        val cursor = mDB.query(
            DB_TABLE_NAME,
            arrayOf(
                BaseColumns._ID, COLUMN_PARENT, COLUMN_THING_NAME,
                COLUMN_DESCRIPTION, COLUMN_FOTO, COLUMN_DATE, COLUMN_ISBOX
            ),
            "$COLUMN_PARENT=${parent}",
            null, null, null,
            "$COLUMN_ISBOX DESC, $COLUMN_THING_NAME"
        )
        return cursorToList(cursor)
    }

    // получить данные по текстовому запросу Where
    fun getWhereData(text: String): List<Thing> {
        val where = "$COLUMN_THING_NAME LIKE '%$text%'"
        val cursor = mDB.query(
            DB_TABLE_NAME,
            arrayOf(
                BaseColumns._ID, COLUMN_THING_NAME, COLUMN_PARENT,
                COLUMN_DESCRIPTION, COLUMN_FOTO, COLUMN_DATE, COLUMN_ISBOX
            ),
            where, null, null, null, COLUMN_THING_NAME
        )
        return cursorToList(cursor)
    }

    fun cursorToList(cursor: Cursor): List<Thing> {
        val list = ArrayList<Thing>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)).toLong()

            // фотографии вещи
            val fotos = ArrayList<String>()
            val cursorFotos = mDB.query(
                DB_TABLE_NAME_FOTOS, arrayOf(COLUMN_NAME_FOTO),
                "$COLUMN_ID_THING=$id", null, null, null, null
            )
            while (cursorFotos.moveToNext()) {
                val name = cursorFotos.getString(cursorFotos.getColumnIndex(COLUMN_NAME_FOTO))
                val file = File(fotoDir.absolutePath, name)
                if (file.isFile) {
                    fotos.add("${fotoDir.absolutePath}/$name")
                }
            }
            cursorFotos.close()

            // атрибуты вещи
            val attributes: MutableMap<String, String> = HashMap()
            val cursorAttributes = mDB.query(
                DB_TABLE_NAME_LIST_ATTR, arrayOf(COLUMN_ID_ATTR, COLUMN_DESCRIPTION_ATTR),
                "$COLUMN_ID_THING=$id", null, null, null, null
            )
            while (cursorAttributes.moveToNext()) {
                val idAttr = cursorAttributes.getLong(cursorAttributes.getColumnIndex(COLUMN_ID_ATTR))
                val cursorAttr = mDB.query(
                    DB_TABLE_NAME_ATTR, arrayOf(COLUMN_NAME_ATTR),
                    "$COLUMN_ID_ATTR=$idAttr", null, null, null, null
                )
                if (cursorAttr.moveToFirst()) {
                    attributes[cursorAttr.getString(cursorAttr.getColumnIndex(COLUMN_NAME_ATTR))] =
                        cursorAttributes.getString(cursorAttributes.getColumnIndex(COLUMN_DESCRIPTION_ATTR))
                }
                cursorAttr.close()
            }
            cursorAttributes.close()

            // объект Thing
            val thing = Thing(
                cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_PARENT)),
                cursor.getString(cursor.getColumnIndex(COLUMN_THING_NAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                fotos,
                attributes,
                cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_ISBOX))
            )
            list.add(thing)
        }
        cursor.close()
        return list
    }

    // Возвращает объект Thing с данными о вещи id
    fun getRec(id: Long): Thing? {
        val cursor = mDB.query(
            DB_TABLE_NAME,
            arrayOf(
                BaseColumns._ID, COLUMN_PARENT, COLUMN_THING_NAME, COLUMN_DESCRIPTION,
                COLUMN_FOTO, COLUMN_DATE, COLUMN_ISBOX
            ),
            "${BaseColumns._ID}=$id", null, null, null, null
        )

        // фото
        val fotos = ArrayList<String>()
        val cursorFotos = mDB.query(
            DB_TABLE_NAME_FOTOS, arrayOf(COLUMN_NAME_FOTO),
            "$COLUMN_ID_THING=$id", null, null, null, null
        )
        while (cursorFotos.moveToNext()) {
            val name = cursorFotos.getString(cursorFotos.getColumnIndex(COLUMN_NAME_FOTO))
            val file = File(fotoDir.absolutePath, name)
            if (file.isFile) fotos.add("${fotoDir.absolutePath}/$name")
        }
        cursorFotos.close()

        // атрибуты
        val attributes: MutableMap<String, String> = HashMap()
        val cursorAttributes = mDB.query(
            DB_TABLE_NAME_LIST_ATTR, arrayOf(COLUMN_ID_ATTR, COLUMN_DESCRIPTION_ATTR),
            "$COLUMN_ID_THING=$id", null, null, null, null
        )
        while (cursorAttributes.moveToNext()) {
            val idAttr = cursorAttributes.getLong(cursorAttributes.getColumnIndex(COLUMN_ID_ATTR))
            val cursorAttr = mDB.query(
                DB_TABLE_NAME_ATTR, arrayOf(COLUMN_NAME_ATTR),
                "$COLUMN_ID_ATTR=$idAttr", null, null, null, null
            )
            if (cursorAttr.moveToFirst()) {
                attributes[cursorAttr.getString(cursorAttr.getColumnIndex(COLUMN_NAME_ATTR))] =
                    cursorAttributes.getString(cursorAttributes.getColumnIndex(COLUMN_DESCRIPTION_ATTR))
            }
            cursorAttr.close()
        }
        cursorAttributes.close()

        val thing = if (cursor.moveToFirst()) {
            Thing(
                cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                cursor.getLong(cursor.getColumnIndex(COLUMN_PARENT)),
                cursor.getString(cursor.getColumnIndex(COLUMN_THING_NAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                fotos,
                attributes,
                cursor.getString(cursor.getColumnIndex(COLUMN_DATE)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_ISBOX))
            )
        } else null
        cursor.close()
        return thing
    }

    // Добавляет объект Thing в базу или апдейтит, если уже есть
    // В случае успеха возвращает id записи, иначе -1
    fun addOrSetRec(thing: Thing): Long {
        var result = -1L
        if (thing.thingName.trim().isNotEmpty()) {
            if (!thing.isNewThing) {
                Log.d("ThingContent", "Все хуево id = ${thing.id}")
                updateRec(thing)
                return thing.id
            }

            val newValues = ContentValues().apply {
                put(COLUMN_THING_NAME, thing.thingName)
                put(COLUMN_PARENT, thing.parent)
                put(COLUMN_DESCRIPTION, thing.description)
                put(COLUMN_DATE, thing.date)
                put(COLUMN_ISBOX, thing.isBox)
            }

            val id = mDB.insert(DB_TABLE_NAME, null, newValues)

            val fotos = thing.newFotos
            Log.d("ThingContent", "fotos size = ${fotos.size}")

            for (nameFoto in fotos) {
                val valuesFoto = ContentValues().apply {
                    put(COLUMN_ID_THING, id)
                    put(COLUMN_NAME_FOTO, nameFoto)
                }
                Log.d("ThingContent", "nameFoto = $nameFoto")
                mDB.insert(DB_TABLE_NAME_FOTOS, null, valuesFoto)
            }

            thing.saveListFoto(id)
            result = id
        } else {
            // не сохраняем — очищаем временные фото
            thing.cancelListFoto()
        }
        return result
    }

    fun updateParent(id: Long, newParent: Long) {
        val cv = ContentValues().apply { put(COLUMN_PARENT, newParent) }
        mDB.update(DB_TABLE_NAME, cv, "${BaseColumns._ID}=$id", null)
    }

    // обновление записи
    private fun updateRec(thing: Thing) {
        val newValues = ContentValues().apply {
            put(COLUMN_THING_NAME, thing.thingName)
            put(COLUMN_PARENT, thing.parent)
            put(COLUMN_DESCRIPTION, thing.description)
            put(COLUMN_DATE, thing.date)
            put(COLUMN_ISBOX, thing.isBox)
        }
        val id = thing.id
        mDB.update(DB_TABLE_NAME, newValues, "${BaseColumns._ID}=$id", null)

        // Добавляем новые фото
        val newFotos = thing.newFotos
        if (newFotos != null) {
            for (nameFoto in newFotos) {
                val valuesFoto = ContentValues().apply {
                    put(COLUMN_ID_THING, id)
                    put(COLUMN_NAME_FOTO, nameFoto)
                }
                mDB.insert(DB_TABLE_NAME_FOTOS, null, valuesFoto)
            }
        }
        thing.saveListFoto(id)
    }

    // удалить запись
    fun delRec(id: Long) {
        val thing = getRec(id) ?: return
        thing.clearListFoto()

        mDB.delete(DB_TABLE_NAME_FOTOS, "$COLUMN_ID_THING = $id", null)
        mDB.delete(DB_TABLE_NAME, "${BaseColumns._ID} = $id", null)

        // если каталог — удаляем содержимое
        if (thing.isBox == Thing.AS_BOX) {
            val list = getParentData(id)
            for (item in list) {
                delRec(item.id)
            }
        }
    }

    // получение строки пути
    fun getPath(id: Long): String? {
        var iParent = id
        val result = StringBuilder("\\ ")
        while (iParent != 0L) {
            val cursor = mDB.query(
                DB_TABLE_NAME, arrayOf(COLUMN_PARENT, COLUMN_THING_NAME),
                "${BaseColumns._ID}=$iParent", null, null, null, null
            )
            if (cursor.moveToFirst()) {
                result.insert(0, "\\ " + cursor.getString(cursor.getColumnIndex(COLUMN_THING_NAME)) + " ")
                iParent = cursor.getLong(cursor.getColumnIndex(COLUMN_PARENT))
            } else {
                cursor.close()
                return null
            }
            cursor.close()
        }
        return result.toString()
    }

    private fun createFotoDir(): Boolean {
        fotoDir = File(pathData, Environment.DIRECTORY_PICTURES)
        if (!fotoDir.exists()) fotoDir.mkdirs()

        val fileNoMedia = File(fotoDir, ".nomedia")
        if (!fileNoMedia.exists()) {
            return try {
                FileOutputStream(fileNoMedia.absolutePath).use { }
                true
            } catch (e: IOException) {
                false
            }
        }
        return true
    }

    // --- JSON helpers ---

    // Сохранение текущего значения cursor[column] в writer
    @Throws(IOException::class)
    private fun saveValueJSON(writer: JsonWriter, cursor: Cursor, column: String) {
        writer.name(column)
        val indexColumn = cursor.getColumnIndex(column)
        when (cursor.getType(indexColumn)) {
            Cursor.FIELD_TYPE_INTEGER -> writer.value(cursor.getLong(indexColumn))
            Cursor.FIELD_TYPE_FLOAT -> writer.value(cursor.getFloat(indexColumn).toDouble())
            Cursor.FIELD_TYPE_STRING -> writer.value(cursor.getString(indexColumn))
            else -> writer.nullValue()
        }
    }

    // Класс для записи нескольких JSON файлов в архив
    private class MyZipOutputStream(os: OutputStream) : ZipOutputStream(os) {
        @Throws(IOException::class)
        override fun close() {
            super.closeEntry()
        }

        @Throws(IOException::class)
        fun closeclose() {
            super.close()
        }
    }

    fun saveZip(zipFile: File): Boolean {
        Log.d(LOG_TAG, zipFile.absolutePath)
        var result = false

        if (!zipFile.exists()) zipFile.parentFile?.mkdirs()
        try {
            val zipOutputStream = MyZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
            val fotos: MutableList<String> = ArrayList()

            for (tableName in TABLES) {
                Log.d(LOG_TAG, "ЗАПИСЬ: $tableName")
                val cursor = getAllData(tableName)
                if (cursor.count == 0) {
                    cursor.close()
                    continue
                }
                Log.d(LOG_TAG, "cursor.getCount() = ${cursor.count}")
                val columns = cursor.columnNames
                val entry = ZipEntry("$tableName.json")
                zipOutputStream.putNextEntry(entry)
                val writer = JsonWriter(OutputStreamWriter(zipOutputStream, Charsets.UTF_8))
                writer.beginArray()
                while (cursor.moveToNext()) {
                    writer.beginObject()
                    for (column in columns) {
                        saveValueJSON(writer, cursor, column)
                        if (column == COLUMN_NAME_FOTO) {
                            fotos.add(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_FOTO)))
                        }
                    }
                    writer.endObject()
                }
                writer.endArray()
                writer.close()
                cursor.close()
            }

            // Сохраняем фотки в архив
            Log.d(LOG_TAG, "Сохраняем фото в архив")
            if (fotos.isNotEmpty()) {
                for (fotoName in fotos) {
                    val fileFoto = File(fotoDir, fotoName)
                    if (fileFoto.isFile) {
                        val entry = ZipEntry(fileFoto.name)
                        zipOutputStream.putNextEntry(entry)
                        val buffer = ByteArray(1024)
                        FileInputStream(fileFoto).use { inputStream ->
                            var length: Int
                            while (inputStream.read(buffer).also { length = it } > -1) {
                                zipOutputStream.write(buffer, 0, length)
                            }
                        }
                        zipOutputStream.closeEntry()
                    }
                }
            }
            zipOutputStream.closeclose()
        } catch (e: IOException) {
            Log.d(LOG_TAG, e.toString())
            return result
        }
        result = true
        return result
    }

    private fun isTable(nameTable: String): Boolean = TABLES.any { it == nameTable }

    private fun saveValue(cv: ContentValues, key: String, value: String) {
        try {
            val l = value.toLong()
            cv.put(key, l)
        } catch (_: NumberFormatException) {
            try {
                val f = value.toFloat()
                cv.put(key, f)
            } catch (_: NumberFormatException) {
                cv.put(key, value)
            }
        }
    }

    // загрузка базы из архива, true если удачно
    fun loadZip(zipFile: File): Boolean {
        var result = false
        try {
            mDB.beginTransaction()

            // Очистка таблиц
            for (tableName in TABLES) {
                mDB.delete(tableName, null, null)
            }

            // Переименуем старые фото с префиксом tmp
            var filesFotoOld = File(fotoDir.absolutePath).listFiles() ?: emptyArray()
            for (i in filesFotoOld.indices) {
                val nameFoto = filesFotoOld[i].name
                if (nameFoto == ".nomedia") continue
                val nameFotoNew = "tmp$nameFoto"
                val fileFotoNew = File(filesFotoOld[i].parent, nameFotoNew)
                if (filesFotoOld[i].renameTo(fileFotoNew)) {
                    filesFotoOld[i] = fileFotoNew
                } else {
                    return false
                }
            }

            // Читаем zip
            val isr: InputStream = FileInputStream(zipFile)
            val zis = ZipInputStream(BufferedInputStream(isr))
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val name = entry!!.name.replace(".json", "").trim()
                // Если JSON таблицы
                if (isTable(name)) {
                    val reader = JsonReader(InputStreamReader(zis, Charsets.UTF_8))
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        val cv = ContentValues()
                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            try {
                                val value = reader.nextString()
                                saveValue(cv, key, value)
                            } catch (_: IllegalStateException) {
                                reader.skipValue()
                            }
                        }
                        mDB.insert(name, null, cv)
                        reader.endObject()
                    }
                    reader.endArray()
                    // не закрываем reader вручную — он оборачивает zis
                } else {
                    // Файлы (фото)
                    val file = File("${fotoDir.absolutePath}/${entry!!.name}")
                    FileOutputStream(file).use { os ->
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (zis.read(buffer).also { count = it } != -1) {
                            os.write(buffer, 0, count)
                        }
                    }
                }
                zis.closeEntry()
            }
            zis.close()

            mDB.setTransactionSuccessful()
            result = true

            // Полностью удаляем старые фото с префиксом tmp
            filesFotoOld = File(fotoDir.absolutePath).listFiles() ?: emptyArray()
            for (file in filesFotoOld) {
                val nameFoto = file.name
                if (nameFoto == ".nomedia") continue
                if (nameFoto.length >= 3 && nameFoto.substring(0, 3) == "tmp") {
                    if (file.isFile) file.delete()
                }
            }
        } catch (e: IOException) {
            Log.d(LOG_TAG, e.toString())
            // Неудача — откатываем имена старых фото
            val filesFotoOld = File(fotoDir.absolutePath).listFiles() ?: emptyArray()
            for (i in filesFotoOld.indices) {
                val nameFoto = filesFotoOld[i].name
                if (nameFoto == ".nomedia") continue
                if (nameFoto.length >= 3 && nameFoto.substring(0, 3) == "tmp") {
                    val nameFotoNew = nameFoto.substring(3)
                    val fileFotoNew = File(filesFotoOld[i].parent, nameFotoNew)
                    if (filesFotoOld[i].renameTo(fileFotoNew)) {
                        filesFotoOld[i] = fileFotoNew
                    }
                }
            }
            result = false
        } finally {
            mDB.endTransaction()
        }
        return result
    }

    // --- SQLiteOpenHelper ---
    private inner class DBHelper(
        context: Context,
        name: String,
        factory: SQLiteDatabase.CursorFactory?,
        version: Int
    ) : SQLiteOpenHelper(context, name, factory, version) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DB_CREATE_SCRIPT)
            db.execSQL(DB_CREATE_TABLE_ATTR_SCRIPT)
            db.execSQL(DB_CREATE_TABLE_FOTOS_SCRIPT)
            db.execSQL(DB_CREATE_TABLE_LIST_ATTR_SCRIPT)

            // Заполнение примерными данными
            val res: Resources = mContext.resources
            val thingStrings = res.getStringArray(R.array.thing_array_primer)
            val storeHouseStrings = res.getStringArray(R.array.storehouse_array_primer)
            val atticStrings = res.getStringArray(R.array.Attic_array_primer)
            val basementStrings = res.getStringArray(R.array.Basement_array_primer)

            for (thing in thingStrings) {
                val newValues = ContentValues().apply {
                    put(COLUMN_PARENT, 0)
                    put(COLUMN_THING_NAME, thing)
                    put(COLUMN_ISBOX, Thing.AS_BOX)
                }
                db.insert(DB_TABLE_NAME, null, newValues)
            }

            for (thing in storeHouseStrings) {
                val newValues = ContentValues().apply {
                    put(COLUMN_PARENT, 1)
                    put(COLUMN_THING_NAME, thing)
                    put(COLUMN_ISBOX, Thing.AS_THING)
                }
                db.insert(DB_TABLE_NAME, null, newValues)
            }

            for (thing in atticStrings) {
                val newValues = ContentValues().apply {
                    put(COLUMN_PARENT, 2)
                    put(COLUMN_THING_NAME, thing)
                    put(COLUMN_ISBOX, Thing.AS_THING)
                }
                db.insert(DB_TABLE_NAME, null, newValues)
            }

            for (thing in basementStrings) {
                val newValues = ContentValues().apply {
                    put(COLUMN_PARENT, 3)
                    put(COLUMN_THING_NAME, thing)
                    put(COLUMN_ISBOX, Thing.AS_THING)
                }
                db.insert(DB_TABLE_NAME, null, newValues)
            }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.d(LOG_TAG, "SQL Обновляем с версии $oldVersion на версию $newVersion")
            if (oldVersion == 1) {
                db.execSQL(DB_CREATE_TABLE_ATTR_SCRIPT)
                db.execSQL(DB_CREATE_TABLE_FOTOS_SCRIPT)
                db.execSQL(DB_CREATE_TABLE_LIST_ATTR_SCRIPT)

                val cursor = db.query(DB_TABLE_NAME, null, null, null, null, null, null)
                while (cursor.moveToNext()) {
                    val date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE))
                    val id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)).toLong()
                    val foto = cursor.getString(cursor.getColumnIndex(COLUMN_FOTO))
                    val dateStr: String = run {
                        if (date != null) {
                            var formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                            var parsed: Date? = try {
                                formatter.parse(date)
                            } catch (_: ParseException) {
                                null
                            }
                            formatter = SimpleDateFormat(Thing.DATE_FORMAT, Locale.getDefault())
                            parsed?.let { formatter.format(it) } ?: formatter.format(Date())
                        } else {
                            val formatter = SimpleDateFormat(Thing.DATE_FORMAT, Locale.getDefault())
                            formatter.format(Date())
                        }
                    }

                    val newValues = ContentValues().apply {
                        put(COLUMN_FOTO, "")
                        put(COLUMN_DATE, dateStr)
                    }
                    db.update(DB_TABLE_NAME, newValues, "${BaseColumns._ID} = $id", null)

                    if (foto != null && foto.isNotEmpty()) {
                        val fotoValues = ContentValues().apply {
                            put(COLUMN_ID_THING, id)
                            put(COLUMN_NAME_FOTO, foto)
                        }
                        db.insert(DB_TABLE_NAME_FOTOS, null, fotoValues)
                    }
                }
                cursor.close()
            }

            if (oldVersion == 2) {
                Log.d(LOG_TAG, "oldVersion == 2")
                db.execSQL("ALTER TABLE $DB_TABLE_NAME_FOTOS ADD COLUMN $COLUMN_DATE text;")
            }
        }
    }
}