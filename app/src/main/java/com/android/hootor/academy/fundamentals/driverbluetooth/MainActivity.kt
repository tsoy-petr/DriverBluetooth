package com.android.hootor.academy.fundamentals.driverbluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hootor.academy.fundamentals.driverbluetooth.other.Utils
import com.android.hootor.academy.fundamentals.driverbluetooth.prefs.SharedPrefsManager
import com.android.hootor.academy.fundamentals.driverbluetooth.service.BluetoothService
import com.android.hootor.academy.fundamentals.driverbluetooth.ui.BluetoothDeviceAdapter
import com.android.hootor.academy.fundamentals.models.BlDevice
import com.android.hootor.academy.fundamentals.settingsblutoothspp.ui.Constants
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var currentDevice: TextView
    private lateinit var currentServiceData: TextView
    private lateinit var currentActiveService: TextView

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private var blAdapter: BluetoothDeviceAdapter? = null

    companion object {
        private const val KEY_ACTION_FIELD = "action"
        private const val KEY_ACTION_START_SERVICE = "KEY_ACTION_START_SERVICE"
        private const val KEY_ACTION_STOP_SERVICE = "KEY_ACTION_STOP_SERVICE"
        private const val KEY_ACTION_SHOW_SETTINGS = "KEY_ACTION_SHOW_SETTINGS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
        initView()
        loadFieldsFromSharedPref()
        initRecyclerView()
        subscribeToObserver()
        setupBluetoothAdapter()

    }

    private fun checkingIntent() {
        val action1C = intent.getStringExtra(KEY_ACTION_FIELD)
        action1C?.let { action ->

            when (action) {
                KEY_ACTION_START_SERVICE -> {
                    sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
                }
                KEY_ACTION_STOP_SERVICE -> {
                    sendCommandToService(Constants.ACTION_STOP_SERVICE)
                }
                KEY_ACTION_SHOW_SETTINGS -> {

                }
                else -> {
                    finish()
                }
            }
        }
    }

    private fun requestPermissions() {
        if (Utils.hasBluetoothPermissions(this)) {
            checkingIntent()
            setupBluetoothAdapter()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.text_dialog_permission),
                Constants.REQUEST_CODE_BLUETOOTH_PERMISSIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun loadFieldsFromSharedPref() {
        SharedPrefsManager.getInstance().getDevice()?.let {
            currentDevice.text = it.title
        }
    }

    private fun initView() {
        currentDevice = findViewById(R.id.tv_current_device)
        currentServiceData = findViewById(R.id.tv_current_service_data)
        currentActiveService = findViewById(R.id.tv_current_active_service)

        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)

        btnStart.setOnClickListener {
            sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE)
        }

        btnStop.setOnClickListener {
            sendCommandToService(Constants.ACTION_STOP_SERVICE)
        }
    }

    private fun subscribeToObserver() {
        BluetoothService.isScanning.observe(this, Observer {
            currentActiveService.text = it.toString()
        })
        BluetoothService.scanningData.observe(this, Observer {
            currentServiceData.text = it.toString()
        })
    }

    private fun initRecyclerView() {
        blAdapter = BluetoothDeviceAdapter(listener = ::onClickItemBlDevice)
        val mlayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.apply {
            adapter = blAdapter
            layoutManager = mlayoutManager
        }
    }

    private fun onClickItemBlDevice(blDevice: BlDevice) {
        currentDevice.text = blDevice.title
        SharedPrefsManager.getInstance().saveDevice(blDevice)
    }

    private fun setupBluetoothAdapter() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
        } else {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            val listBlDevices = mutableListOf<BlDevice>()
            pairedDevices?.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address
                listBlDevices.add(BlDevice(deviceName, deviceHardwareAddress))
            }
            blAdapter?.submitList(listBlDevices)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        checkingIntent()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_ENABLE_BT
            && resultCode != RESULT_OK
        ) {
            Toast.makeText(this, "Bluetooth не включен", Toast.LENGTH_LONG).show()
        } else if (requestCode == Constants.REQUEST_ENABLE_BT
            && resultCode == RESULT_OK
        ) {
            checkingIntent()
            setupBluetoothAdapter()
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(this, BluetoothService::class.java).also {
            it.action = action
            startService(it)
        }
}