package com.example.lohmegalog

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult

data class ScanResultData(val address: String) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if(other !is ScanResultData) return false

        return this.address == other.address
    }
}
