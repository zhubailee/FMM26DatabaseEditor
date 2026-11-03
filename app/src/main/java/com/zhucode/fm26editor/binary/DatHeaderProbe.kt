package com.zhucode.fm26editor.binary

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Non-destructive diagnostic probe. Candidate integers are observations only;
 * they must never be treated as record counts without a verified schema.
 */
object DatHeaderProbe {
    data class Result(
        val fileSize: Long,
        val firstBytesHex: String,
        val littleEndianInt32: Map<Int, Int?>
    )

    fun inspect(input: InputStream, fileSize: Long): Result {
        require(fileSize >= 0) { "Ukuran file tidak valid." }
        val header = input.use { stream ->
            val buffer = ByteArray(32)
            var used = 0
            while (used < buffer.size) {
                val read = stream.read(buffer, used, buffer.size - used)
                if (read < 0) break
                used += read
            }
            buffer.copyOf(used)
        }

        val candidates = (0..12 step 4).associateWith { offset ->
            if (offset + 4 <= header.size) {
                ByteBuffer.wrap(header, offset, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
            } else null
        }
        val hex = header.joinToString(" ") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
        return Result(fileSize, hex, candidates)
    }
}
