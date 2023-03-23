package com.example.lohmegalog.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
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
        requestBLEPermissions()
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

    private fun requestBLEPermissions() {
        ActivityCompat.requestPermissions(
            this@ScanActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    this.startScan()
                } else {
                    showPermissionsInfo()
                }
                return
            }
        }
    }

    private fun checkBLEPermissions(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val res: Int = checkCallingOrSelfPermission(permission)
        val granted = res == PackageManager.PERMISSION_GRANTED
        if (!granted) showPermissionsInfo()
        return granted
    }

    private fun showPermissionsInfo() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.permission_dialog_title)
        builder.setMessage(R.string.permission_dialog_text)
        builder.setPositiveButton(R.string.dialog_confirm) { dialog, which ->
            requestBLEPermissions()
        }
        builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun startScan() {
        if (!checkBLEPermissions()) return
        clearScanResults()
        setScanningUI()
        bbDiscoverer.scanLe(true, {
            setStopUI()
        }, { result ->
            addScanResult(result)
        })
    }

    private fun stopScan() {
        bbDiscoverer.scanLe(false, {
            setStopUI()
        }, null)
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