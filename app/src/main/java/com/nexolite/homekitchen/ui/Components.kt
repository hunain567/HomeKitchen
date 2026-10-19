package com.nexolite.homekitchen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexolite.homekitchen.data.OrderStatus
import com.nexolite.homekitchen.ui.theme.Palette
import com.nexolite.homekitchen.util.dayLabel
import com.nexolite.homekitchen.util.relativeDay

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Card)
            .border(1.dp, Palette.Line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun ScreenHeader(
    eyebrow: String?,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) {
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Muted
                )
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.Ink)
        }
        if (trailing != null) trailing()
    }
}

/** Previous / next day switcher used on the Menu and Orders screens. */
@Composable
fun DateSwitcher(date: Long, onShift: (Long) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE6E8E2)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
            Text(relativeDay(date), style = MaterialTheme.typography.labelLarge, color = Palette.Ink)
            Text(dayLabel(date), style = MaterialTheme.typography.labelSmall, color = Palette.Muted)
        }
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        OrderStatus.PACKED -> Palette.PackedBg to Palette.PackedFg
        OrderStatus.DELIVERED -> Palette.BrandSoft to Palette.Brand
        else -> Palette.PendingBg to Palette.PendingFg
    }
    Pill(status, bg, fg)
}

@Composable
fun Pill(text: String, background: Color, foreground: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = foreground
    )
}

@Composable
fun FilterChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Ink, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.height(38.dp)
        ) { Text(text, style = MaterialTheme.typography.labelMedium) }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Palette.Outline),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Palette.Card, contentColor = Palette.Ink),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier.height(38.dp)
        ) { Text(text, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Palette.Brand,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Palette.Brand) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun QtyStepper(qty: Int, onChange: (Int) -> Unit, max: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(
            onClick = { if (qty > 0) onChange(qty - 1) },
            enabled = qty > 0,
            border = BorderStroke(1.dp, Palette.Outline),
            modifier = Modifier.size(44.dp)
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Decrease") }
        Text(
            qty.toString(),
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        FilledIconButton(
            onClick = { onChange(qty + 1) },
            enabled = max == null || qty < max,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Palette.Brand, contentColor = Color.White),
            modifier = Modifier.size(44.dp)
        ) { Icon(Icons.Rounded.Add, contentDescription = "Increase") }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.BrandSoft)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(color)
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Palette.BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Brand, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.Muted)
}
