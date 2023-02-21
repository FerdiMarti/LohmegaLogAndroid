package com.example.lohmegalog

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat


@SuppressLint("MissingPermission")
class BlueBerryBluetooth private constructor(private var context: Context) {

    companion object {
        @Volatile
        private lateinit var instance: BlueBerryBluetooth

        fun getInstance(context: Context): BlueBerryBluetooth {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = BlueBerryBluetooth(context)
                } else {
                    instance.setContext(context)
                }
                return instance
            }
        }
    }

    private fun setContext(context: Context) {
        this.context = context
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var mScanning: Boolean = false

    private var bluetoothGatt: BluetoothGatt? = null

    fun openConnection(device: BluetoothDevice) {
        if (bluetoothGatt != null) {
            closeConnection(device)
        }
        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
    }

    fun closeConnection(device: BluetoothDevice) {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("CONNECTION", "CONNECTED")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("CONNECTION", "DISCONNECTED")
            }
        }
    }

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
                val result = ScanResultData(id, result, result.device)
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