package com.joeyseph.obdometer

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.pires.obd.commands.control.DistanceSinceCCCommand
import com.github.pires.obd.commands.control.VinCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.enums.ObdProtocols
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class LocationBackgroundService : Service() {

    private var mGpsLocationClient: LocationManager? = null
    private val TAG = "LocationBackgroundServi"
    private var geocoder: Geocoder? = null
    var sharedPref: SharedPreferences? = null
    val message_received = "com.joeyseph.obdometer.MESSAGE_UPDATE_UI"
    var bluetoothSocket: BluetoothSocket? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null


    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: running...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("com.joeyseph.obdometer.foreground", "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, "com.joeyseph.obdometer.foreground")
            .setContentTitle("OBDometer is running...")
            .setContentText("Please do not close the app.")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {

        }
        bluetoothAdapter.startDiscovery()

        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if(device?.address == "13:E0:2F:8D:57:85") {
                        Log.d(TAG, "onReceive: Found transmitter!!")
                        bluetoothSocket =
                            device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                        bluetoothSocket?.connect()
                        inputStream = bluetoothSocket?.inputStream
                        outputStream = bluetoothSocket?.outputStream
                        EchoOffCommand().run(inputStream, outputStream)
                        LineFeedOffCommand().run(inputStream, outputStream)
                        TimeoutCommand(0xFF).run(inputStream, outputStream)
                        SelectProtocolCommand(ObdProtocols.AUTO).run(inputStream, outputStream)
                        val ccc = DistanceSinceCCCommand()
                        ccc.run(inputStream, outputStream)
                        Log.d(TAG, "onReceive: ${ccc.formattedResult}")
                        val km = Integer.parseInt(ccc.formattedResult.replace("km", ""))
                        val miles: Float = km * 0.621371f
                        val editor = sharedPref?.edit()
                        editor?.putFloat("startDistance", miles)
                        editor?.apply()
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        Log.d(TAG, "onStartCommand: Requesting location updates...")
        geocoder = Geocoder(this, Locale.getDefault())
        mGpsLocationClient = getSystemService(LOCATION_SERVICE) as LocationManager
        startLocationUpdates()
        sharedPref = getSharedPreferences("com.joeyseph.obdometer", Context.MODE_PRIVATE)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mGpsLocationClient.let {
            Log.d(TAG, "startLocationUpdates: LocationClient is not null")
            it?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 300f, locationListener)
        }
    }

    private val locationListener =
        android.location.LocationListener { location -> //handle location change
            Log.d(TAG, ": $location")
            val ccc = DistanceSinceCCCommand()
            ccc.run(inputStream, outputStream)
            val state: String? =
                geocoder?.getFromLocation(location.latitude, location.longitude, 1)?.get(0)
                    ?.adminArea
            val road: String? = geocoder?.getFromLocation(location.latitude, location.longitude, 1)?.get(0)
                ?.thoroughfare

            if(sharedPref?.getString("state", "") != state &&
                sharedPref?.getString("state", "") != "") {
                val ccc = DistanceSinceCCCommand()
                ccc.run(inputStream, outputStream)
                Log.d(TAG, "onReceive: ${ccc.formattedResult}")
                val km = Integer.parseInt(ccc.formattedResult.replace("km", ""))
                val miles: Float = (km * 0.621371f) - sharedPref?.getFloat("startDistance", 0.0f)!!
                val log = hashMapOf(
                    "uid" to FirebaseAuth.getInstance().currentUser?.uid,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "road" to road,
                    "oldState" to sharedPref?.getString("state", ""),
                    "newState" to state,
                    "miles" to miles,
                    "timestamp" to System.currentTimeMillis()
                )

                sharedPref!!.edit().putFloat("startDistance", miles).apply()

                val db = Firebase.firestore

                db.collection("logs")
                    .add(log)
                    .addOnSuccessListener {
                        Log.d(TAG, "Sent data!")
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "Something went wrong...")
                    }
            }
            val broadcaster = LocalBroadcastManager.getInstance(this);
            val editor = sharedPref?.edit()
            editor?.putString("state", state)
            editor?.putString("latitude", "${location.latitude}")
            editor?.putString("longitude", "${location.longitude}")
            editor?.putString("road", road)
            editor?.apply()
            val broadcast = Intent(message_received)
            broadcaster.sendBroadcast(broadcast)
        }

    override fun onDestroy() {
        bluetoothSocket?.close()
        super.onDestroy()
    }
}