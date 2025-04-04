import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService


@Composable
fun BluetoothPermissionHandler(
    onPermissionGranted: @Composable (BluetoothAdapter) -> Unit,
    onPermissionDenied: @Composable () -> Unit = {  },
    extraPermissions: Set<String> = emptySet()
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val bluetoothAdapter = context.getSystemService<BluetoothManager>()?.adapter

    // Required permissions based on Android version
    val requiredPermissions = remember {
        getRequiredBluetoothPermissions() + extraPermissions
    }

    var permissionsGranted by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
    }

    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bluetoothEnabled = true
        }
    }

    // Check hardware support
    val bluetoothSupport = remember(packageManager) {
        BluetoothSupport(
            hasClassic = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasLowEnergy = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        )
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    when {
        bluetoothAdapter == null || !bluetoothSupport.hasClassic ->
            MissingFeatureText(text = "Bluetooth is not available on this device")
        !bluetoothSupport.hasLowEnergy ->
            MissingFeatureText(text = "Bluetooth Low Energy is not supported")
        !permissionsGranted -> onPermissionDenied()
        !bluetoothEnabled -> BluetoothDisabledScreen {
            bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        else -> onPermissionGranted(bluetoothAdapter)
    }
}

private data class BluetoothSupport(
    val hasClassic: Boolean,
    val hasLowEnergy: Boolean
)

private fun getRequiredBluetoothPermissions(): Set<String> {
    val permissions = mutableSetOf<String>()

    // Location permissions (required for Android 11 and below)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        permissions.addAll(
            setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Bluetooth permissions based on Android version
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.addAll(
            setOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )
    } else {
        permissions.addAll(
            setOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        )
    }

    return permissions
}

@Composable
private fun DefaultPermissionDeniedContent() {
    Column(
        modifier = Modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

    ) {
        Card(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,

                ) {
                Text(
                    text = "Bluetooth permissions are required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please grant the required permissions in Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BluetoothDisabledScreen(onEnabled: () -> Unit) {
    val result =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onEnabled()
            }
        }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Bluetooth is disabled")
        Button(
            onClick = {
                result.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
        ) {
            Text(text = "Enable")
        }
    }
}

@Composable
private fun MissingFeatureText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
    )
}
