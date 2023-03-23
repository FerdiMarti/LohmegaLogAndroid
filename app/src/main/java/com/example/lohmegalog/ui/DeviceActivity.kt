package com.example.lohmegalog.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.lohmegalog.BlueBerryLogEntryField
import com.example.lohmegalog.BlueBerryLogEntryFields
import com.example.lohmegalog.R
import com.example.lohmegalog.bbBluetooth.BlueBerryBluetoothClient
import com.example.lohmegalog.bbBluetooth.BlueBerryBluetoothClientCallback
import com.example.lohmegalog.protobuf.BbLogEntry
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.pow
import kotlin.math.roundToInt


class DeviceActivity : AppCompatActivity() {
    companion object {
        const val ADDRESS_INTENT_KEY = "device_address"
    }

    //Header UI Elements
    private var bbc: BlueBerryBluetoothClient? = null
    private var connectedImage: ImageView? = null
    private var connectedTextView: TextView? = null
    private var rssiImage: ImageView? = null
    private var rssiTextView: TextView? = null
    private var batteryImage: ImageView? = null
    private var batteryTextView: TextView? = null
    private var blinkImage: ImageView? = null
    private var blinkTextView: TextView? = null
    private var connectProgressBar: ProgressBar? = null

    //RTD Views
    private var rtdProgressBar: ProgressBar? = null
    private var rtdView: CardView? = null
    private var rtdSwitch: SwitchMaterial? = null
    private var accelerationView: LinearLayout? = null
    private var batteryVolView: LinearLayout? = null
    private var illuminanceView: LinearLayout? = null
    private var magnetometerView: LinearLayout? = null
    private var pressureView: LinearLayout? = null
    private var humidityView: LinearLayout? = null
    private var rotView: LinearLayout? = null
    private var tempView: LinearLayout? = null
    private var uvView: LinearLayout? = null
    private var accelerationTextView: TextView? = null
    private var batteryVolTextView: TextView? = null
    private var illuminanceTextView: TextView? = null
    private var magnetometerTextView: TextView? = null
    private var pressureTextView: TextView? = null
    private var humidityTextView: TextView? = null
    private var rotTextView: TextView? = null
    private var tempTextView: TextView? = null
    private var uvTextView: TextView? = null

    private var firstRTD: Boolean = true
    private var statusTimer: Timer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        setupUI()
        bbc = BlueBerryBluetoothClient(this, bbcCallback)
        val deviceAddress = intent.getStringExtra(ADDRESS_INTENT_KEY)
        connectToDevice(deviceAddress)
    }


    private fun setupUI() {
        connectProgressBar = findViewById(R.id.progress_connect)
        connectedImage = findViewById(R.id.connected_image)
        connectedTextView = findViewById(R.id.connected_text_view)
        rssiImage = findViewById(R.id.rssi_image)
        rssiTextView = findViewById(R.id.rssi_text_view)
        batteryImage = findViewById(R.id.battery_image)
        batteryTextView = findViewById(R.id.battery_text_view)
        blinkImage = findViewById(R.id.blink_image)
        blinkTextView = findViewById(R.id.blink_text_view)
        blinkImage?.setOnClickListener { blinkDevice() }

        setSupportActionBar(findViewById(R.id.device_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rtdView = findViewById(R.id.rtd_view)
        rtdProgressBar = findViewById(R.id.progress_rtd)
        rtdSwitch = findViewById(R.id.rtd_switch)
        rtdSwitch?.isChecked = false
        rtdSwitch?.setOnCheckedChangeListener { _, isChecked -> switchChanged(isChecked)}
        accelerationView = findViewById(R.id.acc_view)
        illuminanceView = findViewById(R.id.illuminance_view)
        batteryVolView = findViewById(R.id.battvol_view)
        magnetometerView = findViewById(R.id.magn_view)
        pressureView = findViewById(R.id.pressure_view)
        humidityView = findViewById(R.id.humidity_view)
        rotView = findViewById(R.id.rot_view)
        tempView = findViewById(R.id.temp_view)
        uvView = findViewById(R.id.uv_view)
        setRTDFieldsGone()
        setRTDGone()

        accelerationTextView = findViewById(R.id.rtd_acc_value)
        illuminanceTextView = findViewById(R.id.rtd_illuminance_value)
        batteryVolTextView = findViewById(R.id.rtd_battvol_value)
        magnetometerTextView = findViewById(R.id.rtd_magn_value)
        pressureTextView = findViewById(R.id.rtd_pressure_value)
        humidityTextView = findViewById(R.id.rtd_humidity_value)
        rotTextView = findViewById(R.id.rtd_rot_value)
        tempTextView = findViewById(R.id.rtd_temp_value)
        uvTextView = findViewById(R.id.rtd_UV_value)
    }

    private fun switchChanged(isChecked: Boolean) {
        if (isChecked) {
            setRTDProgressBarVisible()
            subscribeToRTD()
        } else {
            setRTDFieldsGone()
            setRTDProgressBarGone()
            unsubscribeFromRTD()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        disconnectDevice()
        onBackPressed()
        return true
    }

    private fun connectToDevice(deviceAddress: String?) {
        if (deviceAddress == null) {
            throw Exception("No device address given")
        }
        setConnectProgressBarVisible()
        bbc?.openConnection(deviceAddress)
    }

    private fun disconnectDevice() {
        invalidateStatusTimer()
        bbc?.closeConnection()
    }

    private fun blinkDevice() {
        setBlinkButtonPressed()
        bbc?.blinkDevice()
    }

    private fun subscribeToRTD() {
        this.firstRTD = true
        bbc?.subscribeToRTD()
    }

    private fun unsubscribeFromRTD() {
        bbc?.unsubscribeFromRTD()
    }

    private fun startStatusTimer() {
        if (statusTimer != null) {
            invalidateStatusTimer()
        }
        this.statusTimer = Timer()
        statusTimer!!.schedule(timerTask {
            //TODO only one works
            bbc?.readDeviceBattery()
            bbc?.readDeviceRssi()
        }, 2000, 2000)
    }

    private fun invalidateStatusTimer() {
        if (statusTimer != null) {
            statusTimer!!.cancel()
            statusTimer!!.purge()
            statusTimer = null
        }
    }

    private val bbcCallback = object : BlueBerryBluetoothClientCallback() {
        override fun onConnect() {
            startStatusTimer()
            setUIConnected()
        }

        override fun onConnectTimeout() {
            disconnectDevice()
            setUIDisconnected()
            showInfoDialog(getString(R.string.connection_timeout_info))
        }

        override fun onDisconnect() {
            setUIDisconnected()
        }

        override fun onReceivedBattery(success: Boolean, batteryLevel: Int?) {
            if (success) {
                updateBattery(batteryLevel!!)
            }
        }

        override fun onReceivedRssi(success: Boolean, rssi: Int?) {
            if (success) {
                updateRssi(rssi!!)
            }
        }

        override fun onReceivedRealTimeData(success: Boolean, data: BbLogEntry.bb_log_entry) {
            if (success) {
                updateRtd(data)
            }
        }
    }

    private fun updateBattery(batteryLevel: Int) {
        runOnUiThread {
            batteryImage?.setImageResource(selectBatteryImage(batteryLevel))
            batteryTextView?.text = getString(R.string.device_battery_level, batteryLevel.toString())
        }
    }

    private fun updateRssi(rssi: Int) {
        runOnUiThread {
            rssiImage?.setImageResource(selectRssiImage(rssi))
            rssiTextView?.text = getString(R.string.device_rssi, rssi.toString())
        }
    }

    private fun updateRtd(data: BbLogEntry.bb_log_entry) {
        runOnUiThread {
            if (firstRTD) {
                firstRTD = false
                setRTDFieldsVisible()
                setRTDProgressBarGone()
            }
            accelerationTextView?.text = constructFieldListString(
                BlueBerryLogEntryFields.ACCELEROMETER,
                data.accelerometerList
            )
            batteryVolTextView?.text =
                constructFieldString(BlueBerryLogEntryFields.BATTERY_VOLTAGE, data.batteryMv)
            illuminanceTextView?.text = constructFieldString(BlueBerryLogEntryFields.LUX, data.lux)
            magnetometerTextView?.text =
                constructFieldListString(BlueBerryLogEntryFields.COMPASS, data.compassList)
            pressureTextView?.text = constructFieldString(BlueBerryLogEntryFields.PRESSURE, data.pressure)
            humidityTextView?.text = constructFieldString(BlueBerryLogEntryFields.HUMIDITY, data.rh)
            rotTextView?.text = constructFieldListString(BlueBerryLogEntryFields.GYRO, data.gyroList)
            tempTextView?.text =
                constructFieldString(BlueBerryLogEntryFields.TEMPERATURE, data.temperature)
            uvTextView?.text = constructFieldString(BlueBerryLogEntryFields.UVI, data.uvi)
        }
    }

    private fun setRTDGone() {
        runOnUiThread {
            rtdView?.visibility = View.GONE
            setRTDFieldsGone()
            setRTDProgressBarGone()
        }
    }

    private fun setRTDVisible() {
        runOnUiThread {
            rtdView?.visibility = View.VISIBLE
        }
    }

    private fun setRTDFieldsGone() {
        runOnUiThread {
            accelerationView?.visibility = View.GONE
            illuminanceView?.visibility = View.GONE
            batteryVolView?.visibility = View.GONE
            magnetometerView?.visibility = View.GONE
            pressureView?.visibility = View.GONE
            humidityView?.visibility = View.GONE
            rotView?.visibility = View.GONE
            tempView?.visibility = View.GONE
            uvView?.visibility = View.GONE
        }
    }

    private fun setRTDFieldsVisible() {
        runOnUiThread {
            accelerationView?.visibility = View.VISIBLE
            illuminanceView?.visibility = View.VISIBLE
            batteryVolView?.visibility = View.VISIBLE
            magnetometerView?.visibility = View.VISIBLE
            pressureView?.visibility = View.VISIBLE
            humidityView?.visibility = View.VISIBLE
            rotView?.visibility = View.VISIBLE
            tempView?.visibility = View.VISIBLE
            uvView?.visibility = View.VISIBLE
        }
    }

    private fun setRTDProgressBarVisible() {
        runOnUiThread {
            rtdProgressBar?.visibility = View.VISIBLE
        }
    }

    private fun setRTDProgressBarGone() {
        runOnUiThread {
            rtdProgressBar?.visibility = View.GONE
        }
    }

    private fun setUIConnected() {
        setRTDVisible()
        setConnectProgressBarGone()
        runOnUiThread {
            connectedTextView?.text = getString(R.string.device_connected)
            connectedImage?.setImageResource(R.drawable.device_connected)
            blinkTextView?.text = getString(R.string.device_blink_available)
            blinkImage?.setImageResource(R.drawable.blink_connected)
        }
    }

    private fun setUIDisconnected() {
        setRTDGone()
        setConnectProgressBarGone()
        runOnUiThread {
            connectedTextView?.text = getString(R.string.device_disconnected)
            connectedImage?.setImageResource(R.drawable.device_disconnected)
            blinkTextView?.text = getString(R.string.device_disconnected)
            blinkImage?.setImageResource(R.drawable.blink_disconnected)
            batteryTextView?.text = getString(R.string.device_value_unavailable)
            batteryImage?.setImageResource(R.drawable.battery_disconnected)
            rssiTextView?.text = getString(R.string.device_value_unavailable)
            rssiImage?.setImageResource(R.drawable.rssi_disconnected)
        }
    }

    private fun setConnectProgressBarVisible() {
        runOnUiThread {
            connectProgressBar?.visibility = View.VISIBLE
        }
    }

    private fun setConnectProgressBarGone() {
        runOnUiThread {
            connectProgressBar?.visibility = View.GONE
        }
    }

    private fun setBlinkButtonPressed() {
        runOnUiThread {
            blinkImage?.setImageResource(R.drawable.blink_activated)
            Timer().schedule(timerTask {
                blinkImage?.setImageResource(R.drawable.blink_connected)
            }, 2000)
        }
    }

    private fun selectBatteryImage(batteryLevel: Int): Int {
        val percentagePerStep = 100 / 7
        return when (batteryLevel / percentagePerStep) {
            0 -> R.drawable.battery_1bar
            1 -> R.drawable.battery_1bar
            2 -> R.drawable.battery_2bar
            3 -> R.drawable.battery_3bar
            4 -> R.drawable.battery_4bar
            5 -> R.drawable.battery_5bar
            6 -> R.drawable.battery_6bar
            7 -> R.drawable.battery_full
            else -> R.drawable.battery_full
        }
    }

    private fun selectRssiImage(rssi: Int): Int {
        val dbPerStep = -100.0 / 5.0
        return when ((rssi / dbPerStep).roundToInt()) {
            0 -> R.drawable.rssi_full
            1 -> R.drawable.rssi_full
            2 -> R.drawable.rssi_4bar
            3 -> R.drawable.rssi_3bar
            4 -> R.drawable.rssi_2bar
            5 -> R.drawable.rssi_1bar
            else -> R.drawable.rssi_1bar
        }
    }

    private fun constructFieldListString(field: BlueBerryLogEntryField, list: List<Int>): String {
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

    private fun constructFieldString(field: BlueBerryLogEntryField, data: Number): String {
        return field.tounit(data.toFloat()).round(3).toString() + " " + field.unit
    }

    private fun showInfoDialog(message: String) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.info_dialog_title)
            builder.setMessage(message)
            builder.setPositiveButton(R.string.dialog_confirm) { dialog, which ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    private fun Float.round(decimals: Int): Float {
        val multiplier = 10.0.pow(decimals)
        return ((this * multiplier).toInt().toFloat() / multiplier).toFloat()
    }
}