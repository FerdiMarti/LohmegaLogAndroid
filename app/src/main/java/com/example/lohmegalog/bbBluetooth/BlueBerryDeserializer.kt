package com.example.lohmegalog.bbBluetooth

import android.util.Log
import com.example.lohmegalog.protobuf.BbLogEntry
import java.io.EOFException

class BlueBerryDeserializer {
    companion object {
        const val TAG = "BlueBerryDeserializer"
    }

    private var buffer: ByteArray = ByteArray(0)

    private fun addToBuffer(data: ByteArray) {
        buffer += data
    }

    private fun getSize(): Int {
        return buffer.size
    }

    private fun getNextEntry(): BbLogEntry.bb_log_entry {
        val size = getSize()
        if (size < 1) {
            throw EOFException("No Data")
        }
        val packetSize = buffer[0].toInt()
        if (packetSize <= size + 1) {
            val packet = buffer.copyOfRange(1, packetSize + 1)
            val entry = BbLogEntry.bb_log_entry.parseFrom(packet)
            buffer = buffer.copyOfRange(packetSize + 1, size)
            return entry
        } else {
            throw EOFException("Not Enough Data")
        }
    }

    fun processData(data: ByteArray): BbLogEntry.bb_log_entry? {
        try {
            addToBuffer(data)
            return getNextEntry()
        } catch (e: EOFException) {
            return null
        } catch (e: java.lang.Exception) {
            this.flushBuffer() //flush because content is flawed; only makes sense for real time data
            Log.e(TAG, "Error while deserializing: " + e.message)
            return null
        }
    }

    fun flushBuffer() {
        this.buffer = ByteArray(0)
    }
}