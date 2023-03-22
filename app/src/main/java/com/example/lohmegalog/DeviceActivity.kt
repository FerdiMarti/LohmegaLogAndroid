package com.example.lohmegalog

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.lohmegalog.protobuf.BbLogEntry
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.pow


class DeviceActivity : AppCompatActivity() {
    var bbc: BlueBerryBluetoothClient? = null
    var connectedImage: ImageView? = null
    var connectedTV: TextView? = null
    var rssiImage: ImageView? = null
    var rssiTextView: TextView? = null
    var batteryImage: ImageView? = null
    var batteryTextView: TextView? = null
    var blinkButton: ImageButton? = null
    var blinkTV: TextView? = null
    var progressBar: ProgressBar? = null

    //RTD Views
    var rtdView: CardView? = null
    var rtdSwitch: SwitchMaterial? = null
    var accelerationView: LinearLayout? = null
    var battVolView: LinearLayout? = null
    var illuminanceView: LinearLayout? = null
    var magnView: LinearLayout? = null
    var pressureView: LinearLayout? = null
    var humidityView: LinearLayout? = null
    var rotView: LinearLayout? = null
    var tempView: LinearLayout? = null
    var uvView: LinearLayout? = null
    var accelerationTV: TextView? = null
    var battTV: TextView? = null
    var illuminanceTV: TextView? = null
    var magnTV: TextView? = null
    var pressureTV: TextView? = null
    var humidityTV: TextView? = null
    var rotTV: TextView? = null
    var tempTV: TextView? = null
    var uvTV: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        setupUI()
        bbc = BlueBerryBluetoothClient(this, bbcCallback)
        connectToDevice()
    }

    fun setupUI() {
        progressBar = findViewById<ProgressBar>(R.id.progress)
        connectedImage = findViewById<ImageView>(R.id.connected_image)
        connectedTV = findViewById<TextView>(R.id.connected_text_view)
        rssiImage = findViewById<ImageView>(R.id.rssi_image)
        rssiTextView = findViewById<TextView>(R.id.rssi_text_view)
        batteryImage = findViewById<ImageView>(R.id.battery_image)
        batteryTextView = findViewById<TextView>(R.id.battery_text_view)
        blinkButton = findViewById<ImageButton>(R.id.blink_button)
        blinkTV = findViewById<TextView>(R.id.blink_text_view)
        setSupportActionBar(findViewById(R.id.device_toolbar))
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)

        blinkButton?.setOnClickListener {
            blinkDevice()
        }

        rtdView = findViewById(R.id.rtd_view)
        setRTDGone()
        rtdSwitch = findViewById(R.id.rtd_switch)
        rtdSwitch?.isChecked = false
        rtdSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                setRTDFieldsVisible()
                subscribeToRTD()
            } else {
                setRTDFieldsGone()
                unsubscribeFromRTD()
            }
        }

        accelerationView = findViewById(R.id.acc_view)
        illuminanceView = findViewById(R.id.illuminance_view)
        battVolView = findViewById(R.id.battvol_view)
        magnView = findViewById(R.id.magn_view)
        pressureView = findViewById(R.id.pressure_view)
        humidityView = findViewById(R.id.humidity_view)
        rotView = findViewById(R.id.rot_view)
        tempView = findViewById(R.id.temp_view)
        uvView = findViewById(R.id.uv_view)
        setRTDFieldsGone()

        accelerationTV = findViewById(R.id.rtd_acc_value)
        illuminanceTV = findViewById(R.id.rtd_illuminance_value)
        battTV = findViewById(R.id.rtd_battvol_value)
        magnTV = findViewById(R.id.rtd_magn_value)
        pressureTV = findViewById(R.id.rtd_pressure_value)
        humidityTV = findViewById(R.id.rtd_humidity_value)
        rotTV = findViewById(R.id.rtd_rot_value)
        tempTV = findViewById(R.id.rtd_temp_value)
        uvTV = findViewById(R.id.rtd_UV_value)
    }

    fun setRTDGone() {
        rtdView?.visibility = View.GONE
    }

    fun setRTDVisible() {
        rtdView?.visibility = View.VISIBLE
    }

    fun setRTDFieldsGone() {
        accelerationView?.visibility = View.GONE
        illuminanceView?.visibility = View.GONE
        battVolView?.visibility = View.GONE
        magnView?.visibility = View.GONE
        pressureView?.visibility = View.GONE
        humidityView?.visibility = View.GONE
        rotView?.visibility = View.GONE
        tempView?.visibility = View.GONE
        uvView?.visibility = View.GONE
    }

    fun setRTDFieldsVisible() {
        accelerationView?.visibility = View.VISIBLE
        illuminanceView?.visibility = View.VISIBLE
        battVolView?.visibility = View.VISIBLE
        magnView?.visibility = View.VISIBLE
        pressureView?.visibility = View.VISIBLE
        humidityView?.visibility = View.VISIBLE
        rotView?.visibility = View.VISIBLE
        tempView?.visibility = View.VISIBLE
        uvView?.visibility = View.VISIBLE
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

    private fun subscribeToRTD() {
        bbc?.subscribeToRTD()
    }

    private fun unsubscribeFromRTD() {
        bbc?.unsubscribeFromRTD()
    }

    private val bbcCallback = object : BlueBerryBluetoothClientCallback() {
        override fun onConnect() {
            runOnUiThread {
                setRTDVisible()
                connectedTV?.text = "Connected"
                connectedImage?.setImageResource(R.drawable.connected)
                blinkTV?.text = "Blink"
                blinkButton?.setImageResource(R.drawable.blink_active)
                progressBar?.visibility = View.GONE
            }
        }

        override fun onDisconnect() {
            runOnUiThread {
                setRTDVisible()
                connectedTV?.text = "Connected"
                connectedImage?.setImageResource(R.drawable.connected)
                blinkTV?.text = "Blink"
                blinkButton?.setImageResource(R.drawable.blink_active)
                progressBar?.visibility = View.GONE
            }
        }

        override fun onReceivedBattery(success: Boolean, batteryLevel: Int?) {
            runOnUiThread {
                batteryImage?.setImageResource(R.drawable.battery_loaded)
                batteryTextView?.text = batteryLevel.toString() + "%"
            }
        }

        override fun onReceivedRssi(success: Boolean, rssi: Int?) {
            runOnUiThread{
                rssiImage?.setImageResource(R.drawable.range_connected)
                rssiTextView?.text = rssi.toString() + " db"
            }
        }

        override fun onReceivedRealTimeData(success: Boolean, data: BbLogEntry.bb_log_entry) {
            if (success) {
                accelerationTV?.text = constructFieldListString(BlueBerryLogEntryFields.ACCELEROMETER, data.accelerometerList)
                battTV?.text = constructFieldString(BlueBerryLogEntryFields.BATVOLT, data.batteryMv)
                illuminanceTV?.text = constructFieldString(BlueBerryLogEntryFields.LUX, data.lux)
                magnTV?.text = constructFieldListString(BlueBerryLogEntryFields.COMPASS, data.compassList)
                pressureTV?.text = constructFieldString(BlueBerryLogEntryFields.PRESSURE, data.pressure)
                humidityTV?.text = constructFieldString(BlueBerryLogEntryFields.HUMIDITY, data.rh)
                rotTV?.text = constructFieldListString(BlueBerryLogEntryFields.GYRO, data.gyroList)
                tempTV?.text = constructFieldString(BlueBerryLogEntryFields.TEMPERATURE, data.temperature)
                uvTV?.text = constructFieldString(BlueBerryLogEntryFields.UVI, data.uvi)
            }
        }
    }

    fun constructFieldListString(field: _BlueBerryLogEntryField, list: List<Int>): String {
        var str = ""
        for (index in list.indices) {
            val value = field.tounit(list[index].toFloat()).round(3)
            str += value.toString()
            str += if (index != list.size - 1) {
                ", "
            } else {
                " "
            }
        }
        str += field.unit
        return str
    }

    fun constructFieldString(field: _BlueBerryLogEntryField, data: Number): String {
        return field.tounit(data.toFloat()).round(3).toString() + " " + field.unit
    }

    fun Float.round(decimals: Int): Float {
        val multiplier = 10.0.pow(decimals)
        return ((this * multiplier).toInt().toFloat() / multiplier).toFloat()
    }
}