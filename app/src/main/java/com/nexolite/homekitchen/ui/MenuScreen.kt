package com.nexolite.homekitchen.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.data.DishSuggestion
import com.nexolite.homekitchen.data.MenuItem
import com.nexolite.homekitchen.data.Settings
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.WhatsApp
import com.nexolite.homekitchen.util.dayLabel
import com.nexolite.homekitchen.util.rupees
import com.nexolite.homekitchen.util.timeLabel

@Composable
fun MenuScreen(vm: AppViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val date by vm.date.collectAsStateWithLifecycle()
    val rows by vm.menu.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<MenuItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow = dayLabel(date), title = "Menu", trailing = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Palette.Ink)
            }
        })

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DateSwitcher(date = date, onShift = vm::shiftDate)
            Button(
                onClick = { adding = true },
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Brand),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Add Dish", style = MaterialTheme.typography.labelLarge)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BackupBanner(settings, onOpenSettings) }
            item {
                CutoffRow(settings.cutoffMinutes) {
                    TimePickerDialog(
                        context,
                        { _, h, m -> vm.updateSettings { s -> s.copy(cutoffMinutes = h * 60 + m) } },
                        settings.cutoffMinutes / 60,
                        settings.cutoffMinutes % 60,
                        false
                    ).show()
                }
            }
            if (rows.isEmpty()) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.RestaurantMenu,
                            title = "No dishes for this day",
                            message = "Add today's dishes, or copy the last menu and adjust it."
                        )
                        SecondaryButton(text = "Copy Last Menu", onClick = vm::copyPreviousMenu)
                    }
                }
            }
            items(rows, key = { it.item.id }) { row -> DishCard(row) { editing = row.item } }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            PrimaryButton(
                text = "Share Menu on WhatsApp",
                icon = Icons.Rounded.Share,
                container = Palette.Ink,
                enabled = rows.isNotEmpty(),
                onClick = { WhatsApp.shareText(context, buildMenuMessage(date, rows, settings)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (adding) {
        DishDialog(
            initial = null,
            suggestions = suggestions,
            onDismiss = { adding = false },
            onSave = { name, price, qty -> vm.addDish(name, price, qty); adding = false },
            onDelete = null
        )
    }
    editing?.let { item ->
        DishDialog(
            initial = item,
            suggestions = emptyList(),
            onDismiss = { editing = null },
            onSave = { name, price, qty ->
                vm.updateDish(item.copy(name = name, price = price, quantity = qty)); editing = null
            },
            onDelete = { vm.deleteDish(item); editing = null }
        )
    }
}

@Composable
private fun BackupBanner(settings: Settings, onOpenSettings: () -> Unit) {
    val dayMs = 24L * 60 * 60 * 1000
    val age = System.currentTimeMillis() - settings.lastBackupAt
    if (age < dayMs) return
    val text = if (settings.lastBackupAt == 0L) "No backup yet. Keep your data safe." else
        "Last backup was ${age / dayMs} day(s) ago."
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.DangerBg)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Palette.Danger, modifier = Modifier.weight(1f))
        TextButton(onClick = onOpenSettings) {
            Text("Back Up", color = Palette.Danger, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CutoffRow(minutes: Int, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.PendingBg)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Orders close at ${timeLabel(minutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.PendingFg,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onChange) {
            Text("Change", color = Palette.PendingFg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DishCard(row: MenuRow, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(row.item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(rupees(row.item.price), style = MaterialTheme.typography.titleSmall)
        }
        if (row.item.quantity > 0) {
            val fraction = row.booked.toFloat() / row.item.quantity
            ProgressBar(fraction, if (row.soldOut) Palette.Danger else Palette.Brand)
            if (row.soldOut) {
                Text(
                    "Sold out · ${row.booked} of ${row.item.quantity} booked",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Danger
                )
            } else {
                Text(
                    "${row.booked} of ${row.item.quantity} booked · ${row.left} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
            }
        } else {
            Text("${row.booked} booked · no limit", style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        }
    }
}

@Composable
private fun DishDialog(
    initial: MenuItem?,
    suggestions: List<DishSuggestion>,
    onDismiss: () -> Unit,
    onSave: (String, Long, Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var qty by remember { mutableStateOf(initial?.quantity?.takeIf { it > 0 }?.toString() ?: "") }
    val priceValue = price.toLongOrNull()
    val valid = name.isNotBlank() && priceValue != null && priceValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Card,
        title = { Text(if (initial == null) "Add Dish" else "Edit Dish", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (suggestions.isNotEmpty()) {
                    FieldLabel("Previous dishes")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { s ->
                            SuggestionChip(
                                onClick = {
                                    name = s.name
                                    price = s.price.toString()
                                    qty = s.quantity.takeIf { it > 0 }?.toString() ?: ""
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dish name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { v -> price = v.filter { it.isDigit() } },
                        label = { Text("Price (Rs)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { v -> qty = v.filter { it.isDigit() } },
                        label = { Text("Quantity") },
                        placeholder = { Text("No limit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "Quantity is how many plates you will cook. Leave it empty for no limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Muted
                )
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDelete) {
                        Text("Remove Dish", color = Palette.Danger, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), priceValue ?: 0, qty.toIntOrNull() ?: 0) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Brand)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Ink) } }
    )
}

private fun buildMenuMessage(date: Long, rows: List<MenuRow>, settings: Settings): String = buildString {
    append("*").append(settings.kitchenName).append(" – ").append(dayLabel(date)).append("*\n\n")
    rows.forEachIndexed { i, row ->
        append(i + 1).append(". ").append(row.item.name).append(" – ").append(rupees(row.item.price))
        if (row.soldOut) append(" (Sold out)")
        append("\n")
    }
    append("\nOrders close at ").append(timeLabel(settings.cutoffMinutes)).append(".")
    if (settings.menuFooter.isNotBlank()) append("\n").append(settings.menuFooter.trim())
}
