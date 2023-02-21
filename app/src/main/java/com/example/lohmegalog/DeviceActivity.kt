package com.example.lohmegalog

import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class DeviceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        val deviceAddress = intent.getStringExtra("address")
        if (deviceAddress == null) {}//TODO
        val blueBerryBluetoothClient = BlueBerryBluetoothClient(this)
        blueBerryBluetoothClient.openConnection(deviceAddress!!)
    }
}