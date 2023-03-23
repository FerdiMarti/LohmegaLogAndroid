package com.example.lohmegalog.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lohmegalog.R
import com.example.lohmegalog.bbBluetooth.BlueBerryBluetoothDiscoverer

class ScanActivity : AppCompatActivity() {
    private var scanResultView: RecyclerView? = null
    private var progressBar: ProgressBar? = null
    private var stopScanButton: MenuItem? = null
    private var rescanButton: MenuItem? = null

    private val scanResults: ArrayList<ScanResultData> = ArrayList()
    private val bbDiscoverer = BlueBerryBluetoothDiscoverer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        progressBar = findViewById(R.id.progress)
        scanResultView = findViewById(R.id.scan_result_view)
        scanResultView?.layoutManager = LinearLayoutManager(this)
        val adapter = ScanResultAdapter(scanResults,
            ScanResultAdapter.OnClickListener { data ->
                openDevicePage(data.address)
            })
        scanResultView?.adapter = adapter

        setSupportActionBar(findViewById(R.id.main_toolbar))
        BluetoothPermissions.requestBLEPermissions(this)
    }

    override fun onResume() {
        super.onResume()
        //clearScanResults()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scan, menu)
        stopScanButton = menu?.findItem(R.id.action_stopscan)
        rescanButton = menu?.findItem(R.id.action_rescan)
        setStopUI()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BluetoothPermissions.FINE_LOCATION_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    this.startScan()
                } else {
                    BluetoothPermissions.showPermissionsInfo(this)
                }
                return
            }
        }
    }

    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        ActivityCompat.startActivityForResult(this, enableBtIntent, 1, null)
    }

    private fun startScan() {
        if (!BluetoothPermissions.checkBLEPermissions(this)) return
        clearScanResults()
        setScanningUI()
        try {
            bbDiscoverer.scanLe(true, {
                setStopUI()
            }, { result ->
                addScanResult(result)
            })
        } catch (e: java.lang.IllegalArgumentException) {
            setStopUI()
            Log.e(TAG, "Exception while scanning $e")
        } catch (e: IllegalStateException) {
            setStopUI()
            promptEnableBluetooth()
        }

    }

    private fun stopScan() {
        try {
            bbDiscoverer.scanLe(false, {
                setStopUI()
            }, null)
        } catch (e: IllegalStateException) {
            setStopUI()
            promptEnableBluetooth()
        }
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

    private fun setScanningUI() {
        runOnUiThread {
            stopScanButton?.isVisible = true
            rescanButton?.isVisible = false
            progressBar?.visibility = View.VISIBLE
        }
    }

    private fun setStopUI() {
        runOnUiThread {
            stopScanButton?.isVisible = false
            rescanButton?.isVisible = true
            progressBar?.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "ScanActivity"
    }
}