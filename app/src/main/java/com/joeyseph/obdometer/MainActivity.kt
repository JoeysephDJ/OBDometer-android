package com.joeyseph.obdometer

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    var location_perm: String = Manifest.permission.ACCESS_FINE_LOCATION
    var bluetooth_perm: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH_ADMIN
    }


    private var sharedPref: SharedPreferences? = null
    val message_received = "com.joeyseph.obdometer.MESSAGE_UPDATE_UI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPref = getSharedPreferences("com.joeyseph.obdometer", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.putString("state", "")
        editor?.putString("latitude", "")
        editor?.putString("longitude", "")
        editor?.putString("road", "")
        editor?.apply()

        if(ContextCompat.checkSelfPermission(this, location_perm) == PackageManager.PERMISSION_DENIED
            || ContextCompat.checkSelfPermission(this, bluetooth_perm) == PackageManager.PERMISSION_DENIED) {
            val perm: Intent = Intent(this, PermissionsActivity::class.java)
            startActivity(perm)
        }

        val user = Firebase.auth.currentUser
        if(user == null) {
            val login: Intent = Intent(this, LoginActivity::class.java)
            startActivity(login)
        }

        val btnStart: Button = findViewById(R.id.btnStart)

        btnStart.setOnClickListener {
            var stopped = false
            val bgServ: Intent = Intent(this, LocationBackgroundService::class.java)
            val serviceClassName = "com.joeyseph.obdometer.LocationBackgroundService"
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
            for (serviceInfo in runningServices) {
                if (serviceClassName == serviceInfo.service.className) {
                    stopService(bgServ)
                    stopped = true
                    btnStart.text = "Start Service"
                }
            }
            if(!stopped) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(bgServ)
                } else {
                    startService(bgServ)
                }
                btnStart.text = "Stop Service"
            }
        }

        val broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                Log.d(TAG, "onReceive: Received.")
                val state = sharedPref?.getString("state", "")
                val lat = sharedPref?.getString("latitude", "")
                val long = sharedPref?.getString("longitude", "")
                val road = sharedPref?.getString("road", "")
                val miles = sharedPref?.getFloat("miles", 0f)
                runOnUiThread(Runnable {
                    val tvState: TextView = findViewById(R.id.tvState)
                    val tvLatitude: TextView = findViewById(R.id.tvLatitude)
                    val tvLongitude: TextView = findViewById(R.id.tvLongitude)
                    val tvRoad: TextView = findViewById(R.id.tvRoad)
                    val tvMiles: TextView = findViewById(R.id.tvMiles)
                    tvState.text = "State: $state"
                    tvLatitude.text = "Latitude: $lat"
                    tvLongitude.text = "Longitude: $long"
                    tvRoad.text = "Road: $road"
                    tvMiles.text = "Miles: $miles"
                })
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadCastReceiver, IntentFilter(message_received))


    }

}