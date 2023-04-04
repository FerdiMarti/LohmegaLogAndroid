package com.example.lohmegalog.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.lohmegalog.R

/**
 * Helper methods to handle BLE permissions
 */
class BluetoothPermissions {
    companion object {
        const val PERMISSION_REQUEST = 1001

        fun requestPermission(context: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBLEPermission(context)
            } else {
                requestLocationPermission(context)
            }
        }

        fun checkPermission(context: Activity): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val permission = Manifest.permission.BLUETOOTH_SCAN
                val res: Int = context.checkCallingOrSelfPermission(permission)
                val granted = res == PackageManager.PERMISSION_GRANTED
                if (!granted) showBLEPermissionInfo(context)
                return granted
            } else {
                val permission = Manifest.permission.ACCESS_FINE_LOCATION
                val res: Int = context.checkCallingOrSelfPermission(permission)
                val granted = res == PackageManager.PERMISSION_GRANTED
                if (!granted) showLocationPermissionInfo(context)
                return granted
            }
        }

        fun showPermissionInfo(context: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                showBLEPermissionInfo(context)
            } else {
                showLocationPermissionInfo(context)
            }
        }

        private fun requestLocationPermission(context: Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST
            )
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun requestBLEPermission(context: Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_REQUEST
            )
        }

        private fun showLocationPermissionInfo(context: Activity) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.permission_dialog_title)
            builder.setMessage(R.string.location_permission_dialog_text)
            builder.setPositiveButton(R.string.dialog_confirm) { dialog, which ->
                requestLocationPermission(context)
            }
            builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
                dialog.cancel()
            }
            builder.show()
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun showBLEPermissionInfo(context: Activity) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.permission_dialog_title)
            builder.setMessage(R.string.ble_permission_dialog_text)
            builder.setPositiveButton(R.string.dialog_confirm) { dialog, which ->
                requestBLEPermission(context)
            }
            builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
                dialog.cancel()
            }
            builder.show()
        }
    }
}