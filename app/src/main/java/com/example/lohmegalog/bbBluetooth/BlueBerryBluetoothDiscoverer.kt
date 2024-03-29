package com.example.lohmegalog.bbBluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.lohmegalog.ui.ScanResultData

/**
 * Handles scanning for BlueBerry devices.
 * @property context - needed to initialize BluetoothAdapter
 */
@SuppressLint("MissingPermission") //complains about missing ble permissions, but works anyway
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
    private var stopScanHandler: Handler = Handler(Looper.getMainLooper())

    /**
     * start/ stop scanning for devices
     *
     * @param enable whether to start or stop scanning
     * @param stop callback when scan is stopped
     * @param resultCallback callback when a device was discovered
     */
    fun scanLe(
        enable: Boolean,
        stop: () -> Unit,
        resultCallback: ((result: ScanResultData) -> Unit)?
    ) {
        if (enable && resultCallback == null) {
            throw IllegalArgumentException("Please specify a result callback")
        }
        if (bluetoothAdapter.isEnabled) {
            scanLeDevice(enable, stop, resultCallback)
        } else {
            throw IllegalStateException("Bluetooth is not enabled.")
        }
    }

    var scanResultCallback: ((result: ScanResultData) -> Unit)? = null //object that holds the UI callback
    /**
     * start/ stop scanning for devices
     *
     * @param enable whether to start or stop scanning
     * @param stop callback when scan is stopped
     * @param resultCallback callback when a device was discovered
     */
    private fun scanLeDevice(
        enable: Boolean,
        stop: () -> Unit,
        resultCallback: ((result: ScanResultData) -> Unit)?
    ) {
        scanResultCallback = resultCallback
        if (enable) {
            if (scanning) return
            // Stops scanning after a pre-defined scan period.
            stopScanHandler.postDelayed({
                scanLeDevice(false, stop, null)
            }, SCAN_PERIOD)
            scanning = true
            //Filter for BlueBerry Devices
            val filters = listOf(ScanFilter.Builder().setDeviceName(BLUEBERRY_DEVICE_NAME).build())
            val settings = ScanSettings.Builder().build()
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                filters,
                settings,
                bluetoothAdapterScanCallback
            )
        } else {
            stopScanHandler.removeCallbacksAndMessages(null) //prevent automatic stopping from handler (in case the user starts scan again immediately)
            scanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(bluetoothAdapterScanCallback)
            scanResultCallback = null
            stop()
        }
    }

    /**
     * Callback for BluetoothAdapter
     */
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