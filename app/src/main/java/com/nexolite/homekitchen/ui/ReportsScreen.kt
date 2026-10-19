package com.nexolite.homekitchen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.monthLabel
import com.nexolite.homekitchen.util.rupees
import com.nexolite.homekitchen.util.today
import com.nexolite.homekitchen.util.weekday

@Composable
fun ReportsScreen(vm: AppViewModel) {
    val period by vm.period.collectAsStateWithLifecycle()
    val report by vm.report.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            eyebrow = if (period == ReportPeriod.TODAY) "Today" else monthLabel(today()),
            title = "Reports",
            trailing = {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE6E8E2))
                        .padding(3.dp)
                ) {
                    SegmentButton("Today", period == ReportPeriod.TODAY) { vm.setPeriod(ReportPeriod.TODAY) }
                    SegmentButton("Month", period == ReportPeriod.MONTH) { vm.setPeriod(ReportPeriod.MONTH) }
                }
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total Sale", rupees(report.sale), Palette.Ink, Modifier.weight(1f))
                StatTile("Orders", report.orders.toString(), Palette.Ink, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Received", rupees(report.received), Palette.Brand, Modifier.weight(1f))
                StatTile("Unpaid", rupees(report.unpaid), Palette.Danger, Modifier.weight(1f))
            }

            SurfaceCard {
                Text("Last 7 days (Rs)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                WeekChart(report.lastSevenDays)
            }

            SurfaceCard {
                Text(
                    if (period == ReportPeriod.TODAY) "Top dishes today" else "Top dishes this month",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (report.topDishes.isEmpty()) {
                    Text("No sales yet.", style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
                }
                report.topDishes.forEach { d ->
                    Row {
                        Text(d.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "${d.qty} plates · ${rupees(d.amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SegmentButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Palette.Card else Color.Transparent)
    ) {
        Text(
            text,
            color = if (selected) Palette.Ink else Palette.InkSoft,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, valueColor: Color, modifier: Modifier) {
    SurfaceCard(modifier = modifier, padding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
    }
}

@Composable
private fun WeekChart(days: List<Pair<Long, Long>>) {
    val max = (days.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val todayValue = today()
    val description = days.joinToString(", ") { "${weekday(it.first)} ${rupees(it.second)}" }
    Row(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = "Daily sales: $description" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { (day, amount) ->
            val isToday = day == todayValue
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val fraction = (amount.toFloat() / max).coerceIn(0.02f, 1f)
                // The empty space above the bar takes the remaining share of the height.
                Spacer(Modifier.weight(1.001f - fraction))
                if (amount > 0) {
                    Text(
                        shortAmount(amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Palette.Ink else Palette.Muted
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(fraction, fill = true)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(if (isToday) Palette.Brand else Palette.BrandTrack)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    weekday(day),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (isToday) Palette.Ink else Palette.Muted
                )
            }
        }
    }
}

private fun shortAmount(amount: Long): String = when {
    amount >= 100_000 -> String.format("%.1fL", amount / 100_000.0)
    amount >= 1_000 -> String.format("%.1fk", amount / 1_000.0)
    else -> amount.toString()
}
