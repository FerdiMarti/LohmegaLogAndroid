package com.example.lohmegalog.bbBluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.lohmegalog.CCC_DESCRIPTOR_UUID
import com.example.lohmegalog.CMD_OPCODE
import com.example.lohmegalog.UUIDS
import java.util.*

//TODO When functionality is extended, the use of a message queue becomes necessary. Otherwise, reads/ writes can get lost.
@SuppressLint("MissingPermission")
class BlueBerryBluetoothClient constructor(
    private val context: Context,
    private val bbcCallback: BlueBerryBluetoothClientCallback
) {
    companion object {
        const val GATT_MAX_MTU_SIZE = 517
        const val CONNECTION_TIMEOUT_AFTER: Long = 20000
        const val TAG = "BlueBerryClient"
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null
    private var deviceCharacteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()
    private var bbDeserializer = BlueBerryDeserializer()
    private var connectionTimeoutHandler = Handler(Looper.getMainLooper())

    fun openConnection(address: String) {
        if (!bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Connection Failed. Bluetooth is not enabled.")
        }
        closeConnection()
        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            bluetoothGatt = device?.connectGatt(context, false, bluetoothGattCallback)
            checkForConnectionTimeout()
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Device not found with provided address. Unable to connect.")
            throw exception
        }
    }

    private fun checkForConnectionTimeout() {
        connectionTimeoutHandler.postDelayed({
            closeConnection()
            bbcCallback.onConnectTimeout()
        }, CONNECTION_TIMEOUT_AFTER)
    }

    fun closeConnection() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
            deviceCharacteristics = HashMap()
            bbDeserializer = BlueBerryDeserializer()
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionTimeoutHandler.removeCallbacksAndMessages(null)
                Log.d(TAG, "Connected to Device")
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from Device")
                bbcCallback.onDisconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.requestMtu(GATT_MAX_MTU_SIZE)
            } else {
                Log.d(TAG, "Receiving GATT Services Failed")
                bbcCallback.onConnectionError()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Changed MTU")
            } else {
                Log.d(TAG, "onMTUChange received: $status")
            }
            bbcCallback.onConnect()
            storeGattServices(gatt?.services)
            readDeviceBattery()
            readDeviceRssi()
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val uuid = characteristic.uuid.toString()
            if (uuid == UUIDS.C_SENSORS_RTD) {
                val entry = bbDeserializer.processData(characteristic.value)
                if (entry != null) {
                    bbcCallback.onReceivedRealTimeData(true, entry)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristics Write Success")
            } else {
                Log.d(TAG, "Characteristics Write Failed")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor Write Success")
            } else {
                Log.d(TAG, "Descriptor Write Success")
            }
        }
    }

    private fun storeGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        val characteristics: HashMap<String, BluetoothGattCharacteristic> = HashMap()

        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                val cUuid = gattCharacteristic.uuid.toString()
                characteristics[cUuid] = gattCharacteristic
            }
        }
        this.deviceCharacteristics = characteristics
    }

    fun blinkDevice() {
        val chara = deviceCharacteristics[UUIDS.C_CMD_TX]
        if (chara == null) {
            throw IllegalStateException("Characteristic does not exist")
        }
        chara.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val data = ByteArray(1)
        data[0] = CMD_OPCODE.BLINK_LED.toByte()
        chara.value = data
        writeCharacteristic(chara)
    }

    fun readDeviceBattery() {
        val chara = deviceCharacteristics[UUIDS.C_BATTERY_LEVEL]
        if (chara == null) {
            throw IllegalStateException("Characteristic does not exist")
        }
        readCharacteristic(chara)
    }

    fun readDeviceRssi() {
        bluetoothGatt?.readRemoteRssi() ?: run {
            throw IllegalStateException("Gatt not Initialized")
        }
    }

    fun subscribeToRTD() {
        val chara = deviceCharacteristics[UUIDS.C_SENSORS_RTD]
        if (chara == null) {
            throw IllegalStateException("Characteristic does not exist")
        }
        enableNotifications(chara)
    }

    fun unsubscribeFromRTD() {
        val chara = deviceCharacteristics[UUIDS.C_SENSORS_RTD]
        if (chara == null) {
            throw IllegalStateException("Characteristic does not exist")
        }
        disableNotifications(chara)
    }

    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic) ?: run {
            throw IllegalStateException("Gatt not Initialized")
        }
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.writeCharacteristic(characteristic) ?: run {
            throw IllegalStateException("Gatt not Initialized")
        }
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: throw IllegalStateException("Gatt not Initialized")
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e(
                    TAG,
                    "${characteristic.uuid} doesn't support notifications/indications"
                )
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e(
                    TAG,
                    "setCharacteristicNotification failed for ${characteristic.uuid}"
                )
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(
            TAG,
            "${characteristic.uuid} doesn't contain the CCC descriptor!"
        )
    }

    private fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e(
                TAG,
                "${characteristic.uuid} doesn't support indications/notifications"
            )
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e(
                    TAG,
                    "setCharacteristicNotification failed for ${characteristic.uuid}"
                )
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e(
            TAG,
            "${characteristic.uuid} doesn't contain the CCC descriptor!"
        )
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i(
                TAG,
                "No service and characteristic available, call discoverServices() first?"
            )
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) {
                it.uuid.toString() + ": " + it.isIndicatable() + ", " + it.isNotifiable() + "\n\tDescriptors:" + it.descriptors.joinToString(
                    separator = "\n|--",
                    prefix = "|--"
                ) { de -> de.uuid.toString() }
            }
            Log.i(
                TAG,
                "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }
}