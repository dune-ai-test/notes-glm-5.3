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
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

/** Uppercase mono label used for section headers, e.g. "YOUR NOTES". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = FT.monoLabel,
        color = if (color.isUnspecified) FN.dotGray else color
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
            .background(if (selected) FN.strong else FN.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) FN.onStrong else FN.text,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(label, style = FT.chip, color = if (selected) FN.onStrong else FN.text)
        if (count != null) {
            Text(
                text = count.toString(),
                style = FT.monoTiny,
                color = if (selected) FN.onStrong.copy(alpha = 0.6f) else FN.dotGray
            )
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val bgColor = if (background.isUnspecified) FN.surface else background
    val iconTint = if (tint.isUnspecified) FN.text else tint
    Surface(
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, FN.line),
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
                tint = iconTint,
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
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = FT.monoTiny,
            color = FN.dotGray
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
            .background(if (done) FN.strong else FN.surface)
            .then(
                if (done) {
                    Modifier
                } else {
                    Modifier.border(BorderStroke(1.dp, FN.line), RoundedCornerShape(4.dp))
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
                tint = FN.onStrong,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
