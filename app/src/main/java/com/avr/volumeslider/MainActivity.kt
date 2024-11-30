package com.avr.volumeslider

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.avr.volumeslider.util.VolumeControlService
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    private var volumeControlService: VolumeControlService? = null
    private var isBound = false



    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startVolumeControlService()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val foregroundServicePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startVolumeControlService()
            } else {
                // Handle permission denial
                // You might want to show a dialog explaining why the permission is necessary
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VolumeControlService.LocalBinder
            volumeControlService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            volumeControlService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the service
     /*   val serviceIntent = Intent(this, VolumeControlService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.FOREGROUND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    requestNotificationPermission()
                }
                else -> {
                    foregroundServicePermissionLauncher.launch(android.Manifest.permission.FOREGROUND_SERVICE)
                }
            }
        } else {
            // For versions below Android 13, start service directly
            startVolumeControlService()
        }

        setContent {
            VolumeControlScreen(
                onVolumeUp = { volumeControlService?.increaseVolume() },
                onVolumeDown = { volumeControlService?.decreaseVolume() }
            )
        }
    }

    private fun startVolumeControlService() {
        val serviceIntent = Intent(this, VolumeControlService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startVolumeControlService()
                }
                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startVolumeControlService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}

@Composable
fun VolumeControlScreen(
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onVolumeUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Volume Up")
        }

        Button(
            onClick = onVolumeDown,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Volume Down")
        }
    }
}