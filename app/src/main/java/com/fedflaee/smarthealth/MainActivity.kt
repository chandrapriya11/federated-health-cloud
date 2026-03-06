package com.fedflaee.smarthealth

import android.Manifest
import com.fedflaee.smarthealth.ui.HealthTabs
import android.bluetooth.le.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedflaee.smarthealth.ui.*
import com.fedflaee.smarthealth.ui.theme.SmartHealthTheme
import com.fedflaee.smarthealth.utils.*
import com.fedflaee.smarthealth.viewmodel.HealthViewModel
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val permissionRequestCode = 101
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val hrServiceUuid =
        UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val hrCharUuid =
        UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val cccdUuid =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val heartRateState = mutableStateOf(0)
    private val latitudeState = mutableStateOf(0.0)
    private val longitudeState = mutableStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()
        LocationUtils.init(this)

        Log.d("DEVICE_ID", DeviceIdUtils.getDeviceId(this))
        var registrationComplete by mutableStateOf(false)

        ApiClient.registerDevice(this) {
            Log.d("FL_FLOW", "Registration complete.")
            registrationComplete = true
        }

        setContent {
            SmartHealthTheme {

                var showIntro by remember { mutableStateOf(true) }
                var showWelcome by remember { mutableStateOf(false) }

                when {
                    showIntro -> {
                        IntroScreen(
                            onStartClick = {
                                showIntro = false
                                showWelcome = true
                            }
                        )
                    }

                    showWelcome -> {
                        WelcomeScreen(
                            onGetStartedClick = {
                                showWelcome = false
                            }
                        )
                    }

                    else -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {

                            val healthViewModel: HealthViewModel = viewModel()

                            LaunchedEffect(Unit) {
                                VitalsSimulatorEngine.initializeRandomUser()
                            }

                            LaunchedEffect(registrationComplete) {
                                if (registrationComplete) {

                                    Log.d("FL_FLOW", "Starting FL after registration")

                                    healthViewModel.startSending(this@MainActivity)

                                    ApiClient.getGlobalModel(this@MainActivity) { model ->
                                        LocalModelStorage.saveGlobalModel(
                                            this@MainActivity,
                                            model.weights,
                                            model.bias,
                                            model.round
                                        )
                                    }
                                }
                            }

                            LaunchedEffect(heartRateState.value) {
                                healthViewModel.updateHeartRate(heartRateState.value)
                            }

                            HealthTabs(
                                liveHr = heartRateState.value,
                                latitude = latitudeState.value,
                                longitude = longitudeState.value,
                                healthViewModel = healthViewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // ================= BLE =================

    override fun onResume() {
        super.onResume()

        if (hasPermissions()) {
            startBle()

            LocationUtils.getCurrentLocation { lat, lon ->
                latitudeState.value = lat
                longitudeState.value = lon
                Log.d("LOCATION", "Lat=$lat Lon=$lon")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBle() {

        if (!hasPermissions()) return

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter

        if (bluetoothAdapter == null) {
            Log.e("BLE", "Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLE", "Bluetooth is OFF")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner

        if (scanner == null) {
            Log.e("BLE", "BLE Scanner is NULL")
            return
        }

        Log.d("BLE_SCAN", "Starting scan...")

        val scanCallback = object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                val device = result.device

                if (device.address == "FB:5D:12:63:14:38") {

                    Log.d("BLE_SCAN", "Device found. Connecting...")

                    scanner.stopScan(this)

                    bluetoothGatt =
                        device.connectGatt(
                            this@MainActivity,
                            false,
                            gattCallback
                        )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE_SCAN", "Scan failed: $errorCode")
            }
        }

        scanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE_CONN", "Connected")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE_CONN", "Disconnected")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

            Log.d("BLE", "Services discovered")

            val hrService = gatt.getService(hrServiceUuid)
            if (hrService == null) {
                Log.e("BLE", "HR service not found")
                return
            }

            val hrChar = hrService.getCharacteristic(hrCharUuid)
            if (hrChar == null) {
                Log.e("BLE", "HR characteristic not found")
                return
            }

            val notificationEnabled =
                gatt.setCharacteristicNotification(hrChar, true)

            Log.d("BLE", "Notification enabled: $notificationEnabled")

            val descriptor = hrChar.getDescriptor(cccdUuid)

            if (descriptor != null) {
                descriptor.value =
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                gatt.writeDescriptor(descriptor)
                Log.d("BLE", "Descriptor written")
            } else {
                Log.e("BLE", "CCCD descriptor not found")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

            if (characteristic.uuid == hrCharUuid) {

                val flag = characteristic.value[0].toInt()

                val format =
                    if (flag and 0x01 != 0)
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    else
                        BluetoothGattCharacteristic.FORMAT_UINT8

                val hr =
                    characteristic.getIntValue(format, 1) ?: 0

                heartRateState.value = hr

                Log.d("BLE_HR", "LIVE HR = $hr")
            }
        }
    }
    // ================= PERMISSIONS =================

    private fun hasPermissions(): Boolean {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                    PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                toRequest.toTypedArray(),
                permissionRequestCode
            )
        }
    }
}