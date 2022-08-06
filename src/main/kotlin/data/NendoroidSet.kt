package data

import basePath
import java.io.File
import java.nio.file.Paths

data class NendoroidSet(
    override var serial: String = "",
    val setName: String = "",
    val list: MutableList<String> = mutableListOf()
): DataWritable {

    override fun find(): File {
        val path = Paths.get(basePath, "Set")
        val number = String.format("%03d", serial.toIntOrNull() ?: -1)
        return File(path.toFile(), "Set$number.json")
    }

    override fun toString(): String {
        return "$setName : $list"
    }
}