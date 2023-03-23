package com.example.lohmegalog.bbBluetooth

import android.util.Log
import com.example.lohmegalog.protobuf.BbLogEntry
import java.io.EOFException

/**
 * Deserializer for received bytes. Uses Google's ProtocolBuffers to deserialize payload.
 *
 */
class BlueBerryDeserializer {
    companion object {
        const val TAG = "BlueBerryDeserializer"
    }

    //This buffer holds received bytes.
    private var buffer: ByteArray = ByteArray(0)

    private fun addToBuffer(data: ByteArray) {
        buffer += data
    }

    private fun getSize(): Int {
        return buffer.size
    }

    /**
     * returns a full bb_log_entry from the buffer if it contains a complete object. One complete log_entry object comes in multiple payloads.
     * @throws EOFException if there is not enough data in the buffer to parse
     * @return bb_log_entry object
     */
    private fun getNextEntry(): BbLogEntry.bb_log_entry {
        val size = getSize()
        if (size < 1) {
            throw EOFException("No Data")
        }
        val packetSize = buffer[0].toInt() //first byte of payload contains the size of the full object
        if (packetSize <= size + 1) {
            val packet = buffer.copyOfRange(1, packetSize + 1)
            val entry = BbLogEntry.bb_log_entry.parseFrom(packet) //Use Protocol Buffers to parse object
            buffer = buffer.copyOfRange(packetSize + 1, size) //remove used data from buffer
            return entry
        } else {  //not enough data received yet
            throw EOFException("Not Enough Data")
        }
    }

    /**
     * Adds received data to the buffer and returns a bb_log_entry object if possible.
     *
     * @param data bytes to add
     * @return bb_log_entry object
     */
    fun processData(data: ByteArray): BbLogEntry.bb_log_entry? {
        try {
            addToBuffer(data)
            return getNextEntry()
        } catch (e: EOFException) { //not enough data
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