package com.example.lohmegalog.UI

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.lohmegalog.BBBluetooth.BlueBerryBluetoothClient
import com.example.lohmegalog.BBBluetooth.BlueBerryBluetoothClientCallback
import com.example.lohmegalog.BlueBerryLogEntryFields
import com.example.lohmegalog.R
import com.example.lohmegalog.BlueBerryLogEntryField
import com.example.lohmegalog.protobuf.BbLogEntry
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.pow
import kotlin.math.roundToInt


class DeviceActivity : AppCompatActivity() {
    var bbc: BlueBerryBluetoothClient? = null
    var connectedImage: ImageView? = null
    var connectedTV: TextView? = null
    var rssiImage: ImageView? = null
    var rssiTV: TextView? = null
    var batteryImage: ImageView? = null
    var batteryTV: TextView? = null
    var blinkImage: ImageView? = null
    var blinkTV: TextView? = null
    var connectProgressBar: ProgressBar? = null

    //RTD Views
    var rtdProgressBar: ProgressBar? = null
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

    private var firstRTD: Boolean = true
    private var statusTimer: Timer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        setupUI()
        bbc = BlueBerryBluetoothClient(this, bbcCallback)
        val deviceAddress = intent.getStringExtra("address")
        connectToDevice(deviceAddress)
    }

    
    fun setupUI() {
        connectProgressBar = findViewById<ProgressBar>(R.id.progress_connect)
        connectedImage = findViewById<ImageView>(R.id.connected_image)
        connectedTV = findViewById<TextView>(R.id.connected_text_view)
        rssiImage = findViewById<ImageView>(R.id.rssi_image)
        rssiTV = findViewById<TextView>(R.id.rssi_text_view)
        batteryImage = findViewById<ImageView>(R.id.battery_image)
        batteryTV = findViewById<TextView>(R.id.battery_text_view)
        blinkImage = findViewById<ImageView>(R.id.blink_image)
        blinkTV = findViewById<TextView>(R.id.blink_text_view)
        setSupportActionBar(findViewById(R.id.device_toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        blinkImage?.setOnClickListener {
            blinkDevice()
        }

        rtdView = findViewById(R.id.rtd_view)
        rtdProgressBar = findViewById(R.id.progress_rtd)
        setRTDGone()
        rtdSwitch = findViewById(R.id.rtd_switch)
        rtdSwitch?.isChecked = false
        rtdSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                setRTDProgressBarVisible()
                subscribeToRTD()
            } else {
                setRTDFieldsGone()
                setRTDProgressBarGone()
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

    private fun setRTDGone() {
        runOnUiThread() {
            rtdView?.visibility = View.GONE
            setRTDFieldsGone()
            setRTDProgressBarGone()
        }
    }

    private fun setRTDVisible() {
        runOnUiThread() {
            rtdView?.visibility = View.VISIBLE
        }
    }

    private fun setRTDFieldsGone() {
        runOnUiThread() {
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
    }

    private fun setRTDFieldsVisible() {
        runOnUiThread() {
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
    }

    private fun setRTDProgressBarVisible() {
        runOnUiThread() {
            rtdProgressBar?.visibility = View.VISIBLE
        }
    }

    private fun setRTDProgressBarGone() {
       runOnUiThread() {
           rtdProgressBar?.visibility = View.GONE
       }
    }

    private fun setUIConnected() {
        setRTDVisible()
        setConnectProgressBarGone()
        runOnUiThread() {
            connectedTV?.text = "Connected"
            connectedImage?.setImageResource(R.drawable.device_connected)
            blinkTV?.text = "Blink"
            blinkImage?.setImageResource(R.drawable.blink_connected)
        }
    }

    private fun setUIDisconnected() {
        setRTDGone()
        setConnectProgressBarGone()
        runOnUiThread() {
            connectedTV?.text = "Not Connected"
            connectedImage?.setImageResource(R.drawable.device_disconnected)
            blinkTV?.text = "Not Connected"
            blinkImage?.setImageResource(R.drawable.blink_disconnected)
            batteryTV?.text = "n/a"
            batteryImage?.setImageResource(R.drawable.battery_disconnected)
            rssiTV?.text = "n/a"
            rssiImage?.setImageResource(R.drawable.rssi_disconnected)
        }
    }

    private fun setConnectProgressBarVisible() {
        runOnUiThread() {
            connectProgressBar?.visibility = View.VISIBLE
        }
    }

    private fun setConnectProgressBarGone() {
        runOnUiThread() {
            connectProgressBar?.visibility = View.GONE
        }
    }

    private fun setBlinkButtonPressed() {
        runOnUiThread() {
            blinkImage?.setImageResource(R.drawable.blink_activated)
            Timer().schedule(timerTask {
                blinkImage?.setImageResource(R.drawable.blink_connected)
            }, 2000)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        disconnectDevice()
        return true
    }

    private fun connectToDevice(deviceAddress: String?) {
        if (deviceAddress == null) {}//TODO
        setConnectProgressBarVisible()
        bbc?.openConnection(deviceAddress!!)
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

    private val bbcCallback = object : BlueBerryBluetoothClientCallback() {
        override fun onConnect() {
            updateStatusRegularly()
            setUIConnected()
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
        runOnUiThread() {
            batteryImage?.setImageResource(selectBatteryImage(batteryLevel))
            batteryTV?.text = batteryLevel.toString() + "%"
        }
    }

    private fun updateRssi(rssi: Int) {
        runOnUiThread() {
            rssiImage?.setImageResource(selectRssiImage(rssi))
            rssiTV?.text = rssi.toString() + " db"
        }
    }
    
    private fun updateRtd(data: BbLogEntry.bb_log_entry) {
        runOnUiThread() {
            if (firstRTD) {
                firstRTD = false
                setRTDFieldsVisible()
                setRTDProgressBarGone()
            }
            accelerationTV?.text = constructFieldListString(
                BlueBerryLogEntryFields.ACCELEROMETER,
                data.accelerometerList
            )
            battTV?.text = constructFieldString(BlueBerryLogEntryFields.BATVOLT, data.batteryMv)
            illuminanceTV?.text = constructFieldString(BlueBerryLogEntryFields.LUX, data.lux)
            magnTV?.text =
                constructFieldListString(BlueBerryLogEntryFields.COMPASS, data.compassList)
            pressureTV?.text = constructFieldString(BlueBerryLogEntryFields.PRESSURE, data.pressure)
            humidityTV?.text = constructFieldString(BlueBerryLogEntryFields.HUMIDITY, data.rh)
            rotTV?.text = constructFieldListString(BlueBerryLogEntryFields.GYRO, data.gyroList)
            tempTV?.text =
                constructFieldString(BlueBerryLogEntryFields.TEMPERATURE, data.temperature)
            uvTV?.text = constructFieldString(BlueBerryLogEntryFields.UVI, data.uvi)
        }
    }

    fun updateStatusRegularly() {
        this.statusTimer = Timer()
        statusTimer!!.schedule(timerTask {
            //TODO only one works
            bbc?.readDeviceBattery()
            bbc?.readDeviceRssi()
        }, 2000, 2000)
    }

    fun invalidateStatusTimer() {
        if (statusTimer != null) {
            statusTimer!!.cancel()
            statusTimer!!.purge()
            statusTimer = null
        }
    }

    fun selectBatteryImage(batteryLevel: Int): Int {
        val percentagePerStep = 100 / 7
        when(batteryLevel / percentagePerStep) {
            0 -> return R.drawable.battery_1bar
            1 -> return R.drawable.battery_1bar
            2 -> return R.drawable.battery_2bar
            3 -> return R.drawable.battery_3bar
            4 -> return R.drawable.battery_4bar
            5 -> return R.drawable.battery_5bar
            6 -> return R.drawable.battery_6bar
            7 -> return R.drawable.battery_full
            else -> return R.drawable.battery_full
        }
    }

    fun selectRssiImage(rssi: Int): Int {
        val dbPerStep = -100.0 / 5.0
        when((rssi / dbPerStep).roundToInt()) {
            0 -> return R.drawable.rssi_full
            1 -> return R.drawable.rssi_full
            2 -> return R.drawable.rssi_4bar
            3 -> return R.drawable.rssi_3bar
            4 -> return R.drawable.rssi_2bar
            5 -> return R.drawable.rssi_1bar
            else -> return R.drawable.rssi_1bar
        }
    }

    fun constructFieldListString(field: BlueBerryLogEntryField, list: List<Int>): String {
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

    fun constructFieldString(field: BlueBerryLogEntryField, data: Number): String {
        return field.tounit(data.toFloat()).round(3).toString() + " " + field.unit
    }

    fun Float.round(decimals: Int): Float {
        val multiplier = 10.0.pow(decimals)
        return ((this * multiplier).toInt().toFloat() / multiplier).toFloat()
    }
}