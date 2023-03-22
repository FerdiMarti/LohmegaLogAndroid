package com.example.lohmegalog.BBBluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.lohmegalog.ui.ScanResultData


@SuppressLint("MissingPermission")
class BlueBerryBluetoothDiscoverer constructor(private var context: Activity) {

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var mScanning: Boolean = false

    fun scanLe(enable: Boolean, stop: () -> Unit, resultCallback: ((result: ScanResultData) -> Unit)?) {
        promptEnableBluetooth() //TODO test, what if not enabled?
        if (bluetoothAdapter.isEnabled) {
            scanLeDevice(enable, stop, resultCallback) //make sure scan function won't be called several times
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(context, enableBtIntent, 1, null)
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
            val filters = listOf(ScanFilter.Builder().setDeviceName("BlueBerry").build())
            val settings = ScanSettings.Builder().build()
            bluetoothAdapter?.bluetoothLeScanner?.startScan(filters, settings, mLeScanCallback)
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
                val result = ScanResultData(result.device.address)
                if (scanResultCallback != null) {
                    scanResultCallback!!(result)
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