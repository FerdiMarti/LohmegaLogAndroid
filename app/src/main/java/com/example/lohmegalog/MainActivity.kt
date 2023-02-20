package com.example.lohmegalog

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val scanResults: ArrayList<ScanResultData> = ArrayList()
    var scanResultView: RecyclerView? = null
    var progressBar: ProgressBar? = null
    val bleAdapter = BlueBerryBluetooth.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        allowLocationDetectionPermissions()

        progressBar = findViewById(R.id.progress)

        scanResultView = findViewById<RecyclerView>(R.id.scan_result_view)
        scanResultView?.layoutManager = LinearLayoutManager(this)
        val adapter = ScanResultAdapter(scanResults,
        ScanResultAdapter.OnClickListener { data ->
            openDevicePage(data.device)
        })
        scanResultView?.adapter = adapter
    }

    fun openDevicePage(device: BluetoothDevice) {
        bleAdapter.openConnection(device)
        val intent = Intent(this, DeviceActivity::class.java)
        startActivity(intent)
    }

    private fun allowLocationDetectionPermissions() {
        ActivityCompat.requestPermissions(this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    this.startScan()
                } else {
                    print("please allow")
                }
                return
            }
        }
    }

    private fun startScan() {
        progressBar?.visibility = View.VISIBLE
        bleAdapter.scanLe(true, {
            progressBar?.visibility = View.GONE
        }) { result ->
            if (!scanResults.contains(result)) {
                scanResults.add(result)
                scanResultView?.adapter?.notifyDataSetChanged()
                Log.d("RESULT", result.name)
            }
        }
    }

    companion object {
        private const val FINE_LOCATION_PERMISSION_REQUEST = 1001
    }
}