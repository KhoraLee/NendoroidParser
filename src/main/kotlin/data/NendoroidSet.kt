package data

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Paths

data class NendoroidSet(
    val setName: String,
    val list: MutableList<String>
) {
    companion object {
        private const val basePath = "D:\\NendoroidDB\\Nendoroid\\Set"
        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun readSet(serial: Int): NendoroidSet? {
            val file = findSetFile(serial)
            return if (file.exists()) {
                val fr = FileReader(file)
                gson.fromJson(fr, NendoroidSet::class.java)
            } else {
                null
            }
        }

        fun writeSet(set: NendoroidSet, serial: Int) {
            val file = findSetFile(serial)
            val fw = FileWriter(file)
            gson.toJson(set, fw)
            fw.flush()
            fw.close()
        }

        private fun findSetFile(serial: Int): File {
            val path = Paths.get(basePath)
            val number = String.format("%03d", serial)
            return File(path.toFile(), "Set$number.json")
        }

    }

    override fun toString(): String {
        return "$setName : $list"
    }
}