package com.zhucode.fm26editor.binary

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Little-endian primitive writer. Schema-specific serializers belong in versioned parsers. */
class DatBinaryWriter(private val output: OutputStream) {
    fun writeByte(value: Int) {
        require(value in 0..255)
        output.write(value)
    }

    fun writeBoolean(value: Boolean) = writeByte(if (value) 1 else 0)
    fun writeShort(value: Int) = writeBuffer(2).putShort(value.toShort()).flushTo(output)
    fun writeInt(value: Int) = writeBuffer(4).putInt(value).flushTo(output)
    fun writeLong(value: Long) = writeBuffer(8).putLong(value).flushTo(output)
    fun writeFloat(value: Float) = writeBuffer(4).putFloat(value).flushTo(output)
    fun writeDouble(value: Double) = writeBuffer(8).putDouble(value).flushTo(output)

    fun writeBytes(value: ByteArray) = output.write(value)

    /** Writes UTF-8 bytes preceded by a 32-bit byte length. */
    fun writeUtf8(value: String, maxLength: Int = DatBinaryReader.DEFAULT_MAX_STRING_LENGTH) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= maxLength) { "String terlalu panjang: ${bytes.size} byte" }
        writeInt(bytes.size)
        writeBytes(bytes)
    }

    private fun writeBuffer(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    private fun ByteBuffer.flushTo(target: OutputStream) {
        target.write(array())
    }
}
