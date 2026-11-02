package com.zhucode.fm26editor.binary

import java.io.Closeable
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bounded little-endian reader for FMM DAT investigation and parsing.
 * It deliberately does not assume a record schema; callers must provide
 * offsets and field definitions verified against the target database version.
 */
class DatBinaryReader(input: InputStream) : Closeable {
    private val data = input.readBytes()
    private var position = 0

    val size: Int get() = data.size
    val offset: Int get() = position
    val remaining: Int get() = data.size - position

    fun seek(newOffset: Int) {
        require(newOffset in 0..data.size) { "Offset di luar file: $newOffset" }
        position = newOffset
    }

    fun skip(byteCount: Int) {
        require(byteCount >= 0) { "Jumlah byte tidak boleh negatif" }
        requireAvailable(byteCount)
        position += byteCount
    }

    fun readByte(): Int {
        requireAvailable(1)
        return data[position++].toInt() and 0xFF
    }

    fun readSignedByte(): Byte = readByte().toByte()

    fun readBoolean(): Boolean = readByte() != 0

    fun readShort(): Short = readNumber(2).short

    fun readUnsignedShort(): Int = readNumber(2).short.toInt() and 0xFFFF

    fun readInt(): Int = readNumber(4).int

    fun readLong(): Long = readNumber(8).long

    fun readFloat(): Float = readNumber(4).float

    fun readDouble(): Double = readNumber(8).double

    fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "Panjang byte tidak boleh negatif" }
        requireAvailable(length)
        return data.copyOfRange(position, position + length).also { position += length }
    }

    /** FMM reference implementations use a 32-bit length prefix for UTF-8 strings. */
    fun readUtf8(maxLength: Int = DEFAULT_MAX_STRING_LENGTH): String {
        val length = readInt()
        require(length in 0..maxLength) { "Panjang string tidak valid: $length" }
        return readBytes(length).toString(Charsets.UTF_8)
    }

    fun peekBytes(length: Int): ByteArray {
        require(length >= 0) { "Panjang byte tidak boleh negatif" }
        requireAvailable(length)
        return data.copyOfRange(position, position + length)
    }

    private fun readNumber(length: Int): ByteBuffer {
        requireAvailable(length)
        return ByteBuffer.wrap(data, position, length).order(ByteOrder.LITTLE_ENDIAN)
            .also { position += length }
    }

    private fun requireAvailable(length: Int) {
        if (length > remaining) throw EOFException(
            "DAT terpotong pada offset $position: perlu $length byte, tersisa $remaining"
        )
    }

    override fun close() = Unit

    companion object {
        const val DEFAULT_MAX_STRING_LENGTH = 1_048_576
    }
}
