package com.zhucode.fm26editor.binary

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Fixed-width player.dat codec based on the documented FMMLibrary player record layout.
 * The 8-byte file header is preserved byte-for-byte.
 */
data class PlayerRecord(
    val id: Int,
    val uid: Int,
    val attributes: MutableList<Int>,
    var ca: Int,
    var pa: Int,
    var homeReputation: Int,
    var currentReputation: Int,
    var worldReputation: Int,
    var internationalRetirement: Int,
    var squadNumber: Int,
    var preferredSquadNumber: Int,
    var height: Int,
    var weight: Int
)

data class PlayerDatabase(
    val header: ByteArray,
    val players: MutableList<PlayerRecord>
) {
    fun toByteArray(): ByteArray {
        val bytes = ByteArrayOutputStream()
        val out = LittleEndianOutput(bytes)
        out.write(header)
        out.writeInt(players.size)
        players.forEach { p ->
            out.writeInt(p.id); out.writeInt(p.uid)
            p.attributes.forEach(out::writeByte)
            out.writeShort(p.ca); out.writeShort(p.pa)
            out.writeShort(p.homeReputation)
            out.writeShort(p.currentReputation)
            out.writeShort(p.worldReputation)
            out.writeByte(p.internationalRetirement)
            out.writeShort(0)
            out.writeByte(p.squadNumber); out.writeByte(p.preferredSquadNumber)
            out.writeShort(p.height); out.writeShort(p.weight)
            out.writeInt(0)
        }
        return bytes.toByteArray()
    }

    companion object {
        const val HEADER_SIZE = 8
        const val ATTRIBUTE_COUNT = 51
        const val RECORD_SIZE = 82

        @Throws(IOException::class)
        fun parse(data: ByteArray): PlayerDatabase {
            if (data.size < HEADER_SIZE + 4) throw IOException("File player DAT terlalu pendek.")
            val input = LittleEndianInput(data)
            val header = input.readBytes(HEADER_SIZE)
            val count = input.readInt()
            if (count < 0 || count > 2_000_000) throw IOException("Jumlah record pemain tidak valid: $count")
            val expected = HEADER_SIZE.toLong() + 4L + count.toLong() * RECORD_SIZE
            if (expected != data.size.toLong()) {
                throw IOException("Ukuran player DAT tidak cocok dengan layout record (count=$count, ukuran=${data.size}). File tidak diubah.")
            }
            val players = ArrayList<PlayerRecord>(count)
            repeat(count) {
                val id = input.readInt()
                val uid = input.readInt()
                val attrs = MutableList(ATTRIBUTE_COUNT) { input.readUnsignedByte() }
                val ca = input.readShort()
                val pa = input.readShort()
                val home = input.readShort()
                val current = input.readShort()
                val world = input.readShort()
                val retirement = input.readUnsignedByte()
                input.readShort() // reserved
                val squad = input.readUnsignedByte()
                val preferred = input.readUnsignedByte()
                val height = input.readShort()
                val weight = input.readShort()
                input.readInt() // reserved
                players += PlayerRecord(id, uid, attrs, ca, pa, home, current, world, retirement, squad, preferred, height, weight)
            }
            return PlayerDatabase(header, players)
        }
    }
}

private class LittleEndianInput(data: ByteArray) {
    private val input = DataInputStream(ByteArrayInputStream(data))
    fun readBytes(n: Int) = ByteArray(n).also(input::readFully)
    fun readUnsignedByte() = input.readUnsignedByte()
    fun readShort() = java.lang.Short.reverseBytes(input.readShort()).toInt()
    fun readInt() = Integer.reverseBytes(input.readInt())
}

private class LittleEndianOutput(private val output: ByteArrayOutputStream) {
    fun write(bytes: ByteArray) = output.write(bytes)
    fun writeByte(value: Int) = output.write(value and 0xFF)
    fun writeShort(value: Int) {
        writeByte(value); writeByte(value ushr 8)
    }
    fun writeInt(value: Int) {
        writeByte(value); writeByte(value ushr 8); writeByte(value ushr 16); writeByte(value ushr 24)
    }
}
