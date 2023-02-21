package com.example.lohmegalog

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.util.Log


@SuppressLint("MissingPermission")
class BlueBerryBluetoothDiscoverer constructor(private var context: Context) {

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var mScanning: Boolean = false

    fun scanLe(enable: Boolean, stop: () -> Unit, resultCallback: ((result: ScanResultData) -> Unit)?) {
        if (bluetoothAdapter.isEnabled) {
            scanLeDevice(enable, stop, resultCallback) //make sure scan function won't be called several times
        }
    }

    var scanResultCallback: ((result: ScanResultData) -> Unit)? = null
    private fun scanLeDevice(enable: Boolean, stop: () -> Unit, resultCallback: ((result: ScanResultData) -> Unit)?) {
        scanResultCallback = resultCallback
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            Handler().postDelayed({
                mScanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
                scanResultCallback = null
                stop()
            }, 10000)
            mScanning = true
            bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
        } else {
            mScanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
            scanResultCallback = null
            stop()
        }
    }


    private var mLeScanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (result == null) {
                    return
                }
                val mac = result.device.toString()
                val name = result.device.name
                val id = mac + ": " + name
                val result = ScanResultData(id, result, result.device.address)
                if (name == "BlueBerry") {
                    if (scanResultCallback != null) {
                        scanResultCallback!!(result)
                    }
                }
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d("ERROR", errorCode.toString())
            }
        }
}