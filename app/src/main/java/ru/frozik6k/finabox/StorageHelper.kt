package ru.frozik6k.finabox

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

class StorageHelper private constructor() {

    private val mGetter: MountDeviceGetter = MountDeviceGetter().apply {
        fillDevicesEnvirement()
    }

    fun getAllMountedDevices(): ArrayList<MountDevice> {
        val mountedDevice = ArrayList<MountDevice>(mGetter.mountedExternalDevices)
        mountedDevice.addAll(mGetter.mountedRemovableDevices)
        return mountedDevice
    }

    fun getExternalMountedDevices(): ArrayList<MountDevice> = mGetter.mountedExternalDevices

    fun getRemovableMountedDevices(): ArrayList<MountDevice> = mGetter.mountedRemovableDevices

    companion object {
        @Volatile
        private var sStorage: StorageHelper? = null

        fun getInstance(): StorageHelper {
            return sStorage ?: synchronized(this) {
                sStorage ?: StorageHelper().also { sStorage = it }
            }
        }
    }

    enum class MountDeviceType {
        EXTERNAL_SD_CARD, REMOVABLE_SD_CARD
    }

    inner class MountDevice(
        private val mType: MountDeviceType,
        private val mPath: String,
        private val mHash: Int
    ) {

        fun getType(): MountDeviceType = mType
        fun getPath(): String = mPath
        fun getHash(): Int = mHash

        override fun equals(other: Any?): Boolean {
            if (other !is MountDevice) return false
            // Сохраняем оригинальную "особенную" семантику equals из Java:
            // если пути равны — считаем равными; иначе сравниваем hash.
            return if (mPath != other.getPath()) {
                mHash == other.getHash()
            } else {
                true
            }
        }

        override fun hashCode(): Int = mHash
    }

    private inner class MountDeviceGetter {
        var mountedExternalDevices: ArrayList<MountDevice> = ArrayList(3)
            private set
        var mountedRemovableDevices: ArrayList<MountDevice> = ArrayList(3)
            private set

        private fun calcHash(dir: File): Int {
            val tmpHash = StringBuilder()

            tmpHash.append(dir.totalSpace)
            tmpHash.append(dir.usableSpace)

            val list = dir.listFiles()
            if (list != null) {
                for (file in list) {
                    tmpHash.append(file.name)
                    if (file.isFile) {
                        tmpHash.append(file.length())
                    }
                }
            }

            return tmpHash.toString().hashCode()
        }

        private fun testAndAdd(path: String, type: MountDeviceType) {
            var root = File(path)
            if (root.exists() && root.isDirectory && root.canWrite()) {
                val device = MountDevice(type, path, calcHash(root))
                when (type) {
                    MountDeviceType.EXTERNAL_SD_CARD -> {
                        if (!mountedExternalDevices.contains(device)) {
                            mountedExternalDevices.add(device)
                        }
                    }
                    MountDeviceType.REMOVABLE_SD_CARD -> {
                        if (!mountedRemovableDevices.contains(device)) {
                            mountedRemovableDevices.add(device)
                        }
                    }
                }
            }
            root = null
        }

        fun fillDevicesEnvirement() {
            mountedExternalDevices = ArrayList(3)
            mountedRemovableDevices = ArrayList(3)

            // external
            val path = android.os.Environment.getExternalStorageDirectory().absolutePath
            if (path.isNotBlank() &&
                android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
            ) {
                testAndAdd(path, MountDeviceType.EXTERNAL_SD_CARD)
            }

            // removable
            val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
            if (!rawSecondaryStoragesStr.isNullOrEmpty()) {
                val rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator)
                for (raw in rawSecondaryStorages) {
                    testAndAdd(raw, MountDeviceType.REMOVABLE_SD_CARD)
                }
            }
        }

        // Альтернативный способ через парсинг вывода `mount`
        fun fillDevicesProcess() {
            mountedExternalDevices = ArrayList(3)
            mountedRemovableDevices = ArrayList(3)

            var isStream: InputStream? = null
            var isr: InputStreamReader? = null
            var br: BufferedReader? = null
            var proc: Process? = null
            try {
                val runtime = Runtime.getRuntime()
                proc = runtime.exec("mount")
                try {
                    isStream = proc.inputStream
                    isr = InputStreamReader(isStream)
                    br = BufferedReader(isr)
                    var line: String?
                    while (true) {
                        line = br.readLine() ?: break
                        if (line.contains("secure")) continue
                        if (line.contains("asec")) continue

                        if (line.contains("fat")) {
                            // TF card
                            val columns = line.split(" ")
                            if (columns.size > 1) {
                                testAndAdd(columns[1], MountDeviceType.REMOVABLE_SD_CARD)
                            }
                        } else if (line.contains("fuse")) {
                            // internal (external) storage
                            val columns = line.split(" ")
                            if (columns.size > 1) {
                                testAndAdd(columns[1], MountDeviceType.EXTERNAL_SD_CARD)
                            }
                        }
                    }
                } finally {
                    try { br?.close() } catch (_: Exception) {}
                    try { isr?.close() } catch (_: Exception) {}
                    try { proc?.destroy() } catch (_: Exception) {}
                    try { isStream?.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}