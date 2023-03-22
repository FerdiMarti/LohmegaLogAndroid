package com.example.lohmegalog.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lohmegalog.BBBluetooth.BlueBerryBluetoothDiscoverer
import com.example.lohmegalog.R

class MainActivity : AppCompatActivity() {
    private val scanResults: ArrayList<ScanResultData> = ArrayList()
    private var scanResultView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private val bleAdapter = BlueBerryBluetoothDiscoverer(this)
    private var stopScanButton: MenuItem? = null
    private var rescanButton: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progress)

        scanResultView = findViewById(R.id.scan_result_view)
        scanResultView?.layoutManager = LinearLayoutManager(this)
        val adapter = ScanResultAdapter(scanResults,
        ScanResultAdapter.OnClickListener { data ->
            openDevicePage(data.address)
        })
        scanResultView?.adapter = adapter

        setSupportActionBar(findViewById(R.id.main_toolbar))

        allowLocationDetectionPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        stopScanButton = menu!!.findItem(R.id.action_stopscan)
        rescanButton = menu!!.findItem(R.id.action_rescan)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_rescan -> {
            startScan()
            true
        }

        R.id.action_stopscan -> {
            stopScan()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun openDevicePage(address: String) {
        stopScan()
        val intent = Intent(this, DeviceActivity::class.java)
        intent.putExtra(DeviceActivity.ADDRESS_INTENT_KEY, address)
        startActivity(intent)
    }

    private fun allowLocationDetectionPermissions() {
        ActivityCompat.requestPermissions(this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST
        )
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
        clearScanResults()
        setScanningUI()
        bleAdapter.scanLe(true, {
            setStopUI()
        }, { result ->
            addScanResult(result)
        })
    }

    private fun addScanResult(result: ScanResultData) {
        if (!scanResults.contains(result)) {
            scanResults.add(result)
            scanResultView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun clearScanResults() {
        scanResults.clear()
        scanResultView?.adapter?.notifyDataSetChanged()
    }

    private fun stopScan() {
        bleAdapter.scanLe(false, {
            setStopUI()
        }, null)
    }

    private fun setScanningUI() {
        stopScanButton?.isVisible = true
        rescanButton?.isVisible = false
        progressBar?.visibility = View.VISIBLE
    }

    private fun setStopUI() {
        stopScanButton?.isVisible = false
        rescanButton?.isVisible = true
        progressBar?.visibility = View.GONE
    }

    companion object {
        private const val FINE_LOCATION_PERMISSION_REQUEST = 1001
    }
}