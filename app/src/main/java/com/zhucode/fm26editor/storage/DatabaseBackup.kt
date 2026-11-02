package com.zhucode.fm26editor.storage

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class BackupReport(
    val copiedFiles: Int,
    val copiedBytes: Long,
    val sha256ByFile: Map<String, String>
)

/**
 * Copies a selected database tree into a user-selected destination tree.
 * The source is read-only from this class; existing destination files are not overwritten.
 */
class DatabaseBackup(private val resolver: ContentResolver) {

    suspend fun createCopy(sourceTree: Uri, destinationTree: Uri): BackupReport =
        withContext(Dispatchers.IO) {
            require(sourceTree != destinationTree) {
                "Folder sumber dan tujuan tidak boleh sama."
            }

            val source = DocumentFile.fromTreeUri(resolver.contextOrNull(), sourceTree)
            // DocumentFile.fromTreeUri requires a Context; use the overload below instead.
            error("Use createCopy(context, sourceTree, destinationTree)")
        }

    suspend fun createCopy(
        sourceRoot: DocumentFile,
        destinationRoot: DocumentFile
    ): BackupReport = withContext(Dispatchers.IO) {
        require(sourceRoot.uri != destinationRoot.uri) {
            "Folder sumber dan tujuan tidak boleh sama."
        }
        require(sourceRoot.isDirectory && destinationRoot.isDirectory) {
            "Sumber dan tujuan harus berupa folder."
        }

        val hashes = linkedMapOf<String, String>()
        var fileCount = 0
        var totalBytes = 0L

        fun copyDirectory(source: DocumentFile, target: DocumentFile, prefix: String) {
            source.listFiles().forEach { child ->
                val name = child.name ?: return@forEach
                val relative = if (prefix.isEmpty()) name else "$prefix/$name"
                if (child.isDirectory) {
                    val existing = target.findFile(name)
                    require(existing == null) {
                        "Folder tujuan sudah memiliki '$relative'. Pilih folder tujuan kosong."
                    }
                    val newDirectory = target.createDirectory(name)
                        ?: error("Tidak bisa membuat folder '$relative'.")
                    copyDirectory(child, newDirectory, relative)
                } else if (child.isFile) {
                    require(target.findFile(name) == null) {
                        "File tujuan '$relative' sudah ada. Tidak ada file yang ditimpa."
                    }
                    val mime = child.type ?: "application/octet-stream"
                    val output = target.createFile(mime, name)
                        ?: error("Tidak bisa membuat file '$relative'.")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    resolver.openInputStream(child.uri).use { input ->
                        requireNotNull(input) { "Tidak bisa membaca '$relative'." }
                        resolver.openOutputStream(output.uri, "w").use { stream ->
                            requireNotNull(stream) { "Tidak bisa menulis '$relative'." }
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                stream.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                copied += count
                            }
                            stream.flush()
                        }
                    }
                    hashes[relative] = digest.digest().joinToString("") { "%02x".format(it) }
                    fileCount++
                    totalBytes += copied
                }
            }
        }

        copyDirectory(sourceRoot, destinationRoot, "")
        BackupReport(fileCount, totalBytes, hashes)
    }
}
