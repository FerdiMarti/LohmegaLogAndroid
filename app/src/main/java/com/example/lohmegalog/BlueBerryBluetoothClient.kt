package com.example.lohmegalog

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*

@SuppressLint("MissingPermission")
class BlueBerryBluetoothClient constructor(private var context: Context) {
    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null

    fun openConnection(address: String) {
        closeConnection()
        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            bluetoothGatt = device?.connectGatt(context, false, bluetoothGattCallback)
        } catch (exception: IllegalArgumentException) {
            Log.w("TAG", "Device not found with provided address.  Unable to connect.")
        }
    }

    fun closeConnection() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("CONNECTION", "CONNECTED")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("CONNECTION", "DISCONNECTED")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATTSUCCESS", "Got Services")
                displayGattServices(gatt?.services)
            } else {
                Log.d("GATT", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("VALUE", characteristic.getStringValue(0))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d("VALUE", characteristic.uuid.toString())
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return

        val gattServiceData: MutableList<HashMap<String, BluetoothGattService>> = mutableListOf()
        val gattCharacteristicData: MutableList<ArrayList<HashMap<String, String>>> = mutableListOf()

        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val currentServiceData = HashMap<String, BluetoothGattService>()
            val s_uuid = gattService.uuid.toString()
            currentServiceData[s_uuid] = gattService
            gattServiceData += currentServiceData

            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
            val gattCharacteristics = gattService.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, BluetoothGattCharacteristic> = hashMapOf()
                val c_uuid = gattCharacteristic.uuid.toString()
                currentCharaData[c_uuid] = gattCharacteristic
                Log.d("Characteristic", gattCharacteristic.uuid.toString())
                if (gattCharacteristic.uuid.toString() == Defs.C_MANUFACTURER) {
                    readCharacteristic(gattCharacteristic)
                    Log.d("MATCH", gattCharacteristic.service.uuid.toString())
                }
            }
            gattCharacteristicData += gattCharacteristicGroupData
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        bluetoothGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)
        } ?: run {
            Log.w("Tag", "BluetoothGatt not initialized")
        }
    }
}