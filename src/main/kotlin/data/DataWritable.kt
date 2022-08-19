package data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter

interface DataWritable {

    companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }

    val num: String

    fun save() {
        val file = find()
        val fw = FileWriter(file)
        gson.toJson(this, fw)
        fw.flush()
        fw.close()
    }

    fun find(): File

}
