package com.notzeetaa.yakt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import com.notzeetaa.yakt.ui.theme.YaktTest6Theme
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        clearAndCreateCacheFolder(this)

        val viewModel by viewModels<MyViewModel> {
            MyViewModelFactory(DataStoreManager(this))
        }
        val updateViewModel by viewModels<UpdateViewModel>()

        setContent {
            YaktTest6Theme {
                val canUpdate by updateViewModel.canUpdate.collectAsState()
                val updateAvailable by updateViewModel.updateAvailable.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val updateMessageDialog by updateViewModel.updateMessage.collectAsState()
                var showDialog by remember { mutableStateOf(!isDeviceRooted()) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val selectedItem = when (currentRoute) {
                    "home" -> 0
                    "extras" -> 1
                    "information" -> 2
                    else -> 0
                }

                val updateMessage = stringResource(R.string.update_available_snackbar)
                val updateActionLabel = stringResource(R.string.update_action)

                // Composables e LaunchedEffects
                if (showDialog) {
                    RootAccessDialog { showDialog = false }
                }

                LaunchedEffect(Unit) {
                    updateViewModel.checkForUpdate()
                }

                LaunchedEffect(updateAvailable) {
                    if (updateAvailable) {
                        val result = snackbarHostState.showSnackbar(
                            message = updateMessage,
                            actionLabel = updateActionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            updateViewModel.triggerUpdateCheck {
                                showUpdateDialog = true
                            }
                        }
                    }
                }

                if (showUpdateDialog && updateMessageDialog != null) {
                    GeneralDialog(
                        title = stringResource(R.string.script_update_check_title),
                        content = { Text(updateMessageDialog!!) },
                        onDismiss = { showUpdateDialog = false },
                        confirmText = if (canUpdate) stringResource(R.string.update) else stringResource(R.string.ok),
                        onConfirm = if (canUpdate) {
                            {
                                showUpdateDialog = false
                                updateViewModel.updateScripts(context) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else null
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        BottomNavigationBar(
                            selectedItem = selectedItem,
                            updateAvailable = updateAvailable,
                            onItemSelected = { index ->
                                when (index) {
                                    0 -> navController.navigate("home") { launchSingleTop = true }
                                    1 -> navController.navigate("extras") { launchSingleTop = true }
                                    2 -> navController.navigate("information") { launchSingleTop = true }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomePage(viewModel, updateViewModel)
                            }
                            composable("extras") {
                                ExtrasPage(viewModel, context = this@MainActivity)
                            }
                            composable("information") {
                                InformationPage(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isDeviceRooted(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "whoami"))
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        val output = bufferedReader.readLine()
        output == "root"
    } catch (e: Exception) {
        false
    }
}

@Composable
fun RootAccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.root_access_required_title)) },
        text = { Text(stringResource(R.string.root_access_required_text)) },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun BottomNavigationBar(
    selectedItem: Int,
    updateAvailable: Boolean,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        stringResource(R.string.home),
        stringResource(R.string.extras),
        stringResource(R.string.information)
    )

    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Settings, Icons.Filled.Info)
    val unselectedIcons = listOf(Icons.Outlined.Home, Icons.Outlined.Settings, Icons.Outlined.Info)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Box {
                        Icon(
                            imageVector = if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                            contentDescription = item
                        )
                        if (index == 0 && updateAvailable) {
                            Badge(modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}
