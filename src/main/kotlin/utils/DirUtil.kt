package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DirUtil {
    companion object {
        fun copyDirectory(sourceDirectory: File, destinationDirectory: File) {
            if (!destinationDirectory.exists()) {
                destinationDirectory.mkdir()
            }
            for (f in sourceDirectory.list()) {
                copyDirectoryCompatibilityMode(File(sourceDirectory, f), File(destinationDirectory, f))
            }
        }

        private fun copyDirectoryCompatibilityMode(source: File, destination: File) {
            if (source.isDirectory) {
                copyDirectory(source, destination)
            } else {
                copyFile(source, destination)
            }
        }

        private fun copyFile(sourceFile: File, destinationFile: File) {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destinationFile).use { out ->
                    val buf = ByteArray(1024)
                    var length: Int
                    while (input.read(buf).also { length = it } > 0) {
                        out.write(buf, 0, length)
                    }
                }
            }
        }
    }
}