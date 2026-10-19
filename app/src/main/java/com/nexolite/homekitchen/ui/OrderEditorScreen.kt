package com.nexolite.homekitchen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.data.Fulfillment
import com.nexolite.homekitchen.data.OrderInput
import com.nexolite.homekitchen.data.OrderLine
import com.nexolite.homekitchen.data.isPaid
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.dayLabel
import com.nexolite.homekitchen.util.rupees

@Composable
fun OrderEditorScreen(vm: AppViewModel, orderId: Long?, onClose: () -> Unit) {
    val date by vm.date.collectAsStateWithLifecycle()
    val menu by vm.menu.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()

    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var fulfillment by remember { mutableStateOf(Fulfillment.PICKUP) }
    var note by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("New Order") }
    val qty = remember { mutableStateMapOf<Long, Int>() }
    // Quantities already held by this order, so editing does not count them as "booked by others".
    val originalQty = remember { mutableStateMapOf<Long, Int>() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        if (orderId != null) {
            vm.loadOrder(orderId)?.let { d ->
                title = "Order #${d.order.orderNo}"
                customerName = d.customer?.name ?: ""
                phone = d.customer?.phone ?: ""
                fulfillment = d.order.fulfillment
                note = d.order.note
                paid = d.order.isPaid()
                d.items.forEach {
                    qty[it.menuItemId] = (qty[it.menuItemId] ?: 0) + it.qty
                    originalQty[it.menuItemId] = (originalQty[it.menuItemId] ?: 0) + it.qty
                }
            }
        }
    }

    val total = menu.sumOf { (qty[it.item.id] ?: 0) * it.item.price }
    val hasItems = menu.any { (qty[it.item.id] ?: 0) > 0 }
    val canSave = customerName.isNotBlank() && hasItems

    val matches = if (customerName.isBlank()) emptyList() else customers.filter {
        it.name.contains(customerName.trim(), ignoreCase = true) &&
            !it.name.equals(customerName.trim(), ignoreCase = true)
    }.take(5)

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(dayLabel(date), style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
            }
            if (orderId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete order", tint = Palette.Danger)
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                if (matches.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        matches.forEach { c ->
                            SuggestionChip(
                                onClick = { customerName = c.name; phone = c.phone },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { v -> phone = v.filter { it.isDigit() || it == '+' || it == ' ' } },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Items")
                if (menu.isEmpty()) {
                    Text(
                        "There is no menu for this day yet. Add dishes on the Menu screen first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.Danger
                    )
                } else {
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Palette.Card)
                            .border(1.dp, Palette.Line, shape)
                    ) {
                        menu.forEachIndexed { index, row ->
                            val current = qty[row.item.id] ?: 0
                            val own = originalQty[row.item.id] ?: 0
                            val limit = if (row.item.quantity > 0) row.item.quantity - (row.booked - own) else null
                            Row(
                                Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.item.name, style = MaterialTheme.typography.titleSmall)
                                    val sub = when {
                                        limit == null -> rupees(row.item.price)
                                        limit - current <= 0 && current == 0 -> "Sold out"
                                        else -> "${rupees(row.item.price)} · ${limit - current} left"
                                    }
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sub == "Sold out") Palette.Danger else Palette.Muted
                                    )
                                }
                                QtyStepper(qty = current, onChange = { qty[row.item.id] = it }, max = limit)
                            }
                            if (index < menu.lastIndex) HorizontalDivider(color = Palette.Line)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Pickup / Delivery")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleOption("Pickup", fulfillment == Fulfillment.PICKUP, Palette.Brand, Palette.BrandSoft, Modifier.weight(1f)) {
                        fulfillment = Fulfillment.PICKUP
                    }
                    ToggleOption("Delivery", fulfillment == Fulfillment.DELIVERY, Palette.Brand, Palette.BrandSoft, Modifier.weight(1f)) {
                        fulfillment = Fulfillment.DELIVERY
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Payment")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleOption("Paid", paid, Palette.Brand, Palette.BrandSoft, Modifier.weight(1f)) { paid = true }
                    ToggleOption("Unpaid (Add to Dues)", !paid, Palette.Danger, Palette.DangerBg, Modifier.weight(1f)) { paid = false }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Card)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
                Text(rupees(total), style = MaterialTheme.typography.headlineSmall)
            }
            PrimaryButton(
                text = if (orderId == null) "Save Order" else "Update Order",
                enabled = canSave,
                modifier = Modifier.weight(1f),
                onClick = {
                    vm.saveOrder(
                        OrderInput(
                            editingId = orderId,
                            date = date,
                            customerName = customerName,
                            phone = phone,
                            lines = menu.map { OrderLine(it.item, qty[it.item.id] ?: 0) },
                            fulfillment = fulfillment,
                            note = note,
                            paid = paid
                        ),
                        onSaved = onClose
                    )
                }
            )
        }
    }

    if (confirmDelete && orderId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Palette.Card,
            title = { Text("Delete this order?") },
            text = { Text("The order and any payment taken with it will be removed. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; vm.deleteOrder(orderId, onClose) },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Danger)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Palette.Ink) } }
        )
    }
}

@Composable
private fun ToggleOption(
    text: String,
    selected: Boolean,
    accent: Color,
    accentSoft: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else Palette.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) accentSoft else Palette.Card,
            contentColor = if (selected) accent else Palette.Ink
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold))
    }
}
