package extensions

import data.DataWritable
import java.io.FileReader

inline fun <reified T> DataWritable.load(): T? {
    val file = find()
    return if (file.exists()) {
        val fr = FileReader(file)
        DataWritable.gson.fromJson(fr, T::class.java)
    } else {
        null
    }
}
