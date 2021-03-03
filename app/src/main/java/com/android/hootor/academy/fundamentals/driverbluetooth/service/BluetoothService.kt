package com.android.hootor.academy.fundamentals.driverbluetooth.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.android.hootor.academy.fundamentals.driverbluetooth.App
import com.android.hootor.academy.fundamentals.driverbluetooth.MainActivity
import com.android.hootor.academy.fundamentals.driverbluetooth.R
import com.android.hootor.academy.fundamentals.driverbluetooth.prefs.SharedPrefsManager
import com.android.hootor.academy.fundamentals.models.BroadcastData
import com.android.hootor.academy.fundamentals.models.ScanningData
import com.android.hootor.academy.fundamentals.settingsblutoothspp.ui.Constants
import com.android.hootor.academy.fundamentals.settingsblutoothspp.ui.Constants.BROADCAST_ACTION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

class BluetoothService : LifecycleService() {

    private var isFirstRun = true
    private var serviceKilled = false
    private var scanningJob: Job? = null
    private var btSocket: BluetoothSocket? = null

    private lateinit var curNotificationBuilder: NotificationCompat.Builder
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    companion object {
        val isScanning = MutableLiveData<Boolean>()
        val scanningData = MutableLiveData<ScanningData>()
    }

    private fun postInitialValues() {
        isScanning.postValue(false)
//        scanningData.postValue(ScanningData.Empty)
    }

    private fun killService() {
        serviceKilled = true
        isFirstRun = true
        pauseService()
        btSocket?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.i("happy", e.toString())
            }
        }
        scanningJob?.cancel()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        isScanning.postValue(false)
    }

    override fun onCreate() {
        super.onCreate()

        curNotificationBuilder = provideBaseNotificationBuilder()

        postInitialValues()

        scanningData.observe(this, {
            it?.let { updateBroadCast(it) }
        })
    }

    private fun updateBroadCast(scanningData: ScanningData) {

        val intent = Intent(BROADCAST_ACTION)

        val data =
            when (scanningData) {
                is ScanningData.Empty -> {
                    BroadcastData()
                }
                is ScanningData.Error -> {
                    BroadcastData(
                        isError = true, error = scanningData.message
                    )
                }
                is ScanningData.Barcode -> {
                    BroadcastData(barcode = scanningData.data)
                }
                is ScanningData.HasNotBluetoothPermissions -> {
                    BroadcastData(settingsNotMade = true)
                }
                is ScanningData.ScanningClose -> {
                    BroadcastData(disconnection = true)
                }
                is ScanningData.ShowSettingFragment -> {
                    BroadcastData(settingsNotMade = true)
                }
                is ScanningData.NewConnection -> {
                    BroadcastData(isConnected = true)
                }
            }
        val json = Json.encodeToString(data)
        intent.putExtra("dataScanning", json)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                    } else {
                        startScanning()
                    }
                }
                Constants.ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }
                Constants.ACTION_STOP_SERVICE -> {
                    killService()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {

        startScanning()

        isScanning.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        startForeground(Constants.NOTIFICATION_ID, curNotificationBuilder.build())
    }

    private fun startScanning() {

        if (btSocket?.isConnected == true) {
            return
        }

        scanningJob = lifecycleScope.launch(Dispatchers.IO) {
            val currMac = SharedPrefsManager.getInstance().getMac()
            if (currMac.isNotEmpty()
                && bluetoothAdapter?.isEnabled == true
            ) {
                isScanning.postValue(true)
                CoroutineScope(Dispatchers.IO).launch {

                    val device = bluetoothAdapter.getRemoteDevice(currMac)

                    btSocket = try {
                        device.createRfcommSocketToServiceRecord(Constants.SOCKET_UUID)
                    } catch (e: IOException) {
                        isScanning.postValue(false)
                        scanningData.postValue(ScanningData.Error(e.message.toString()))
                        null
                    }
                    bluetoothAdapter.cancelDiscovery()

                    btSocket?.apply {
                        try {
                            connect()
                            val buffer = ByteArray(1024)
                            while (isScanning.value!!) {
                                try {
                                    val bytes = inputStream.read(buffer)
                                    val bc = String(buffer, 0, bytes)
                                    scanningData.postValue(ScanningData.Barcode(bc))
                                } catch (e: IOException) {
                                    isScanning.postValue(false)
                                    scanningData.postValue(ScanningData.ScanningClose)
                                    killService()
                                    break
                                }
                            }
                        } catch (e: IOException) {
                            try {
                                close()
                                isScanning.postValue(false)
                                scanningData.postValue(ScanningData.Error(e.message.toString()))
                                killService()
                            } catch (e2: IOException) {
                                isScanning.postValue(false)
                                scanningData.postValue(ScanningData.Error(e2.message.toString()))
                                killService()
                            }
                        }
                    }

                }
            } else {
                scanningData.postValue(ScanningData.ShowSettingFragment)
                killService()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun provideMainActivityPendingIntent() =
        PendingIntent.getActivity(
            App.INSTANCE,
            0,
            Intent(App.INSTANCE, MainActivity::class.java).also {
                it.action = Constants.ACTION_SHOW_SETTING_BLUETOOTH_FRAGMENT
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun provideBaseNotificationBuilder() =
        NotificationCompat.Builder(App.INSTANCE, Constants.NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_barecode_scanner_24)
            .setContentTitle("Scanning")
//            .setContentText("00:00:00")
            .setVibrate(null)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setContentIntent(provideMainActivityPendingIntent())

}