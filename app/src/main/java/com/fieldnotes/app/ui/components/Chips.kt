package com.fieldnotes.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.ui.theme.CardWhite
import com.fieldnotes.app.ui.theme.DotGray
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.Ink
import com.fieldnotes.app.ui.theme.Line
import com.fieldnotes.app.ui.theme.tagColor

/** Uppercase mono label used for section headers, e.g. "YOUR NOTES". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = DotGray) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = FT.monoLabel,
        color = color
    )
}

@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Ink else CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CardWhite else Ink,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(label, style = FT.chip, color = if (selected) CardWhite else Ink)
        if (count != null) {
            Text(text = count.toString(), style = FT.monoTiny, color = DotGray)
        }
    }
}

@Composable
fun TagChipView(
    name: String,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bg = tagColor(colorIndex)
    val onColor = if (colorIndex == 6) CardWhite else Ink
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Ink else bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "#$name",
            style = FT.chip,
            color = if (selected) CardWhite else onColor
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = CardWhite,
    tint: Color = Ink,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = background,
        shadowElevation = 2.dp,
        modifier = modifier.size(size)
    ) {
        Box(
            Modifier.clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun ColoredDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MetaPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CardWhite,
        border = BorderStroke(1.dp, Line),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = FT.monoTiny,
            color = DotGray
        )
    }
}

/** Small square checkbox used on checklist cards and editor checklist blocks. */
@Composable
fun MiniCheckbox(done: Boolean, onToggle: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (done) Ink else CardWhite)
            .then(
                if (done) {
                    Modifier
                } else {
                    Modifier.border(BorderStroke(1.dp, Line), RoundedCornerShape(4.dp))
                }
            )
            .then(
                if (onToggle != null) Modifier.clickable { onToggle() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = CardWhite,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
