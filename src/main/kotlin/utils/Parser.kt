package utils

import basePath
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.notExists

object Parser {
    fun createBaseDir() {
        val ranges = (0..20).toList()
        ranges.forEach {
            val folderName = String.format("%04d-%04d", it * 100, (it + 1) * 100 - 1)
            val path = Paths.get(basePath, folderName)
            if (path.notExists()) Files.createDirectories(path)
        }
        val path = Paths.get(basePath, "Set")
        if (path.notExists()) Files.createDirectories(path)
    }

    fun copyPreJSON() {
        val preDir = File("pre-Nendoroid")
        DirUtil.copyDirectory(preDir, File(basePath))
    }

}
