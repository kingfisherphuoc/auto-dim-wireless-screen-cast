package com.ph.screencast.autodim

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AutoDimScreen(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("AutoDim", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Automatically lower your handheld's screen brightness while casting to a TV, " +
                    "then restore your previous brightness when casting ends.",
                style = MaterialTheme.typography.bodyLarge
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cast monitoring", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (enabled) "On" else "Off",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = null)
                }
            }
            Instruction(
                title = "Turn on",
                description = "Enable the toggle to start monitoring. If asked, allow AutoDim to modify " +
                    "system settings, then return to this app. Monitoring starts automatically after " +
                    "permission is granted. Allow notifications to see the monitoring notification."
            )
            Instruction(
                title = "Connect to your TV",
                description = "Use Android's Screen Cast controls to connect to your TV. The toggle " +
                    "does not start casting. Once connected, AutoDim lowers the handheld's brightness " +
                    "to its minimum without adding a dark overlay to the TV picture."
            )
            Instruction(
                title = "Turn off",
                description = "Disable the toggle to stop monitoring, remove the notification, and " +
                    "restore your saved brightness and automatic brightness setting. You can also " +
                    "tap Turn off in the notification. This does not disconnect Screen Cast."
            )
            Instruction(
                title = "Quick Settings tile",
                description = "Swipe down twice to open Quick Settings, tap Edit, and drag AutoDim " +
                    "into your active tiles. Tap the tile to turn monitoring on or off without opening " +
                    "the app. If permissions are needed, the tile opens AutoDim to request them."
            )
            Text(
                "AutoDim monitors in the background while enabled. After restarting your device, " +
                    "open this app and enable it again. AutoDim does not prevent your device from sleeping.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Instruction(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 480)
@Composable
private fun AutoDimScreenPreview() {
    MaterialTheme { AutoDimScreen(false) {} }
}
