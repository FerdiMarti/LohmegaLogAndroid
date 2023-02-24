package com.example.lohmegalog

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*
import kotlin.collections.HashMap

@SuppressLint("MissingPermission")
class BlueBerryBluetoothClient constructor(private val context: Context, private val bbcCallback: BlueBerryBluetoothClientCallback) {
    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()

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
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATTSUCCESS", "Got Services")
                val GATT_MAX_MTU_SIZE = 517
                gatt?.requestMtu(GATT_MAX_MTU_SIZE)
            } else {
                Log.d("GATT", "onServicesDiscovered received: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("GATT SUCCESS", "Changed MTU")
            } else {
                Log.d("GATT", "onMTUChange received: $status")
            }
            bbcCallback.onConnect()
            gatt?.printGattTable()
            displayGattServices(gatt?.services)
            readDeviceBattery()
            readDeviceRssi()
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("VALUE", characteristic.getStringValue(0))
                if (characteristic.uuid.toString() == UUIDS.C_BATTERY_LEVEL) {
                    val level = characteristic.value.first().toInt()
                    Log.d("BATTERY LEVEL", level.toString())
                    bbcCallback.onReceivedBattery(level)
                }
            } else {
                Log.d("VALUE", "FAILED")
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bbcCallback.onReceivedRssi(rssi)
            } else {
                Log.d("RSSI", "FAILED")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Log.d("VALUE", characteristic.value.toHexString())
            Log.d("VALUE", "DONE")
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
                Log.d("DESCRIPTORWRITE", "WRITE SUCCESS")
            } else {
                Log.d("DESCRIPTORWRITE", "WRITE FAILED")
            }
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        val characteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()

        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                val c_uuid = gattCharacteristic.uuid.toString()
                characteristics[c_uuid] = gattCharacteristic
                /*if (gattCharacteristic.uuid.toString() == UUIDS.C_SENSORS_RTD) {
                    enableNotifications(gattCharacteristic)
                }
                if (c_uuid == UUIDS.C_SENSORS_LOG) {
                    readCharacteristic(gattCharacteristic)
                }
                if (c_uuid == UUIDS.C_CMD_TX) {
                    gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val data = ByteArray(1)
                    data.set(0, CMD_OPCODE.BLINK_LED.toByte())
                    gattCharacteristic.value = data
                    writeCharacteristic(gattCharacteristic)
                }
                if (c_uuid == UUIDS.C_CFG_LOG_ENABLE) {
                    gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val data = ByteArray(4)
                    data.set(3, 1)
                    gattCharacteristic.value = data
                    writeCharacteristic(gattCharacteristic)
                }
                if (c_uuid == UUIDS.C_CFG_INTERVAL) {
                    gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    val data = ByteArray(4)
                    data.set(3, 1)
                    gattCharacteristic.value = data
                    writeCharacteristic(gattCharacteristic)
                }*/
            }
        }
        this.characteristics = characteristics
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

    public fun blinkDevice() {
        val chara = characteristics[UUIDS.C_CMD_TX]
        if (chara == null) {
            //TODO
            return
        }
        chara!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
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
        bluetoothGatt?.let { gatt ->
            gatt.readRemoteRssi()
        } ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w("TAG", "BluetoothGatt not initialized")
            return
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.writeCharacteristic(characteristic)
        } ?: run {
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
}