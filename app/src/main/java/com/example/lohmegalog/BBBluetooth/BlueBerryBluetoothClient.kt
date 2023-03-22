package com.example.lohmegalog.BBBluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.example.lohmegalog.CCC_DESCRIPTOR_UUID
import com.example.lohmegalog.CMD_OPCODE
import com.example.lohmegalog.UUIDS
import java.util.*
import kotlin.collections.HashMap

@SuppressLint("MissingPermission")
class BlueBerryBluetoothClient constructor(private val context: Context, private val bbcCallback: BlueBerryBluetoothClientCallback) {
    companion object {
        const val GATT_MAX_MTU_SIZE = 517
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()
    private var bbDeserializer = BlueBerryDeserializer()

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
            characteristics = HashMap()
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("CONNECTION", "CONNECTED")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("CONNECTION", "DISCONNECTED")
                bbcCallback.onDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATT_SUCCESS", "Got Services")
                gatt?.requestMtu(GATT_MAX_MTU_SIZE)
            } else {
                Log.d("GATT", "onServicesDiscovered received: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATT", "Changed MTU")
            } else {
                Log.d("GATT", "onMTUChange received: $status")
            }
            bbcCallback.onConnect()
            getGattServices(gatt?.services)
            readDeviceBattery()
            readDeviceRssi()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val uuid = characteristic.uuid.toString()
            if (uuid == UUIDS.C_BATTERY_LEVEL) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val level = characteristic.value.first().toInt()
                    bbcCallback.onReceivedBattery(true, level)
                } else {
                    bbcCallback.onReceivedBattery(false, null)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bbcCallback.onReceivedRssi(true, rssi)
            } else {
                bbcCallback.onReceivedRssi(false, null)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val uuid = characteristic.uuid.toString()
            if (uuid == UUIDS.C_SENSORS_RTD) {
                val entry = bbDeserializer.processData(characteristic.value)
                if (entry != null) {
                    bbcCallback.onReceivedRealTimeData(true, entry)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("WRITE", "WRITE SUCCESS")
            } else {
                Log.d("WRITE", "WRITE FAILED")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("DESCRIPTOR_WRITE", "WRITE SUCCESS")
            } else {
                Log.d("DESCRIPTOR_WRITE", "WRITE FAILED")
            }
        }
    }

    private fun getGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        val characteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()

        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                val cUuid = gattCharacteristic.uuid.toString()
                characteristics[cUuid] = gattCharacteristic
            }
        }
        this.characteristics = characteristics
    }

    fun blinkDevice() {
        val chara = characteristics[UUIDS.C_CMD_TX]
        if (chara == null) {
            //TODO
            return
        }
        chara.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val data = ByteArray(1)
        data[0] = CMD_OPCODE.BLINK_LED.toByte()
        chara.value = data
        writeCharacteristic(chara)
    }

    fun readDeviceBattery() {
        val chara = characteristics[UUIDS.C_BATTERY_LEVEL]
        if (chara == null) {
            //TODO
            return
        }
        readCharacteristic(chara)
    }

    fun readDeviceRssi() {
        bluetoothGatt?.readRemoteRssi() ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun subscribeToRTD() {
        val chara = characteristics[UUIDS.C_SENSORS_RTD]
        if (chara == null) {
            //TODO
            return
        }
        enableNotifications(chara)
    }

    fun unsubscribeFromRTD() {
        val chara = characteristics[UUIDS.C_SENSORS_RTD]
        if (chara == null) {
            //TODO
            return
        }
        disableNotifications(chara)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic) ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.writeCharacteristic(characteristic) ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() + ": " + it.isIndicatable() + ", " + it.isNotifiable() + "\n\tDescriptors:" + it.descriptors.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) {de -> de.uuid.toString()}
            }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }
}