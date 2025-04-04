package com.example.bleconnect.classic


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ClassicBluetoothManager {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val CONNECTION_TIMEOUT: Long = 10000 // 10 seconds timeout for connection
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailableAndEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(
        device: BluetoothDevice,
        onConnectionSuccess: () -> Unit = {},
        onConnectionFailure: (String) -> Unit = {}
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                )

                // Set connection timeout
                withTimeout(10000) { // 10 seconds timeout
                    socket.connect()
                }

                bluetoothSocket = socket
                if (bluetoothSocket?.isConnected == true) {
                    onConnectionSuccess()
                    true
                } else {
                    throw IOException("Socket connection failed")
                }
            } catch (e: IOException) {
                onConnectionFailure(e.message ?: "Unknown error")
                bluetoothSocket?.close()
                bluetoothSocket = null
                false
            }
        }
    }

    /**
     * Disconnect from the device
     */
    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d("ClassicBluetoothManager", "Disconnected")
        } catch (e: IOException) {
            Log.e("ClassicBluetoothManager", "Error during disconnection: ${e.message}")
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
        }
    }
}
