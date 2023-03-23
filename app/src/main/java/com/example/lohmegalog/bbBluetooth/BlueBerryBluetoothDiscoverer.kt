package com.example.lohmegalog.bbBluetooth

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
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.lohmegalog.ui.ScanResultData


@SuppressLint("MissingPermission")
class BlueBerryBluetoothDiscoverer constructor(private var context: Activity) {

    companion object {
        const val BLUEBERRY_DEVICE_NAME = "BlueBerry"
        const val SCAN_PERIOD: Long = 10000
        const val TAG = "BlueBerryDiscoverer"
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanning: Boolean = false

    fun scanLe(
        enable: Boolean,
        stop: () -> Unit,
        resultCallback: ((result: ScanResultData) -> Unit)?
    ) {
        if (enable && resultCallback == null) {
            throw Exception("Please specify a result callback")
        }
        if (promptEnableBluetooth()) {
            //if Bluetooth was not enabled stop() and return
            stop()
            return
        }
        if (bluetoothAdapter.isEnabled) {
            scanLeDevice(enable, stop, resultCallback)
        }
    }

    private fun promptEnableBluetooth(): Boolean {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(context, enableBtIntent, 1, null)
            return true
        }
        return false
    }

    var scanResultCallback: ((result: ScanResultData) -> Unit)? = null
    private fun scanLeDevice(
        enable: Boolean,
        stop: () -> Unit,
        resultCallback: ((result: ScanResultData) -> Unit)?
    ) {
        scanResultCallback = resultCallback
        if (enable) {
            if (scanning) return
            // Stops scanning after a pre-defined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                scanLeDevice(false, stop, null)
            }, SCAN_PERIOD)
            scanning = true
            //Filter only for BlueBerry Devices
            val filters = listOf(ScanFilter.Builder().setDeviceName(BLUEBERRY_DEVICE_NAME).build())
            val settings = ScanSettings.Builder().build()
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                filters,
                settings,
                bluetoothAdapterScanCallback
            )
        } else {
            scanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(bluetoothAdapterScanCallback)
            scanResultCallback = null
            stop()
        }
    }

    private var bluetoothAdapterScanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (result == null) {
                    return
                }
                //so far, only mac address is needed from result
                val resultData = ScanResultData(result.device.address)
                if (scanResultCallback != null) {
                    scanResultCallback!!(resultData)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e(TAG, errorCode.toString())
            }
        }
}