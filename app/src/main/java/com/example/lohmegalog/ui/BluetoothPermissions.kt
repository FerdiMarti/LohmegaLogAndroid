package com.example.lohmegalog.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.lohmegalog.R

/**
 * Helper methods to handle BLE permissions
 */
class BluetoothPermissions {
    companion object {

        const val FINE_LOCATION_PERMISSION_REQUEST = 1001

        fun requestBLEPermissions(context: Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_LOCATION_PERMISSION_REQUEST
            )
        }

        fun checkBLEPermissions(context: Activity): Boolean {
            val permission = Manifest.permission.ACCESS_FINE_LOCATION
            val res: Int = context.checkCallingOrSelfPermission(permission)
            val granted = res == PackageManager.PERMISSION_GRANTED
            if (!granted) showPermissionsInfo(context)
            return granted
        }

        fun showPermissionsInfo(context: Activity) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.permission_dialog_title)
            builder.setMessage(R.string.permission_dialog_text)
            builder.setPositiveButton(R.string.dialog_confirm) { dialog, which ->
                requestBLEPermissions(context)
            }
            builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
                dialog.cancel()
            }
            builder.show()
        }
    }
}