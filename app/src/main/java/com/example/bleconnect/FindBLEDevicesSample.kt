
package com.example.bleconnect


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected

import androidx.compose.material3.Card
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bleconnect.classic.ClassicBluetoothManager
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")

@Composable
internal fun FindDevicesScreen(onClose: () -> Unit, onConnect: (BluetoothDevice) -> Unit) {
    val context = LocalContext.current
    val adapter = checkNotNull(context.getSystemService<BluetoothManager>()?.adapter)
    var scanning by remember { mutableStateOf(true) }
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    val pairedDevices = remember { mutableStateListOf<BluetoothDevice>(*adapter.bondedDevices.toTypedArray()) }
    val classicBluetoothManager = remember { ClassicBluetoothManager() }
    val scope = rememberCoroutineScope()

    if(classicBluetoothManager.isBluetoothAvailableAndEnabled()){
        if (scanning) {

            LaunchedEffect(Unit) {
                scope.launch(Dispatchers.IO) {
                    devices.clear()
                    var isReceiverRegistered = false // Track registration state

                    val discoveryReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                BluetoothDevice.ACTION_FOUND -> {
                                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                    if (device != null && !devices.contains(device)) {
                                        devices.add(device)
                                    }
                                }
                                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                    scanning = false
                                    if (isReceiverRegistered) {
                                        context.unregisterReceiver(this)
                                        isReceiverRegistered = false
                                    }
                                }
                            }
                        }
                    }

                    val filter = IntentFilter().apply {
                        addAction(BluetoothDevice.ACTION_FOUND)
                        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    }

                    // Register the receiver and mark it as registered
                    context.registerReceiver(discoveryReceiver, filter)
                    isReceiverRegistered = true

                    try {
                        adapter.startDiscovery()
                        delay(20000) // Timeout for scanning
                    } finally {
                        if (adapter.isDiscovering) {
                            adapter.cancelDiscovery()
                        }
                        scanning = false
                        // Unregister only if it was registered
                        if (isReceiverRegistered) {
                            context.unregisterReceiver(discoveryReceiver)
                            isReceiverRegistered = false
                        }
                    }
                }
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bluetooth Scanner",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (scanning) "Scanning for devices..." else "Scan complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        IconButton(
                            onClick = {
                                devices.clear()
                                scanning = true
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Devices List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (devices.isEmpty() && !scanning) {
                    item {
                        NoDevicesFound()
                    }
                } else {
                    items(devices) { device ->
                        BluetoothDeviceItem(
                            bluetoothDevice = device,
                            isSampleServer = false,
                            onConnect = { handleDeviceConnection(context, device, onConnect) }
                        )
                    }
                }

                if (pairedDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Paired Devices",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(pairedDevices) { device ->
                        BluetoothDeviceItem(
                            bluetoothDevice = device,
                            onConnect = onConnect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoDevicesFound() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(
            text = "No devices found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = "Try moving closer to the device or refresh the scan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@SuppressLint("MissingPermission")
private fun handleDeviceConnection(
    context: Context,
    device: BluetoothDevice,
    onConnect: (BluetoothDevice) -> Unit
) {
    // Check if the device is BLE
    if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
        // BLE device: Directly connect without pairing
//        Toast.makeText(context, "Connecting to BLE device...", Toast.LENGTH_LONG).show()
        onConnect(device)
    } else {
        // Classic Bluetooth: Check if already paired or initiate pairing
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            // Device is already paired, proceed
            onConnect(device)
        } else {
            // Device is not paired, initiate pairing
            val pairingReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)

                    if (bondState == BluetoothDevice.BOND_BONDED && prevBondState != BluetoothDevice.BOND_BONDED) {
                        // Pairing successful
                        context.unregisterReceiver(this)
                        Toast.makeText(context, "Device paired Successfully", Toast.LENGTH_LONG).show()
                        onConnect(device)
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        // Pairing failed
                        context.unregisterReceiver(this)
                        Toast.makeText(context, "Device couldn't pair", Toast.LENGTH_LONG).show()
                    }
                }
            }

            // Register for bond state change events
            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            context.registerReceiver(pairingReceiver, filter)

            // Attempt to create a bond (pair)
            device.createBond()
        }
    }
}



@SuppressLint("MissingPermission")
@Composable
internal fun BluetoothDeviceItem(
    bluetoothDevice: BluetoothDevice,
    isSampleServer: Boolean = false,
    onConnect: (BluetoothDevice) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect(bluetoothDevice) }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (bluetoothDevice.bondState) {
                        BluetoothDevice.BOND_BONDED -> Icons.Rounded.BluetoothConnected
                        BluetoothDevice.BOND_BONDING -> Icons.Rounded.Bluetooth
                        else -> Icons.AutoMirrored.Rounded.BluetoothSearching
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = if (isSampleServer) "GATT Sample server" else (bluetoothDevice.name ?: "Unknown Device"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSampleServer) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = bluetoothDevice.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ChipStatus(bluetoothDevice.bondState)
        }
    }
}

@Composable
private fun ChipStatus(bondState: Int) {
    val (backgroundColor, textColor, text) = when (bondState) {
        BluetoothDevice.BOND_BONDED -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary,
            "Paired"
        )
        BluetoothDevice.BOND_BONDING -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.tertiary,
            "Pairing..."
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.outline,
            "Not Paired"
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}