package com.nexolite.homekitchen.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.ui.theme.Palette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(settings.kitchenName) }
    var footer by remember { mutableStateOf(settings.menuFooter) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.saveBackupTo(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                vm.updateSettings { it.copy(kitchenName = name.trim().ifEmpty { "Home Kitchen" }, menuFooter = footer.trim()) }
                onClose()
            }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SurfaceCard {
                Text("WhatsApp Menu", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kitchen name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = footer,
                    onValueChange = { footer = it },
                    label = { Text("Closing line") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The kitchen name appears at the top of the shared menu and the closing line at the end.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
                PrimaryButton(
                    text = "Save",
                    onClick = {
                        vm.updateSettings { it.copy(kitchenName = name.trim().ifEmpty { "Home Kitchen" }, menuFooter = footer.trim()) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SurfaceCard {
                Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
                Text(
                    "All data is stored on this phone. Take a backup regularly and keep it on WhatsApp or Google Drive, " +
                        "so you can restore it if the phone is lost or changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.InkSoft
                )
                Text(
                    "Last backup: " + if (settings.lastBackupAt == 0L) "Never" else
                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(settings.lastBackupAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (settings.lastBackupAt == 0L) Palette.Danger else Palette.Muted
                )
                PrimaryButton(
                    text = "Share Backup",
                    icon = Icons.Rounded.CloudUpload,
                    onClick = {
                        scope.launch {
                            val uri = vm.createShareableBackup() ?: return@launch
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Send backup to"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(
                    text = "Save Backup to Phone",
                    onClick = { saveLauncher.launch(vm.backupFileName()) },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(
                    text = "Restore from Backup",
                    color = Palette.Danger,
                    onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                "Home Kitchen 1.0",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = Palette.Card,
            title = { Text("Restore this backup?") },
            text = { Text("All current data on this phone will be replaced by the data in the backup file.") },
            confirmButton = {
                Button(
                    onClick = { vm.restoreFrom(uri); pendingRestore = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Danger)
                ) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel", color = Palette.Ink) } }
        )
    }
}
