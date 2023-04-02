package com.joeyseph.obdometer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsActivity : AppCompatActivity() {
    private val TAG = "PermissionsActivity"
    var location_perm: String = Manifest.permission.ACCESS_FINE_LOCATION
    var bluetooth_perm: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH_ADMIN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)


        val btnRequest: Button = findViewById(R.id.btnRequest)
        val btnBluetooth: Button = findViewById(R.id.btnBluetooth)
        btnRequest.setOnClickListener {
            Log.d(TAG, "onCreate: Button pressed.")
            if (ContextCompat.checkSelfPermission(
                    this,
                    location_perm
                ) !==
                PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        location_perm
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(location_perm), 1
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(location_perm), 1
                    )
                }
            }
        }
        btnBluetooth.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    bluetooth_perm
                ) !==
                PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        bluetooth_perm
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(bluetooth_perm, Manifest.permission.BLUETOOTH_SCAN), 1
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(bluetooth_perm, Manifest.permission.BLUETOOTH_SCAN), 1
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) ===
                                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                            bluetooth_perm) ===
                                PackageManager.PERMISSION_GRANTED))  {
                        finish()
                    }
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: denied")
                }
                return
            }
        }
    }
}