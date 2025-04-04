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

    // ************************************* Logic is removed by the owner because it is getting used ***************************
    //************************************** commercially, but if anyone wants to learn about it, they **************************
    //************************************** can email them here: sagar.tiwawri.work@gmail.com cheers! **************************
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
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun saveDataToFileLive(uri: Uri, data: String, activity: Activity, device: BluetoothDevice, onSaveCompleteLive: () -> Unit) {
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun deleteTableSaveFileLive(activity: Activity, device: BluetoothDevice){
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun saveTableToFileLive(activity: Activity, data: String, device: BluetoothDevice) {
   //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

//Live Table
@SuppressLint("MissingPermission")
private fun loadTableFromFileLive(activity: Activity, device: BluetoothDevice): List<List<String>> {
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}


// Function to generate table data in CSV format
private fun generateTableData(tableData: List<List<String>>): String {
   //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun saveDataToDownloads(
    activity: Activity,
    fileName: String,
    data: String,
    device: BluetoothDevice,
    onSaveComplete: () -> Unit
) {
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}


@SuppressLint("MissingPermission")
private fun deleteTableSaveFile(activity: Activity, device: BluetoothDevice){
   //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun saveTableToFile(activity: Activity, data: String, device: BluetoothDevice) {
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
}

@SuppressLint("MissingPermission")
private fun loadTableFromFile(activity: Activity, device: BluetoothDevice): List<List<String>> {
    //*********************** In Commercial use *****************************
    //********************** Logic not available ****************************
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
