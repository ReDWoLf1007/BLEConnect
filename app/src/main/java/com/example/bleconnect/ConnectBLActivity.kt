package com.example.bleconnect

import BluetoothPermissionHandler
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment

import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerDefaults.standardContainerColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bleconnect.classic.ClassicBluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID


@SuppressLint("MissingPermission")
@Composable
fun ConnectGATTSample(onBack: () -> Unit) {
    var selectedDevice by rememberSaveable {
        mutableStateOf<BluetoothDevice?>(null)
    }

    BackHandler {
        onBack()
    }
    var showBluetoothPermissionHandler by remember {
        mutableStateOf(false)
    }
    val classicBluetoothManager = remember { ClassicBluetoothManager() }

    if (showBluetoothPermissionHandler) {
        BluetoothPermissionHandler(
            onPermissionGranted = {
                showBluetoothPermissionHandler = false
            },


            )
    }

    RememberBluetoothStateListener(
        onBluetoothDisabled = {
            showBluetoothPermissionHandler = true
        }
    )

    val isBluetoothEnabled by remember { mutableStateOf(classicBluetoothManager.isBluetoothAvailableAndEnabled()) }

    AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
        if (device == null) {
            FindDevicesScreen(onClose = { showBluetoothPermissionHandler = true } ){
                if (classicBluetoothManager.isBluetoothAvailableAndEnabled()) {
                    selectedDevice = it
                } else {
                    showBluetoothPermissionHandler = true
                }
            }
        } else {
            if (isBluetoothEnabled) {
                ConnectDeviceScreen(
                    device = device,
                    onClose = { selectedDevice = null }
                )
                Log.i("ConnectGATTSample", "ConnectGATTSample: ConnectDeviceScreen called successfully!")
            } else {
                // Instead of just showing a Toast, we show the permission handler
                Log.e("ConnectGATTSample", "Bluetooth not Enabled!")
                showBluetoothPermissionHandler = true
            }
        }
    }
}



// Optional: Add a utility function to check Bluetooth state changes
@Composable
fun RememberBluetoothStateListener(
    onBluetoothDisabled: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    if (state == BluetoothAdapter.STATE_OFF) {
                        onBluetoothDisabled()
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}


@SuppressLint("InlinedApi", "MissingPermission")
@Composable
fun ConnectDeviceScreen(
    device: BluetoothDevice,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // Check if the device is a Classic Bluetooth device or BLE
    when (device.type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> {
            // Classic Bluetooth (SPP / HC-05)
//            Toast.makeText(context, "Detected Classic Bluetooth Device!", Toast.LENGTH_LONG).show()
            Log.i("BluetoothCheck", "Device is Classic Bluetooth (SPP)")
//            ConnectHC05DeviceScreen(device = device, onClose = onClose)
        }

        BluetoothDevice.DEVICE_TYPE_LE, BluetoothDevice.DEVICE_TYPE_DUAL -> {
            // BLE Device (JDY-23 or Other BLE modules)
//            Toast.makeText(context, "Detected BLE Device!", Toast.LENGTH_LONG).show()
            Log.i("BluetoothCheck", "Device is BLE")
            ConnectJDY23DeviceScreen(device = device, onClose = onClose)
        }

        else -> {
            // Unknown Device Type
//            Toast.makeText(context, "Unknown Bluetooth Type!", Toast.LENGTH_LONG).show()
            Log.e("BluetoothCheck", "Unknown device type: ${device.type}")
        }
    }
}

// Helper function to convert hex string to byte array
fun hexToByteArray(hexString: String): ByteArray {
    val hex = hexString.replace(" ", "")
    val bytes = ByteArray(hex.length / 2)

    for (i in bytes.indices) {
        bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return bytes
}

fun bytesToBits(byteArray: ByteArray): String {
    return byteArray.joinToString("") { byte ->
        String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
    }
}

fun getBitAtIndex(bitString: String, index: Int): Char? {
    return if (index in bitString.indices) bitString[index] else null
}

fun convertLastTwoBytesToDecimal(bytes: ByteArray): Int {
    if (bytes.size < 5) return 0
//    val lastTwo = bytes.takeLast(2)
    return (bytes[3].toInt() and 0xFF shl 8) or (bytes[4].toInt() and 0xFF)
}

fun convertToFloat(value: Int): Float {
    val divisor = 10.0f
    return value / divisor
}


@SuppressLint("MissingPermission")
@Composable
fun ConnectJDY23DeviceScreen(device: BluetoothDevice, onClose: () -> Unit) {

    BackHandler {
        // Do nothing when the back button is pressed
    }

    // Get the Activity context from LocalContext
    val activity = LocalContext.current as Activity

    val scope = rememberCoroutineScope()

//    **************************** Live Table Data Variables **************************

    val tableDataLive = remember {
        mutableStateListOf<List<String>>().apply {
            addAll(loadTableFromFileLive(activity,device))
        }
    }

    val quickHexValues = listOf(

        "AB BB 31 00 00",
        "AB BB 32 00 00",
        "AB BB 33 00 00",
        "AB BB 34 00 00",
        "AB BB 35 00 00",
        "AB BB 36 00 00",
        "AB BB 37 00 00",
        "AB BB 38 00 00",
        "AB BB 39 00 00",
    ) // Static hex values

    var panelVoltStatLive by remember { mutableStateOf(false) }
    var panelCapacityStatLive by remember { mutableStateOf(false) }
    var gridChargingStatLive by remember { mutableStateOf(false) }
    var batteryStatLive by remember { mutableStateOf(false) }
    var loadDetectStatLive by remember { mutableStateOf(false) }
    var loadShortStatLive by remember { mutableStateOf(false) }
    var chargingCVModeStatLive by remember { mutableStateOf(false) }
    var chargingCCModeStatLive by remember { mutableStateOf(false) }
    var loadStatLive by remember { mutableStateOf(false) }

    val showTableDataLive = remember { mutableStateListOf("0.0", "0.0", "0.0", "0.0", "0.0", "0.0","0.0", "0.0") }

    // Using a map to store values at specific indices
    val collectedDataMapLive = mutableMapOf<Int, Float>()

    val collectedDataLive = mutableListOf<List<Float>>() // To collect data for a single row

    fun onSaveCompleteLive(){

        tableDataLive.clear()
        panelVoltStatLive = false
        panelCapacityStatLive = false
        gridChargingStatLive = false
        batteryStatLive = false
        loadDetectStatLive = false
        loadShortStatLive = false
        chargingCCModeStatLive = false
        chargingCVModeStatLive = false
        loadStatLive = false
        Log.i("onSaveCompleteLive", "onSaveCompleteLive inside, triggered")
    }

//    ******************************** Live Table data End *******************************************


//    **********************************Day Data Variables *******************************************
//    val tableData = remember {
//        mutableStateListOf<List<String>>().apply {
//            addAll(loadTableFromFile(activity,device))
//        }
//    }
    val tableData = remember {
        mutableStateListOf<List<String>>()
    }

    var panelVoltStat by remember { mutableStateOf("NORMAL") }
    var panelCapacityStat by remember { mutableStateOf("NORMAL") }
    var gridChargingStat by remember { mutableStateOf("NORMAL") }
    var batteryStat by remember { mutableStateOf("NORMAL") }
    var loadDetectStat by remember { mutableStateOf("NORMAL") }
    var loadShortStat by remember { mutableStateOf("NORMAL") }
    var chargingCVModeStat by remember { mutableStateOf("NORMAL") }
    var chargingCCModeStat by remember { mutableStateOf("NORMAL") }
    var loadStat by remember { mutableStateOf("NORMAL") }

    var faultRead by remember {mutableStateOf(false)}

    var index = 1

    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale("en", "IN"))
    val calendar = Calendar.getInstance()
    val timestampList = remember { mutableListOf<String>() } // Stores timestamps separately

    fun onSaveComplete(){
        tableData.clear()
        timestampList.clear()
        panelVoltStat = ""
        panelCapacityStat = ""
        gridChargingStat = ""
        batteryStat = ""
        loadDetectStat = ""
        loadShortStat = ""
        chargingCCModeStat = ""
        chargingCVModeStat = ""
        loadStat = ""
        index = 1
        Log.i("onSaveComplete", "onSaveComplete inside, triggered")
    }

    val collectedData = mutableListOf<List<Float>>() // To collect data for a single row
    // Using a map to store values at specific indices
    val collectedDataMap = mutableMapOf<Int, Float>()

    // State to track whether to show the table screen
    var showTableScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Keeps track of the connection state
    var connectionState by remember { mutableStateOf(false) }

    // Keeps track of the messages sent and received
    val messageLog = remember { mutableStateListOf<String>() }

    val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    var bluetoothGatt: BluetoothGatt? by remember { mutableStateOf(null) }

    var isCheckingConnection by remember { mutableStateOf(false) }

    var autoScroll by remember { mutableStateOf(true) }

    val classicBluetoothManager = remember { ClassicBluetoothManager() }

    var isReading by remember { mutableStateOf(false) }
    var readFetchData by remember { mutableStateOf(false) }
    var currentMessage by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var loadingScreen by remember { mutableStateOf(false) }

    var isButtonEnabled by remember { mutableStateOf(true) }

    val dataBuffer = remember { mutableListOf<Byte>() } // Buffer to store incoming data

    // New state variables for hex input
    var hexInput by remember { mutableStateOf("") }
    var isHexMode by remember { mutableStateOf(false) }

    var solarEnergy by remember { mutableFloatStateOf(0f) }
    var gridEnergy by remember { mutableFloatStateOf(0f) }
    var consumedEnergy by remember { mutableFloatStateOf(0f) }
    var deviceFaults by remember { mutableFloatStateOf(0f) }

    // Validation function for hex input
    fun isValidHex(input: String): Boolean {
        return input.matches(Regex("^[0-9A-Fa-f\\s]*$"))
    }

    // Theme colors for status
    val connectionColor = if (connectionState) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    fun checkConnection(): Boolean {
        return bluetoothGatt?.let { gatt ->
            val connectionGATTState = bluetoothManager.getConnectionState(gatt.device, BluetoothProfile.GATT)
            if (connectionGATTState == BluetoothProfile.STATE_CONNECTED) {
                // Optionally, try reading a characteristic as a ping check
                val service = gatt.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))

                if (characteristic != null) {
                    gatt.readCharacteristic(characteristic)
                }

                true
            } else {
                Toast.makeText(context, "JDY-23 Disconnected, Reconnect Required", Toast.LENGTH_LONG).show()
                false
            }
        } ?: false
    }



    // Call this function at the end when all reads are done
    fun finalizeTableData() {
        if (timestampList.isEmpty() || tableData.isEmpty()) {
            Log.e("BLE", "Error: No data to finalize. timestampList or tableData is empty.")
            return // Prevent crash
        }

        if (timestampList.size < tableData.size) {
            Log.e("BLE", "Error: timestampList has fewer items than tableData. Adjusting.")
            while (timestampList.size < tableData.size) {
                timestampList.add("Unknown Date") // Fill missing timestamps
            }
        }

        timestampList.reverse() // Reverse only the timestamps

        // Merge reversed timestamps back into tableData
        for (i in tableData.indices) {
            tableData[i] = listOf(timestampList[i]) + tableData[i] // Add reversed timestamp
        }

        messageLog.add("Final tableData with reversed timestamps: $tableData")
        Log.i("FinalDownload Function", "Final tableData with reversed timestamps: $tableData")
    }

    fun connectToDevice() {
        scope.launch(Dispatchers.Main) {
            if (classicBluetoothManager.isBluetoothAvailableAndEnabled()) {
                try {
                    isCheckingConnection = true
                    messageLog.add("Attempting to connect to ${device.address}...")
                    Log.i("Connect function", "Attempting to connect to ${device.address}...")

                    bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                            when (newState) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    messageLog.add("Connected to JDY-23 BLE module")
                                    Log.i("Connect function", "Connected to JDY-23 BLE module")
                                    bluetoothGatt = gatt
                                    connectionState = true
                                    gatt.discoverServices() // Discover services
                                }
                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    messageLog.add("Disconnected from JDY-23 BLE module")
                                    Log.i("Connect function", "disConnected to JDY-23 BLE module")
                                    connectionState = false
                                    bluetoothGatt?.close()
                                    bluetoothGatt = null

                                }
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                messageLog.add("Services discovered on JDY-23")
                                Log.i("onServiceDisc", "Services discovered on JDY-23")
                                val service = gatt.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                                val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))

                                if (characteristic != null) {
                                    messageLog.add("Found JDY-23 characteristic, sending dummy data to prevent disconnect")
                                    Log.i("onServiceDisc", "Found JDY-23 characteristic, sending dummy data to prevent disconnect")
//                                    characteristic.value = byteArrayOf(0x01) // Send a dummy byte
//                                    gatt.writeCharacteristic(characteristic)
//                                    gatt.readCharacteristic(characteristic) // Force read

                                    characteristic.let {
                                        gatt.setCharacteristicNotification(it, true)
                                        val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))
                                        descriptor?.let { d ->
                                            d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                            gatt.writeDescriptor(d)
                                        }
                                    }
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                            Log.i("BLE", "Notification received: ${characteristic.value.joinToString(" ")}")
                            val receivedBytes = characteristic.value
                            Log.i(TAG, "Received Bytes: ${receivedBytes}")


                            // Add new bytes to the buffer
                            dataBuffer.addAll(receivedBytes.toList())

                            Log.i(TAG, "the data phone received: $dataBuffer")

                            val receivedMessage = receivedBytes.toString(Charsets.UTF_8)
                            Log.d(TAG, "Received Message String: $receivedMessage")
//                            messageLog.add("ReceivedMessage: $receivedMessage")
//                            messageLog.add("Hex Value: $hexValue")

                            // Process in chunks of 5 bytes
                            while (dataBuffer.size >= 5) {
                                val chunk = dataBuffer.take(5) // Take first 5 bytes
                                dataBuffer.subList(0, 5).clear() // Remove processed bytes from buffer

                                processChunk(chunk.toByteArray()) // Process the chunk
                            }
                        }

                        val TAG = "processedChunk"

                        init {
                            // Ensure calendar starts from today
                            calendar.time = Date()
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                        }

                        // Function to process each 5-byte chunk
                        private fun processChunk(chunk: ByteArray) {

                            Log.d("BLE", "Processing 5-byte chunk: $chunk")
                            val receivedHex = chunk.joinToString(" ") { "%02X".format(it) }
                            Log.i("BLE", "Processed 5 byte chunk in HEX: $receivedHex")

                            // Perform your data extraction here
                            // Update data based on receivedBytes chunk

                            if(!readFetchData){
                                //   ******************** LIVE DATA ANALYSIS *****************************

                                when (chunk[2]) {
//                                    0x30.toByte() -> collectedDataMapLive[0] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x31.toByte() -> collectedDataMapLive[0] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x32.toByte() -> collectedDataMapLive[1] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x33.toByte() -> collectedDataMapLive[2] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x34.toByte() -> collectedDataMapLive[3] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x35.toByte() -> collectedDataMapLive[4] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x36.toByte() -> collectedDataMapLive[5] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x37.toByte() -> collectedDataMapLive[6] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                    0x38.toByte() -> {
                                        var bitString = bytesToBits(byteArrayOf(chunk[3], chunk[4]))
                                        Log.d("BitCoversion", "Received Data in Bits: $bitString")
                                        Log.i("bitAtIndex", "Bit at index 2: ${getBitAtIndex(bitString, 2)}")
                                        Log.i("bitAtIndex", "Bit at index 13: ${getBitAtIndex(bitString, 13)}")
                                        Log.i("bitAtIndex", "Bit at index 1: ${getBitAtIndex(bitString, 1)}")
                                        bitString = bitString.reversed()
                                        Log.d("bitReversed", "BitString reversed: $bitString")
                                        panelVoltStatLive = getBitAtIndex(bitString, 2) == '1'
                                        panelCapacityStatLive = getBitAtIndex(bitString, 3) == '1'
                                        gridChargingStatLive = getBitAtIndex(bitString, 4) == '1'
                                        batteryStatLive = getBitAtIndex(bitString, 5) == '1'
                                        loadDetectStatLive = getBitAtIndex(bitString, 6) == '1'
                                        loadShortStatLive = getBitAtIndex(bitString, 7) == '1'
                                        chargingCVModeStatLive = getBitAtIndex(bitString, 12) == '1'
                                        chargingCCModeStatLive = getBitAtIndex(bitString, 13) == '1'
                                        loadStatLive = getBitAtIndex(bitString, 9) == '1'
                                    }
                                    0x39.toByte() -> collectedDataMapLive[7] = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                }

                                Log.i("when(chunk[2]", "collectedDataMapLive: $collectedDataMapLive")

                                if (collectedDataMapLive.size == 8) {

                                    // Adding each float as its own single-element list
                                    collectedDataLive.clear()
                                    for (key in collectedDataMapLive.keys.sorted()) {  // Sort keys before accessing
                                        collectedDataLive.add(listOf(collectedDataMapLive[key] ?: 0.0F))
                                    }

                                    Log.d("charRead", "collectedDataMapLive: $collectedDataMapLive")
                                    val timestamp = SimpleDateFormat("d/M/yy HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis())
                                    tableDataLive.add(listOf(timestamp) + collectedDataLive.flatten().map { it.toString() })
                                    Log.d("CharRead", "table Data: $tableDataLive")
                                    showTableDataLive.clear()
                                    showTableDataLive.addAll(collectedDataLive.flatten().map { it.toString() })
                                    collectedDataMapLive.clear()
                                    saveTableToFileLive(activity, generateTableDataLive(tableDataLive), device)
                                }
                            }else {
                                // ******************dAY WISE dATA ANALYSIS ***************************

                                when (chunk[1]) {
                                    0xBA.toByte() -> {
                                        val solarEnergy = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                        collectedDataMap[0] = solarEnergy
//                                    messageLog.add("Solar Energy $index: $solarEnergy")
                                        Log.i(TAG, "Solar Energy $index : $solarEnergy")
                                    }
                                    0xBB.toByte() -> {
                                        val gridEnergy = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                        collectedDataMap[1] = gridEnergy
//                                    messageLog.add("Grid Energy $index: $gridEnergy")
                                        Log.i(TAG, "Grid Energy $index : $gridEnergy")
                                    }
                                    0xBC.toByte() -> {
                                        val consumedEnergy = convertToFloat(convertLastTwoBytesToDecimal(chunk))
                                        collectedDataMap[2] = consumedEnergy
//                                    messageLog.add("Consumed Energy $index: $consumedEnergy")
                                        Log.i(TAG, "Consumed Energy $index : $consumedEnergy")
                                    }
                                    0xBD.toByte() -> {
                                        val bitString = bytesToBits(byteArrayOf(chunk[4], chunk[3]))
                                        panelVoltStat = if (getBitAtIndex(bitString, 2) == '1') "HIGH" else "NORMAL"
//                                    panelCapacityStat = getBitAtIndex(bitString, 3) == '1'
//                                    gridChargingStat = getBitAtIndex(bitString, 4) == '1'
                                        batteryStat = if (getBitAtIndex(bitString, 5) == '1') "HIGH" else "LOW"
                                        loadDetectStat = if (getBitAtIndex(bitString, 6) == '1') "OVER LOAD" else "NORMAL"
                                        loadShortStat = if (getBitAtIndex(bitString, 7) == '1') "SHORT" else "NORMAL"
//                                    chargingCVModeStat = getBitAtIndex(bitString, 12) == '1'
//                                    chargingCCModeStat = getBitAtIndex(bitString, 13) == '1'
//                                    loadStat = getBitAtIndex(bitString, 15) == '1'
                                        faultRead = true

                                    }
                                }

                                Log.i("when(chunk[1]", "collectedDataMap: $collectedDataMap")

                                if(collectedDataMap.size == 3 && faultRead){
                                    // Adding each float as its own single-element list
                                    collectedData.clear()
                                    for (key in collectedDataMap.keys.sorted()) {
                                        collectedData.add(listOf(collectedDataMap[key] ?: 0.0F))
                                    }

                                    // Get current timestamp and decrement for the next read
                                    val timestamp = dateFormat.format(calendar.time)
                                    timestampList.add(timestamp) // Store separately
                                    Log.d("BLE", "Generated Timestamp: $timestamp")
                                    Log.d("BLE", "Generated TimestampList: $timestampList")
                                    calendar.add(Calendar.DATE, -1) // Move to the previous day for next read
                                    Log.i(TAG, "timestamp: $timestamp")

//                                messageLog.add("CollectedDataMap $index: $collectedDataMap")
                                    Log.i(TAG, "CollectedDataMap $index: $collectedDataMap")
                                    collectedDataMap.clear()

//                                messageLog.add("collectedData $index: $collectedData")
                                    Log.i(TAG, "CollectedData $index: $collectedData")
                                    tableData.add(collectedData.flatten().map { it.toString() } + panelVoltStat + batteryStat + loadDetectStat + loadShortStat)
                                    faultRead = false
//                                messageLog.add("tableData Now $index: $tableData")
                                    Log.i(TAG, "tabledata Now $index: $tableData")

//                                messageLog.add("Reached Here")
                                    Log.i(TAG, "Reached Here")
                                    index = index + 1

                                }
                            }
                        }

                        override fun onCharacteristicWrite(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            status: Int
                        ) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
//                                messageLog.add("Write successful: Prevented auto-disconnect")
                                Log.i("CharacterWrite", "Write successful: Prevented auto-disconnect")
                            }
                        }

                        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.i("DescriptorWrite", "Descriptor written successfully")
                            } else {
                                Log.e("DescriptorWrite", "Failed to write descriptor, status: $status")
                            }
                        }


                    })

                } catch (e: Exception) {
                    messageLog.add("Connection failed: ${e.message}")
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                    connectionState = false
                } finally {
                    isCheckingConnection = false
                }
            }
        }
    }



    fun intiateDownloadingData(){
        scope.launch(Dispatchers.Main){
            Log.i("Download function", "Enter finalizeTableData()")
            finalizeTableData()
            Log.i("Download function", "Exit finalizeTableData()")
            delay(200)
            val timestamp = SimpleDateFormat("d/M/yy HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            saveDataToDownloads(activity, "${device.name}_data_$timestamp.csv", generateTableData(tableData), device,
                { onSaveComplete() })
            delay(2000)
            showDialog = false
            loadingScreen = false
        }
    }


    fun disconnectDevice() {
        scope.launch(Dispatchers.IO) {
            try {
                bluetoothGatt?.disconnect()  // Request disconnection
                delay(500)  // Short delay to ensure proper disconnection
                bluetoothGatt?.close()  // Close GATT connection
                bluetoothGatt = null  // Avoid memory leaks

                connectionState = false
                // Toggle reading state
                messageLog.add("Disconnected from JDY-23")
            } catch (e: Exception) {
                messageLog.add("Disconnection failed: ${e.message}")
            }
        }
    }


    fun sendMessage() {
        scope.launch(Dispatchers.IO) {
            try {
                if (hexInput.isNotEmpty() && isValidHex(hexInput)) {
                    val hexBytes = hexToByteArray(hexInput)

                    val service = bluetoothGatt?.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))

                    if (characteristic != null) {
                        characteristic.value = hexBytes
                        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // or WRITE_TYPE_NO_RESPONSE

                        val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
                        if (success) {
                            messageLog.add("Sent HEX: $hexInput")
                            messageLog.add("Sent HEX: $hexBytes")
                        } else {
                            messageLog.add("Failed to send HEX data")
                        }
                    } else {
                        messageLog.add("BLE characteristic not found")
                    }

                    hexInput = ""
                }
                else {
                    if (currentMessage.isNotEmpty()) {
                        try {
//                            val decimalValue = currentMessage.toInt() // Convert string to integer
                            val messageBytes = currentMessage.toByteArray(Charsets.UTF_8) // Convert integer to byte array

                            // Get the writable characteristic (JDY-23 uses UUID FFE1 for TX)
                            val service = bluetoothGatt?.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                            val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))

                            if (characteristic != null) {
                                characteristic.value = messageBytes
                                bluetoothGatt?.writeCharacteristic(characteristic)  // Write to BLE characteristic
                                messageLog.add("Sent TEXT: $currentMessage")
                                currentMessage = "" // Clear after sending
                            } else {
                                messageLog.add("Error: Characteristic not found")
                            }
                        } catch (e: Exception) {
                            messageLog.add("Error: Invalid Input, ${e.message}")
                        }
                    }
                }

            } catch (e: IOException) {
                messageLog.add("Send failed: ${e.message}")
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()  // Take full size of parent
//                    .verticalScroll(rememberScrollState())  // Make column scrollable
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enhanced Header Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,  // Centers all content in the Box
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = device.name ?: "Unknown Device",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(connectionColor, CircleShape)
                                )
                                Text(
                                    text = if (connectionState) "Connected" else "Disconnected",
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = connectionColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Background connection checker
                LaunchedEffect(connectionState) {
                    scope.launch(Dispatchers.IO){
                        while (true) {
                            if (connectionState) {
                                val isStillConnected = checkConnection()
                                if (!isStillConnected && connectionState) {
                                    // Connection was lost
                                    connectionState = false
                                    messageLog.add("Connection lost. Device disconnected.")
                                    Log.i("LaunchedEffect conn", "Connection lost. Device disconnected.")
                                    try {
                                        bluetoothGatt?.close()
                                    } catch (e: IOException) {
                                        // Ignore close errors
                                    }
                                    bluetoothGatt = null
                                }
                            }
                            delay(3000) // Check every second
                        }
                    }
                }

                fun sendHexCommand(index: Int, maxRetries: Int = 3) {
                    val command = hexToByteArray(quickHexValues[index])
                    val service = bluetoothGatt?.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
                    val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))

                    if (characteristic == null) {
                        Log.e("sendHexCommand", "BLE characteristic not found")
                        return
                    }

                    characteristic.value = command
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // or WRITE_TYPE_NO_RESPONSE

                    var attempts = 0
                    var success = false

                    while (attempts < maxRetries && !success) {
                        success = bluetoothGatt?.writeCharacteristic(characteristic) == true
                        if (success) {
                            Log.d("sendHexCommand", "Sent HEX: ${command.joinToString { String.format("%02X", it) }}")
                        } else {
                            Log.e("sendHexCommand", "Failed to send HEX data, attempt ${attempts + 1}")
                            attempts++
                            Thread.sleep(200) // Small delay before retrying (adjust as needed)
                        }
                    }

                    if (!success) {
                        Log.e("sendHexCommand", "Failed to send HEX data after $maxRetries attempts")
                    }
                }


                LaunchedEffect(showTableScreen) {
                    scope.launch(Dispatchers.IO){
                        var index = 0
                        while (showTableScreen) {
                            if (connectionState) {
                                delay(700)
                                sendHexCommand(index)
                                index = (index + 1) % quickHexValues.size
                                if (index == 0) {
                                    delay(300)
                                }
                            } else {
                                index = 0 // Reset index when connection is lost
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Connection Button
                    Button(
                        onClick = { if (connectionState) disconnectDevice() else connectToDevice() },
                        modifier = Modifier
                            .padding(10.dp)
                            .width(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!connectionState) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isCheckingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = if (connectionState) "Linked" else "Link")
                        }
                    }

                    Button(
                        onClick = {
                            showTableScreen = !showTableScreen
                        },

                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        enabled = connectionState

                    ) {
                        Text(if(showTableScreen) "Close Table" else "Show Table")
                    }
                }

                @Composable
                fun ImageWithBackgroundColor() {
                    Image(
                        painter = painterResource(id = R.raw.bl_device),
                        contentDescription = "",
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Transparent)
                            .padding(20.dp)
                    )
                }


                if(showTableScreen){
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (connectionState) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.LightGray),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(0.5f)
                                            .border(1.dp, Color.Gray)
                                            .background(Color.LightGray),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.Gray,
                                                    shape = RectangleShape
                                                )
                                                .background(
                                                    color = Color(0xFFBDB0FA),
                                                    shape = RectangleShape
                                                ),
                                            contentAlignment = Alignment.Center // Centers text vertically and horizontally
                                        ) {
                                            Text(
                                                text = "Attribute",
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(0.5f)
                                            .border(1.dp, Color.Gray)
                                            .background(Color.LightGray),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.Gray,
                                                    shape = RectangleShape
                                                )
                                                .background(
                                                    color = Color(0xFFBDB0FA),
                                                    shape = RectangleShape
                                                ),
                                            contentAlignment = Alignment.Center // Centers text vertically and horizontally
                                        ) {
                                            Text(
                                                text = "DATA",
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.Gray)
                                        .background(Color.LightGray),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(0.5f)
                                            .border(1.dp, Color.Gray)
                                            .background(Color.LightGray),

                                        ) {
                                        listOf("Solar Energy(J)", "Solar Voltage(V)", "Solar Current(A)", "Battery Voltage(V)",
                                            "Battery Current(A)", "Load Current(A)", "Grid Current(A)", "Consumed Energy(J)").forEach { header ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.Gray,
                                                        shape = RectangleShape
                                                    )
                                                    .background(
                                                        color = Color(0xFFFFFFFF),
                                                        shape = RectangleShape
                                                    ),
                                                contentAlignment = Alignment.Center // Centers text vertically and horizontally
                                            ) {
                                                Text(
                                                    text = header,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Clip,
                                                    color = Color.Black
                                                )
                                            }
                                        }

                                    }
                                    Column(

                                        modifier = Modifier
                                            .weight(0.5f) // Allow the table to take up Half the space
                                            .padding(top = 0.dp),
                                    ) {
                                        showTableDataLive.forEach { rowData ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp)
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.Gray,
                                                        shape = RectangleShape
                                                    )
                                                    .background(
                                                        color = Color(0xFFFFFFFF),
                                                        shape = RectangleShape
                                                    ),
                                                contentAlignment = Alignment.Center // Centers text vertically and horizontally
                                            ) {
                                                Text(
                                                    text = rowData,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Clip,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.Black,
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Color(0xFFBDB0FA)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.Black,
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.Absolute.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Text(
                                            text = "STATUS:",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){

                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "GRID CHARGING",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )

                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,
                                        )

                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (!gridChargingStatLive) Color(
                                                        0xffff7500
                                                    ) else Color(0xFF1AFF00)
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (gridChargingStatLive) "ON" else "OFF",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }

                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "LOAD",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,

                                            )
                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,

                                            )
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (!loadStatLive) Color(0xffff7500)
                                                    else Color(
                                                        0xFF1AFF00
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (loadStatLive) "ON" else "OFF",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }

                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "Mode",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }


                                        VerticalDivider(
                                            thickness = 2.dp,
                                        )
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (!chargingCCModeStatLive && !chargingCVModeStatLive) Color(0xffff7500)
                                                    else Color(
                                                        0xFF1AFF00
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (chargingCCModeStatLive) "CC Charging" else if(chargingCVModeStatLive) "CV Charging"  else "NOT Charging",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Color(0xFFBDB0FA)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.Black,
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.Absolute.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Text(
                                            text = "FAULTS:",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center

                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){

                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "BATTERY",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,

                                            )
                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,

                                            )
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (batteryStatLive) Color(0xffff7500)
                                                    else Color(
                                                        0xFF1AFF00
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (batteryStatLive) "LOW" else "NORMAL",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }

                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){

                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "PANEL VOLTAGE",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,

                                            )
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (panelVoltStatLive) Color(0xffff7500)
                                                    else Color(0xFF1AFF00)
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (panelVoltStatLive) "HIGH" else "NORMAL",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "PANEL CAPACITY",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,

                                            )
                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,

                                            )
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (panelCapacityStatLive) Color(
                                                        0xffff7500
                                                    ) else Color(
                                                        0xFF1AFF00
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (panelCapacityStatLive) "HIGH" else "LOW",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }

                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(standardContainerColor)
                                            .shadow(1.dp, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = Color.White
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ){
                                            Text(
                                                text = "LOAD",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,

                                            )
                                        }

                                        VerticalDivider(
                                            thickness = 2.dp,
                                        )

                                        // Wrap Text in Box to center it
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(
                                                    color = if (loadDetectStatLive || loadShortStatLive) Color(
                                                        0xffff7500
                                                    ) else Color(0xFF1AFF00)
                                                ),
                                            contentAlignment = Alignment.Center // Center text vertically
                                        ) {
                                            Text(
                                                text = if (loadDetectStatLive) "OVER LOAD" else if (loadShortStatLive) "SHORT CIRCUIT" else "OK",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }

                                }

                            }else{
                                saveTableToFileLive(activity, generateTableDataLive(tableDataLive), device)
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "☝\uFE0F Link with the Device",
                                                fontWeight = FontWeight.ExtraBold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "Device Not connected to Bluetooth",
                                                style = MaterialTheme.typography.headlineMedium,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "Waiting for Connection",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(25.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        if(connectionState){

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ){

                                Text(
                                    text = "\uD83D\uDC47 Fetch the Monthly Data",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,

                                )
                            }
                        }
                    }
                }
                else{
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        readFetchData = false
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if(connectionState) "See the Live Data ☝\uFE0F" else "☝\uFE0F Link with the Device",
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            ImageWithBackgroundColor()

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "\uD83D\uDC47 Fetch the Monthly Data",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,

                            )
                        }
                    }
                }



                // Bottom Actions
                Row(

                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    if(loadingScreen){
                        LoadingScreen()
                    }else{
                        Button(
                            onClick = {
                                scope.launch {
                                    isButtonEnabled = false  // Disable button immediately
                                    delay(120000)  // Keep the button disabled for 2 minutes
                                    isButtonEnabled = true  // Re-enable the button
                                }
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val timestamp = SimpleDateFormat("d/M/yy HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                                        saveDataToDownloads(activity, "${device.name}_LIVE_$timestamp.csv", generateTableDataLive(tableDataLive), device, { onSaveCompleteLive() })
                                        delay(300)
                                    }

                                    readFetchData = true
                                    showTableScreen = false

                                    withContext(Dispatchers.IO) {
                                        delay(500)
                                        hexInput = "AB BA 31 00 00"
                                        sendMessage()
                                        delay(300)
                                    }

                                    loadingScreen = true

                                    delay(7000) // Keep it on the Main thread if it’s UI-related

                                    withContext(Dispatchers.IO) {
                                        intiateDownloadingData()
                                    }

                                    readFetchData = false
                                    disconnectDevice()
                                }
                            },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            enabled = isButtonEnabled,

                            ) {
                            Text("Fetch DATA")
                        }
                    }


                    Button(
                        onClick = {
                            disconnectDevice()
                            onClose()

                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}


@Composable
fun LoadingScreen() {

    Dialog(onDismissRequest = { /* Prevent dismissing */ }) {
        Box(
            modifier = Modifier.padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Download Please wait...")
                }
            }

        }
    }

}


// Function to generate table data in CSV format
private fun generateTableDataLive(tableDataLive: List<List<String>>): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("TimeStamp, Solar Energy(J), Solar Voltage(V), Solar Current(A), Battery Voltage(V), Battery Current(A), Load Current(A), Grid Current(A), Consumed Energy(J)\n") // Header row
    for (row in tableDataLive) {
        stringBuilder.append(row.joinToString(", "))
        stringBuilder.append("\n")
    }
    return stringBuilder.toString()
}

@SuppressLint("MissingPermission")
private fun saveDataToFileLive(uri: Uri, data: String, activity: Activity, device: BluetoothDevice, onSaveCompleteLive: () -> Unit) {
    try {
        activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(data)
            }
            Toast.makeText(activity, "DataTable Downloaded", Toast.LENGTH_LONG).show()
            deleteTableSaveFileLive(activity, device)
            onSaveCompleteLive()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(activity, "Failed to save File: $e", Toast.LENGTH_LONG).show()
    }
}

@SuppressLint("MissingPermission")
private fun deleteTableSaveFileLive(activity: Activity, device: BluetoothDevice){
    // Delete the file after successful save
    val file = activity.getFileStreamPath("${device.name}_LIVE_table_data.csv")
    if (file.exists()) {
        file.delete()
//        Toast.makeText(activity, "Local file Erased", Toast.LENGTH_SHORT).show()
        Log.i("deletelocalfileLive", "Local file Erased for LIVE")
    }
}

@SuppressLint("MissingPermission")
private fun saveTableToFileLive(activity: Activity, data: String, device: BluetoothDevice) {
    try {
        activity.openFileOutput("${device.name}_LIVE_table_data.csv", Activity.MODE_PRIVATE).use { outputStream ->
            outputStream.write(data.toByteArray(Charsets.UTF_8))
        }
//        Toast.makeText(activity, "Table saved successfully", Toast.LENGTH_SHORT).show()
        Log.i("saveTabletoFile", "Table saved successfully")

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(activity, "Failed to save table: $e", Toast.LENGTH_LONG).show()
        Log.w("SaveTableError", "saveTableToFileLive: Failed to save table: $e")
    }
}

//Live Table
@SuppressLint("MissingPermission")
private fun loadTableFromFileLive(activity: Activity, device: BluetoothDevice): List<List<String>> {
    val savedData = mutableListOf<List<String>>()
    try {
        val file = activity.getFileStreamPath("${device.name}_LIVE_table_data.csv")
        if (!file.exists()) {
            Log.w("FileRead", "File does not exist. Returning empty table.")
            return emptyList() // Prevent error when file is missing
        }

        val fileInputStream = activity.openFileInput("${device.name}_LIVE_table_data.csv")
        val data = fileInputStream.bufferedReader().use { it.readLines() }

        for (row in data.drop(1)) { // Drop the header row
//            savedData.add(row.split(", "))
            val splitRow = row.split(",").map { it.trim() } // Trim to remove leading/trailing spaces
            Log.d("Debug", "Row: $row, Split: $splitRow")
            // Ensure all fields are valid, preventing NumberFormatException
            if (splitRow.size == 9 && splitRow.all { it.isNotEmpty() }) {
                savedData.add(splitRow)
            } else {
                Log.e("DataError", "Malformed row detected: $row")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("LoadTableError", "Failed to load table: ${e.message}")
    }
    return savedData
}


// Function to generate table data in CSV format
private fun generateTableData(tableData: List<List<String>>): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("TimeStamp, Solar Energy, Grid Energy, Consumed Energy, Panel Fault, Battery Fault, Load Fault, Circuit Fault\n") // Header row
    for (row in tableData) {
        stringBuilder.append(row.joinToString(", "))
        stringBuilder.append("\n")
    }
    return stringBuilder.toString()
}

@SuppressLint("MissingPermission")
private fun saveDataToDownloads(
    activity: Activity,
    fileName: String,
    data: String,
    device: BluetoothDevice,
    onSaveComplete: () -> Unit
) {
    val resolver = activity.contentResolver

    // Generate folder name based on current date (YYYY-MM-DD)
    val folderName = "${device.name} ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "csv")
        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        try {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(data.toByteArray())
//                Toast.makeText(context, "File saved in Downloads", Toast.LENGTH_LONG).show()
                Log.i("SaveDataToDownloads", "File saved in Downloads")
                deleteTableSaveFileLive(activity, device)
                onSaveComplete()
                Log.i("saveDownloads", "onSaveComplete triggered!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
//            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("SaveDataToDownloads", "Failed to save file: ${e.message}")
        }
    } ?: Log.e("SaveDataToDownloads", "Failed to create file URI")
}


@SuppressLint("MissingPermission")
private fun deleteTableSaveFile(activity: Activity, device: BluetoothDevice){
    // Delete the file after successful save
    val file = activity.getFileStreamPath("${device.name}_LIVE_table_data.csv")
    if (file.exists()) {
        file.delete()
//        Toast.makeText(activity, "Local file Erased", Toast.LENGTH_SHORT).show()
        Log.i("deletelocalfile", "Local file Erased")
    }
}

@SuppressLint("MissingPermission")
private fun saveTableToFile(activity: Activity, data: String, device: BluetoothDevice) {
    try {
        activity.openFileOutput("${device.name}_LIVE_table_data.csv", Activity.MODE_PRIVATE).use { outputStream ->
            outputStream.write(data.toByteArray(Charsets.UTF_8))
        }
//        Toast.makeText(activity, "Table saved successfully", Toast.LENGTH_SHORT).show()
        Log.i("saveTableToFile", "Table saved successfully")

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(activity, "Failed to save table: $e", Toast.LENGTH_LONG).show()
        Log.e("SaveTableError", "saveTableToFile: Failed to save table: $e")
    }
}

@SuppressLint("MissingPermission")
private fun loadTableFromFile(activity: Activity, device: BluetoothDevice): List<List<String>> {
    val savedData = mutableListOf<List<String>>()
    try {
        val file = activity.getFileStreamPath("${device.name}_LIVE_table_data.csv")
        if (!file.exists()) {
            Log.w("FileRead", "File does not exist. Returning empty table.")
            return emptyList() // Prevent error when file is missing
        }

        val fileInputStream = activity.openFileInput("${device.name}_LIVE_table_data.csv")
        val data = fileInputStream.bufferedReader().use { it.readLines() }

        for (row in data.drop(1)) { // Drop the header row
//            savedData.add(row.split(", "))
            val splitRow = row.split(",").map { it.trim() } // Trim to remove leading/trailing spaces
            Log.d("Debug", "Row: $row, Split: $splitRow")
            // Ensure all fields are valid, preventing NumberFormatException
            if (splitRow.size == 8 && splitRow.all { it.isNotEmpty() }) {
                savedData.add(splitRow)
            } else {
                Log.e("DataError", "Malformed row detected: $row")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("LoadTableError", "Failed to load table: ${e.message}")
    }
    return savedData
}

@Composable
private fun MessageLogItem(message: String) {
    val isReceived = message.startsWith("Received:")
    val isSent = message.startsWith("Sent:")
    val isHex = message.startsWith("Hex:")

    val backgroundColor = when {
        isReceived -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        isSent -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
        isHex -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isReceived -> MaterialTheme.colorScheme.primary
                isSent -> MaterialTheme.colorScheme.secondary
                isHex -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
