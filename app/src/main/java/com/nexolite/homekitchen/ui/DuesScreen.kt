package com.nexolite.homekitchen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.data.CustomerDue
import com.nexolite.homekitchen.ui.theme.Bricolage
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.WhatsApp
import com.nexolite.homekitchen.util.rupees
import com.nexolite.homekitchen.util.shortDay

@Composable
fun DuesScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val dues by vm.dues.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var receiving by remember { mutableStateOf<CustomerDue?>(null) }

    val totalDue = dues.sumOf { it.balance }
    val visible = dues.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow = "Customer balances", title = "Dues")

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Palette.Ink)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Total to receive", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC9CEC9))
                    Text(
                        rupees(totalDue),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = Bricolage),
                        color = Color.White
                    )
                    Text(
                        "from ${dues.size} customer" + if (dues.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC9CEC9)
                    )
                }
            }
            if (dues.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search customer") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Palette.Card,
                            focusedContainerColor = Palette.Card,
                            unfocusedBorderColor = Palette.Outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "All clear",
                        message = "No customer owes you anything right now."
                    )
                }
            }
            items(visible, key = { it.id }) { due ->
                SurfaceCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(due.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${due.unpaidCount} unpaid order" + (if (due.unpaidCount == 1) "" else "s") +
                                    " · last ${shortDay(due.lastDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.Muted
                            )
                        }
                        Text(rupees(due.balance), style = MaterialTheme.typography.titleMedium, color = Palette.Danger)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = "Receive Payment",
                            onClick = { receiving = due },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Send Reminder",
                            color = Palette.Ink,
                            onClick = {
                                WhatsApp.messageNumber(
                                    context, due.phone,
                                    "Assalam o Alaikum ${due.name}, a friendly reminder from ${settings.kitchenName}: " +
                                        "your pending amount is ${rupees(due.balance)}. Kindly clear it when convenient. Thank you!"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    receiving?.let { due ->
        ReceivePaymentDialog(
            due = due,
            onDismiss = { receiving = null },
            onConfirm = { amount -> vm.receivePayment(due.id, amount); receiving = null }
        )
    }
}

@Composable
private fun ReceivePaymentDialog(due: CustomerDue, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by remember { mutableStateOf(due.balance.toString()) }
    val value = amount.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        title = { Text("Receive Payment", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${due.name} owes ${rupees(due.balance)}.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v -> amount = v.filter { it.isDigit() } },
                    label = { Text("Amount received (Rs)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The amount is applied to the oldest unpaid orders first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                enabled = value > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Brand)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Ink) } }
    )
}
