package data

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Paths
import java.util.*

data class Nendoroid(
    var num: String,
    var name: MutableMap<Locale, String> = mutableMapOf(),
    var series: MutableMap<Locale, String> = mutableMapOf(),
    var gsc_productNum: Int = 0,
    var price: Int = -1,
    var release_date: MutableSet<String> = mutableSetOf(),
    var image: String = "",
    var gender: Gender? = null,
    var set: Int? = null
) {
    companion object {
        private const val basePath = "D:\\NendoroidDB\\Nendoroid"
        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun readNendoroid(number: String): Nendoroid? {
            val file = findNendoroidFile(number)
            return if (file != null && file.exists()) {
                val fr = FileReader(file)
                gson.fromJson(fr, Nendoroid::class.java)
            } else {
                null
            }
        }

        fun writeNendoroid(nendoroid: Nendoroid) {
            val file = findNendoroidFile(nendoroid.num) ?: return
            val fw = FileWriter(file)
            gson.toJson(nendoroid, fw)
            fw.flush()
            fw.close()
        }

        private fun findNendoroidFile(number: String): File? {
            val range = (number.replace("\\D".toRegex(), "").toIntOrNull() ?: return null) / 100
            val folderName = String.format("%04d-%04d", range * 100, (range + 1) * 100 - 1)
            val path = Paths.get(basePath, folderName)
            return File(path.toFile(), "$number.json")
        }

    }

    override fun toString(): String {
        return """
            번호 : $num
            이름 : $name
            시리즈 : $series
            가격 : $price
            발매일 : $release_date
            성별 : $gender
            GSC 상품 번호 : $gsc_productNum
        """.trimIndent()
    }

    fun merge(nendoroid: Nendoroid) {
        if (num != nendoroid.num) return
        name.putAll(nendoroid.name)
        series.putAll(nendoroid.series)
        if (price == -1) price = nendoroid.price
        if (!nendoroid.release_date.isEmpty()) release_date = nendoroid.release_date
        if (gender == null) gender = nendoroid.gender
    }

    fun addSetInfo(set: Int) {
        if (this.set != null) return
        this.set = set
    }

    fun writeOnDisk() {
        writeNendoroid(this)
    }
}
