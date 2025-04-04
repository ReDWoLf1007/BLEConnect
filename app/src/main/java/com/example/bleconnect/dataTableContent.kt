package com.example.bleconnect


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.DrawerDefaults.standardContainerColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

@SuppressLint("MissingPermission")
@Composable
fun TableScreenLive(device: BluetoothDevice, onBack: () -> Unit, activity: Activity) {
    val tableDataLive = remember {
        mutableStateListOf<List<String>>().apply {
            addAll(loadTableFromFileLive(activity,device))
        }
    }

    var panelVoltStatLive by remember { mutableStateOf(false) }
    var panelCapacityStatLive by remember { mutableStateOf(false) }
    var gridChargingStatLive by remember { mutableStateOf(false) }
    var batteryStatLive by remember { mutableStateOf(false) }
    var loadDetectStatLive by remember { mutableStateOf(false) }
    var loadShortStatLive by remember { mutableStateOf(false) }
    var chargingCVModeStatLive by remember { mutableStateOf(false) }
    var chargingCCModeStat by remember { mutableStateOf(false) }
    var loadStatLive by remember { mutableStateOf(false) }

    var bluetoothGatt by remember { mutableStateOf<BluetoothGatt?>(null) }

    val showTableDataLive = remember { mutableStateListOf("0.0", "0.0", "0.0", "0.0", "0.0", "0.0","0.0", "0.0", "0.0") }

    // Using a map to store values at specific indices
    val collectedDataMapLive = mutableMapOf<Int, Float>()

//    var isReading = remember { (true) }
    // Keeps track of the connection state
    var isConnected by remember { mutableStateOf(true) }

    
    fun onSaveComplete(){

        tableDataLive.clear()
        panelVoltStatLive = false
        panelCapacityStatLive = false
        gridChargingStatLive = false
        batteryStatLive = false
        loadDetectStatLive = false
        loadShortStatLive = false
        chargingCCModeStat = false
        chargingCVModeStatLive = false
        loadStatLive = false
    }


    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        uri?.let {
            saveDataToFileLive(
                uri,
                generateTableDataLive(tableDataLive),
                activity,
                device
            ) { onSaveComplete() }
        }
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
        if (bytes.size < 2) return 0
        val lastTwo = bytes.takeLast(2)
        return (lastTwo[0].toInt() and 0xFF shl 8) or (lastTwo[1].toInt() and 0xFF)
    }

    fun convertToFloat(value: Int): Float {
        val divisor = 10.0f
        return value / divisor
    }
    
    val coroutineScope = rememberCoroutineScope()

    val quickHexValues = listOf(
        "AB BB 30 00 00",
        "AB BA 31 00 00",
        "AB BB 32 00 00",
        "AB BB 33 00 00",
        "AB BB 34 00 00",
        "AB BB 35 00 00",
        "AB BB 36 00 00",
        "AB BB 37 00 00",
        "AB BB 38 00 00",
        "AB BB 39 00 00",
//        "AB BB 3A 00 00",
//        "AB BB 3B 00 00",
//        "AB BB 3C 00 00",
    ) // Static hex values

    BackHandler {
        // Do nothing when the back button is pressed
    }

    // Callback for handling BLE connection and data reception
    val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Connected to GATT server. Discovering services...")
                isConnected = true
                bluetoothGatt = gatt
                gatt?.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("BLE", "Disconnected from GATT server.")
                isConnected = false
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("onServicesDisc", "Services discovered on JDY-23")
                Log.i("BLE", "Services discovered:")
                gatt?.services?.forEach { service ->
                    Log.i("BLE", "Service UUID: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        Log.i("BLE", "   Characteristic UUID: ${characteristic.uuid}")
                        service.describeContents()
                    }

                }
                val service = gatt?.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
                val characteristic = service?.getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"))

                if (characteristic != null) {
                    Log.d("onServiceDisc", "Found JDY-23 characteristic, sending dummy data to prevent disconnect")
                    characteristic.value = byteArrayOf(0x01) // Send a dummy byte
                    gatt.writeCharacteristic(characteristic)

                    // Enable notifications for this characteristic
                    gatt.setCharacteristicNotification(characteristic, true)

                    // Find the descriptor that enables notifications
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"))

                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    } else {
                        Log.e("BLE", "Descriptor not found!")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { value ->
                val receivedBytes = value

                Log.d("charRead", "Received Byte: $receivedBytes")


                when (receivedBytes[2]) {
                    0x30.toByte() -> collectedDataMapLive[0] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x31.toByte() -> collectedDataMapLive[1] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x32.toByte() -> collectedDataMapLive[2] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x33.toByte() -> collectedDataMapLive[3] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x34.toByte() -> collectedDataMapLive[4] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x35.toByte() -> collectedDataMapLive[5] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x36.toByte() -> collectedDataMapLive[6] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x37.toByte() -> collectedDataMapLive[7] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                    0x38.toByte() -> {
                        val bitString = bytesToBits(receivedBytes.sliceArray(3..4))
                        panelVoltStatLive = getBitAtIndex(bitString, 2) == '1'
                        panelCapacityStatLive = getBitAtIndex(bitString, 3) == '1'
                        gridChargingStatLive = getBitAtIndex(bitString, 4) == '1'
                        batteryStatLive = getBitAtIndex(bitString, 5) == '1'
                        loadDetectStatLive = getBitAtIndex(bitString, 6) == '1'
                        loadShortStatLive = getBitAtIndex(bitString, 7) == '1'
                        chargingCVModeStatLive = getBitAtIndex(bitString, 12) == '1'
                        chargingCCModeStat = getBitAtIndex(bitString, 13) == '1'
                        loadStatLive = getBitAtIndex(bitString, 15) == '1'
                    }
                    0x39.toByte() -> collectedDataMapLive[8] = convertToFloat(convertLastTwoBytesToDecimal(receivedBytes))
                }
                Log.d("collectedDataMapLive", "collectedDataMapLive: $collectedDataMapLive")

                if (collectedDataMapLive.size == 9) {
                    Log.d("charRead", "collectedDataMapLive: $collectedDataMapLive")
                    val timestamp = SimpleDateFormat("d/M/yy HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
                    tableDataLive.add(listOf(timestamp) + collectedDataMapLive.values.map { it.toString() })
                    Log.d("CharRead", "table Data: $tableDataLive")
                    showTableDataLive.clear()
                    showTableDataLive.addAll(collectedDataMapLive.values.map { it.toString() })
                    collectedDataMapLive.clear()
                    saveTableToFileLive(activity, generateTableDataLive(tableDataLive), device)
                }
            }
        }
    }


    LaunchedEffect(device) {
        bluetoothGatt = device.connectGatt(activity, false, gattCallback)
    }

//    fun sendHexCommand(index: Int) {
//        val command = hexToByteArray(quickHexValues[index])
//        val service = bluetoothGatt?.getService(UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"))
//        val characteristic = service?.getCharacteristic(UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"))
//
//        if (characteristic != null) {
//            characteristic.value = command
//            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // or WRITE_TYPE_NO_RESPONSE
//
//            val success = bluetoothGatt?.writeCharacteristic(characteristic) == true
//            if (success) {
//                Log.d("sendHexCommand", "Sent HEX: $command")
//            } else {
//                Log.e("sendHexCommand", "Failed to send HEX data")
//            }
//        } else {
//            Log.e("sendHexCommand", "BLE characteristic not found")
//        }
//    }
//
//    LaunchedEffect(device) {
//        if (isConnected) {
//            var index = 0
//            while (true) {
//                sendHexCommand(index)
//                delay(500)
//                index = (index + 1) % quickHexValues.size
//            }
//        }
//    }



    // Clean up BLE connection when composable is removed
    DisposableEffect(Unit) {
        onDispose {
            bluetoothGatt?.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isConnected) {

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
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RectangleShape
                            ),
                        contentAlignment = Alignment.Center // Centers text vertically and horizontally
                    ) {
                        Text(
                            text = "Header",
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
                                color = MaterialTheme.colorScheme.secondaryContainer,
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
                    listOf("Grid Energy(J)", "Solar Energy(J)", "Solar Voltage(V)", "Solar Current(A)", "Battery Voltage(V)",
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
                                    color = Color(0xfffff400),
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
                                    color = Color(0xFF76E33E),
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
//            CircularProgressIndicator(
//                modifier = Modifier
//                    .padding(6.dp)
//                    .size(25.dp)
//            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF42A5F5)
                            )
                        )
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
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFF8B07D),
                                    Color(0xFFF8AA63)
                                )
                        ))
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
                    Text(
                        text = "GRID STATUS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                    )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (!gridChargingStatLive) Color(0xffff7500) else Color(
                                    0xFF76E33E
                                )
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
                    Text(
                        text = "LOAD STATUS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                    )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (!loadStatLive) Color(0xffff7500) else Color(0xFF76E33E)
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
                    Text(
                        text = "CV MODE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                        )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (!chargingCVModeStatLive) Color(0xffff7500) else Color(
                                    0xFF76E33E
                                )
                            ),
                        contentAlignment = Alignment.Center // Center text vertically
                    ) {
                        Text(
                            text = if (chargingCVModeStatLive) "Charging" else "NOT Charging",
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
                    Text(
                        text = "CC Mode",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                        )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (!chargingCCModeStat) Color(0xffff7500) else Color(0xFF76E33E)
                            ),
                        contentAlignment = Alignment.Center // Center text vertically
                    ) {
                        Text(
                            text = if (chargingCCModeStat) "Charging" else "NOT Charging",
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
                        .background(
                            Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8B07D),
                                Color(0xFFF8AA63)
                            )
                        ))
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
                    Text(
                        text = "BATTERY",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                    )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (batteryStatLive) Color(0xffff7500) else Color(0xFF76E33E)
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
                    Text(
                        text = "PANEL VOLTAGE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                    )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (panelVoltStatLive) Color(0xffff7500) else Color(0xFF76E33E)
                            ),
                        contentAlignment = Alignment.Center // Center text vertically
                    ) {
                        Text(
                            text = if (panelVoltStatLive) "HIGH" else "LOW",
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
                    Text(
                        text = "PANEL CAPACITY",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                    )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (panelCapacityStatLive) Color(0xffff7500) else Color(
                                    0xFF76E33E
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
                    Text(
                        text = "LOAD",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,

                        )
                    VerticalDivider(
                        thickness = 2.dp,

                    )
                    // Wrap Text in Box to center it
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (loadDetectStatLive || loadShortStatLive) Color(0xffff7500) else Color(
                                    0xFF76E33E
                                )
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
    if(isConnected){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    saveTableToFileLive(activity, generateTableDataLive(tableDataLive), device)
                    onBack()
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Close")
            }
//            OutlinedButton(
//                onClick = {
//                    onSaveComplete()
//                    deleteTableSaveFileLive(activity, device)
//                },
//                modifier = Modifier.padding(8.dp)
//            ) {
//                Text("Clear")
//            }

            Button(
                onClick = {
//                    isReading = false
                    saveFileLauncher.launch("Download_LIVE_table_data.csv")
//                    isReading = true
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Download")
            }
        }
    }
}

// Function to generate table data in CSV format
private fun generateTableDataLive(tableDataLive: List<List<String>>): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("TimeStamp, Panel Voltage(V), Panel Current(A), Battery Voltage(V), Battery Current(A), Load Current(A), Grid Current(A)\n") // Header row
    for (row in tableDataLive) {
        stringBuilder.append(row.joinToString(", "))
        stringBuilder.append("\n")
    }
    return stringBuilder.toString()
}

@SuppressLint("MissingPermission")
private fun saveDataToFileLive(uri: Uri, data: String, activity: Activity, device: BluetoothDevice, onSaveComplete: () -> Unit) {
    try {
        activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(data)
            }
            Toast.makeText(activity, "DataTable Downloaded", Toast.LENGTH_LONG).show()
            deleteTableSaveFileLive(activity, device)
            onSaveComplete()
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
    }
}

@SuppressLint("MissingPermission")
private fun saveTableToFileLive(activity: Activity, data: String, device: BluetoothDevice) {
    try {
        activity.openFileOutput("${device.name}_LIVE_table_data.csv", Activity.MODE_PRIVATE).use { outputStream ->
            outputStream.write(data.toByteArray(Charsets.UTF_8))
        }
        Toast.makeText(activity, "Table saved successfully", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(activity, "Failed to save table: $e", Toast.LENGTH_LONG).show()
        Log.e("SaveTableError", "saveTableToFileLive: Failed to save table: $e")
    }
}

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
            if (splitRow.size == 6 && splitRow.all { it.isNotEmpty() }) {
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
