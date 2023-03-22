package com.example.lohmegalog.BBBluetooth

import android.util.Log
import com.example.lohmegalog.protobuf.BbLogEntry
import java.io.EOFException

class BlueBerryDeserializer {
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
        addToBuffer(data)
        try {
            val entry = getNextEntry()
            return entry
        } catch (e: EOFException) {
            Log.d("Deserialize", e.message!!)
        } catch (e: java.lang.Exception) {
            this.flush()
            Log.d("Deserialize", e.message!!)
        }
        return null
    }

    fun flush() {
        this.buffer = ByteArray(0)
    }
}