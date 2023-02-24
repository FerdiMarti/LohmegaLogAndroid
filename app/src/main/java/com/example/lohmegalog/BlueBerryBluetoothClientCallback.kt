package com.example.lohmegalog

import android.bluetooth.BluetoothGatt

abstract class BlueBerryBluetoothClientCallback {

    open fun onConnect() {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedConfiguration() {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedRealTimeData() {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedRssi(rssi: Int) {
        throw java.lang.RuntimeException("Stub!")
    }

    open fun onReceivedBattery(batteryLevel: Int) {
        throw java.lang.RuntimeException("Stub!")
    }
}
