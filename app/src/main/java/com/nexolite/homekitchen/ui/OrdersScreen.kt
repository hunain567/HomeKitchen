package com.nexolite.homekitchen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.data.OrderStatus
import com.nexolite.homekitchen.data.OrderWithDetails
import com.nexolite.homekitchen.data.balance
import com.nexolite.homekitchen.data.isPaid
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.dayLabel
import com.nexolite.homekitchen.util.rupees

private enum class OrderFilter(val label: String) { ALL("All"), PENDING("Pending"), PACKED("Packed"), DELIVERED("Delivered"), UNPAID("Unpaid") }

@Composable
fun OrdersScreen(vm: AppViewModel, onNewOrder: () -> Unit, onOpenOrder: (Long) -> Unit) {
    val date by vm.date.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(OrderFilter.ALL) }

    val pending = orders.count { it.order.status == OrderStatus.PENDING }
    val packed = orders.count { it.order.status == OrderStatus.PACKED }
    val delivered = orders.count { it.order.status == OrderStatus.DELIVERED }
    val visible = orders.filter {
        when (filter) {
            OrderFilter.ALL -> true
            OrderFilter.PENDING -> it.order.status == OrderStatus.PENDING
            OrderFilter.PACKED -> it.order.status == OrderStatus.PACKED
            OrderFilter.DELIVERED -> it.order.status == OrderStatus.DELIVERED
            OrderFilter.UNPAID -> !it.order.isPaid()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow = dayLabel(date), title = "Orders", trailing = {
            Button(
                onClick = onNewOrder,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Brand),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("New Order", style = MaterialTheme.typography.labelLarge)
            }
        })

        DateSwitcher(date = date, onShift = vm::shiftDate, modifier = Modifier.padding(horizontal = 20.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CountTile("Pending", pending, Palette.PendingBg, Palette.PendingFg, Modifier.weight(1f))
            CountTile("Packed", packed, Palette.PackedBg, Palette.PackedFg, Modifier.weight(1f))
            CountTile("Delivered", delivered, Palette.BrandSoft, Palette.Brand, Modifier.weight(1f))
        }

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderFilter.entries.forEach { f ->
                val label = if (f == OrderFilter.ALL) "All ${orders.size}" else f.label
                FilterChipButton(label, selected = filter == f, onClick = { filter = f })
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = if (orders.isEmpty()) "No orders yet" else "Nothing here",
                        message = if (orders.isEmpty()) "When someone orders in your WhatsApp group, tap New Order to note it."
                        else "No orders match this filter."
                    )
                }
            }
            items(visible, key = { it.order.id }) { o ->
                OrderCard(
                    details = o,
                    onClick = { onOpenOrder(o.order.id) },
                    onAdvance = { vm.advanceStatus(o.order) },
                    onMarkPaid = { vm.markPaid(o.order) }
                )
            }
        }
    }
}

@Composable
private fun CountTile(label: String, count: Int, bg: Color, fg: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, color = fg)
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun OrderCard(
    details: OrderWithDetails,
    onClick: () -> Unit,
    onAdvance: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val order = details.order
    SurfaceCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${order.orderNo} · ${details.customer?.name ?: "Unknown"}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            StatusPill(order.status)
        }
        Text(
            details.items.joinToString(", ") { "${it.qty} × ${it.name}" },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.InkSoft
        )
        val extra = listOfNotNull(order.fulfillment, order.note.takeIf { it.isNotBlank() }).joinToString(" · ")
        Text(extra, style = MaterialTheme.typography.bodySmall, color = Palette.Muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rupees(order.total), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            when {
                order.isPaid() -> Text("Paid", style = MaterialTheme.typography.labelSmall, color = Palette.Brand)
                order.paidAmount > 0 -> Text("Due ${rupees(order.balance())}", style = MaterialTheme.typography.labelSmall, color = Palette.Danger)
                else -> Text("Unpaid", style = MaterialTheme.typography.labelSmall, color = Palette.Danger)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (order.status) {
                OrderStatus.PENDING -> SecondaryButton("Mark as Packed", onAdvance, Modifier.weight(1f))
                OrderStatus.PACKED -> PrimaryButton("Mark as Delivered", onAdvance, Modifier.weight(1f).height(46.dp))
                else -> {}
            }
            if (!order.isPaid()) {
                SecondaryButton("Mark as Paid", onMarkPaid, Modifier.weight(1f), color = Palette.Danger)
            }
        }
    }
}
