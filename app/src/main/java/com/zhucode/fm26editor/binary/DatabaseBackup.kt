package com.zhucode.fm26editor.binary

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException

/**
 * Copies a selected SAF directory into a separate destination directory.
 * The caller must ensure source and destination are not the same directory
 * and that destination is not nested inside source.
 */
object DatabaseBackup {
    data class Result(val filesCopied: Int, val bytesCopied: Long)

    @Throws(IOException::class)
    fun copyFolder(
        source: DocumentFile,
        destination: DocumentFile,
        resolver: ContentResolver
    ): Result {
        require(source.isDirectory) { "Sumber bukan folder." }
        require(destination.isDirectory) { "Tujuan bukan folder." }
        require(source.uri != destination.uri) { "Folder sumber dan tujuan tidak boleh sama." }

        var count = 0
        var totalBytes = 0L

        fun copyDirectory(from: DocumentFile, to: DocumentFile) {
            for (child in from.listFiles()) {
                val name = child.name ?: throw IOException("Ada file tanpa nama.")
                if (child.isDirectory) {
                    val targetDir = to.createDirectory(name)
                        ?: throw IOException("Gagal membuat folder: $name")
                    copyDirectory(child, targetDir)
                } else if (child.isFile) {
                    val mime = child.type ?: "application/octet-stream"
                    val target = to.createFile(mime, name)
                        ?: throw IOException("Gagal membuat file: $name")
                    val input = resolver.openInputStream(child.uri)
                        ?: throw IOException("Gagal membaca file: $name")
                    val output = resolver.openOutputStream(target.uri, "w")
                        ?: throw IOException("Gagal menulis file: $name")
                    input.use { sourceStream ->
                        output.use { targetStream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = sourceStream.read(buffer)
                                if (read < 0) break
                                targetStream.write(buffer, 0, read)
                                totalBytes += read
                            }
                        }
                    }
                    count++
                }
            }
        }

        copyDirectory(source, destination)
        return Result(count, totalBytes)
    }
}
