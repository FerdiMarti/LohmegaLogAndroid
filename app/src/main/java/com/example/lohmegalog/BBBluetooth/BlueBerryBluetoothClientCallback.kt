package com.example.lohmegalog.BBBluetooth

import com.example.lohmegalog.protobuf.BbLogEntry

abstract class BlueBerryBluetoothClientCallback {

    open fun onConnect() {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onDisconnect() {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedRealTimeData(success: Boolean, data: BbLogEntry.bb_log_entry) {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedRssi(success: Boolean, rssi: Int?) {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedBattery(success: Boolean, batteryLevel: Int?) {
        throw java.lang.RuntimeException("Stub!")
    }
}
