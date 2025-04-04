package com.example.bleconnect


import BluetoothPermissionHandler
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bleconnect.ui.theme.BLEConnectTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConnectViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState = _uiState.asStateFlow()

    data class ConnectUiState(
        var showGATTScreen: Boolean = false,
        // Add other state properties here as needed
    )

    fun showGATTScreen() {
        _uiState.update { currentState ->
            currentState.copy(showGATTScreen = true)
        }
    }

    fun hideGATTScreen() {
        _uiState.update { currentState ->
            currentState.copy(showGATTScreen = false)
        }
    }
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // Do nothing when back is pressed
//            }
//        })

        setContent {

            // Declare a MutableState for theme toggle
            val isDarkTheme = remember { mutableStateOf(false) }

            BLEConnectTheme(isDarkTheme.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(isDarkTheme)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isDarkTheme: MutableState<Boolean>) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val currentScreenTitle = getScreenTitle(navController)
                    Text(
                        text = currentScreenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Theme toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (isDarkTheme.value) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        // Customized Switch with Icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(60.dp, 40.dp) // Adjust dimensions as needed
                                .border(
                                    width = 1.dp,
                                    color = Color.Gray,
                                    shape = RoundedCornerShape(15.dp)
                                )
                                .background(color = if (isDarkTheme.value) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(15.dp))
                                .clickable { isDarkTheme.value = !isDarkTheme.value }
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme.value) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkTheme.value) Color.White else Color.Black,
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(if (isDarkTheme.value) Alignment.CenterEnd else Alignment.CenterStart)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )


            )
        },

        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavigationHost(navController)
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "connect") {
        composable("home") {  }
        composable("connect") {

            BackHandler {
                // Do nothing when the back button is pressed
            }
            val connectViewModel: ConnectViewModel = viewModel()
            val uiState by connectViewModel.uiState.collectAsState()

            if (uiState.showGATTScreen) {
                BluetoothPermissionHandler(
                    onPermissionGranted = {
                        ConnectGATTSample(onBack = { connectViewModel.hideGATTScreen() })
                    },
                )
            } else {
                ConnectScreen(
                    onScanClick = {
                        connectViewModel.showGATTScreen()
                    }
                )
            }
        }
        composable("settings") { }
    }
}
@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomAppBar{
//
        NavigationBarItem(
            selected = currentRoute == "connect",
            onClick = {
                if (currentRoute != "connect") {
                navController.navigate("connect") {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } },
            icon = { Icon(Icons.Filled.Bluetooth, contentDescription = "Connect") },
            label = { Text("Connect") },

        )
    }
}

@SuppressLint("ResourceType")
@Composable
fun ConnectScreen(onScanClick: () -> Unit) {

    Column(
        modifier = Modifier
            .padding(top = 30.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Solar Panel Reading",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
//        Row {
//            Image(
//                painter = painterResource(id = R.raw.tib_logo),
//                contentDescription = "TIB LOGO",
//                modifier = Modifier
//                    .size(35.dp)
//                    .background(Color.Transparent)
//                    .padding(5.dp)
//            )
//            Text(
//                text = "TIBE Innovations PVT. LTD.",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.secondary,
//                modifier = Modifier.padding(6.dp)
//            )
//        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Scan for BLE Devices",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.padding(6.dp))
        Button(
            onClick = onScanClick,
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        ) {
            Text(
                text = "Scan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "Discover Bluetooth Devices",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Composable
fun getScreenTitle(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return when (navBackStackEntry?.destination?.route) {
        "home" -> "Home"
        "connect" -> "BLEConnect"
        "settings" -> "Settings"
        else -> "connect"
    }
}
