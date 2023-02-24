package com.example.lohmegalog

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class DeviceActivity : AppCompatActivity() {
    var bbc: BlueBerryBluetoothClient? = null
    var batteryTextView: TextView? = null
    var blinkButton: ImageButton? = null
    var rssiTextView: TextView? = null
    var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        setupUI()
        bbc = BlueBerryBluetoothClient(this, bbcCallback)
        connectToDevice()
    }

    fun setupUI() {
        progressBar = findViewById<ProgressBar>(R.id.progress)
        batteryTextView = findViewById<TextView>(R.id.battery_text_view)
        blinkButton = findViewById<ImageButton>(R.id.blink_button)
        rssiTextView = findViewById<TextView>(R.id.rssi)
        setSupportActionBar(findViewById(R.id.device_toolbar))
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        blinkButton?.setOnClickListener {
            blinkDevice()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        disconnectDevice()
        return true
    }

    private fun connectToDevice() {
        progressBar?.visibility = View.VISIBLE
        val deviceAddress = intent.getStringExtra("address")
        if (deviceAddress == null) {}//TODO
        bbc?.openConnection(deviceAddress!!)
    }

    private fun disconnectDevice() {
        bbc?.closeConnection()
    }

    private fun blinkDevice() {
        bbc?.blinkDevice()
    }

    private val bbcCallback = object : BlueBerryBluetoothClientCallback() {
        override fun onConnect() {
            runOnUiThread {
                progressBar?.visibility = View.GONE
            }
        }

        override fun onReceivedBattery(batteryLevel: Int) {
            runOnUiThread {
                batteryTextView?.text = batteryLevel.toString() + "%"
            }
        }

        override fun onReceivedRssi(rssi: Int) {
            runOnUiThread{
                rssiTextView?.text = rssi.toString() + " db"
            }
        }
    }
}