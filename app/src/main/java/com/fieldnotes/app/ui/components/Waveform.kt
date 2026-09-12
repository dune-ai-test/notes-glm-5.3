package com.fieldnotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.ui.theme.FN

/**
 * A waveform built from amplitude samples (0–24). When [progress] > 0 the bars
 * up to that fraction light up in [playedColor]; otherwise bars are statically
 * mixed. Pass [Color.Unspecified] to use theme defaults.
 */
@Composable
fun Waveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    playedColor: Color = Color.Unspecified,
    baseColor: Color = Color.Unspecified,
    idleColor: Color = Color.Unspecified,
    maxHeight: Dp = 22.dp,
    barWidth: Dp = 5.dp,
    gap: Dp = 4.dp,
    live: Boolean = false,
    bars: Int = 30
) {
    val played = if (playedColor.isUnspecified) FN.accent else playedColor
    val base = if (baseColor.isUnspecified) FN.text else baseColor
    val idle = if (idleColor.isUnspecified) FN.dotGray else idleColor
    val buckets = remember(amplitudes, live, bars) { bucketize(amplitudes, bars, live) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        buckets.forEachIndexed { index, amp ->
            val height = (6 + amp).dp.coerceAtMost(maxHeight)
            val color = when {
                progress > 0f ->
                    if (index.toFloat() / buckets.size <= progress) played else idle
                index % 3 == 1 -> played
                amp >= 12 -> base
                else -> idle
            }
            Box(
                Modifier
                    .width(barWidth)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

private fun bucketize(amplitudes: List<Int>, target: Int, live: Boolean): List<Int> = when {
    amplitudes.isEmpty() -> List(target.coerceAtMost(24)) { 6 }
    live && amplitudes.size > target -> amplitudes.takeLast(target)
    amplitudes.size <= target -> amplitudes
    else -> List(target) { i ->
        amplitudes[((i.toLong() * amplitudes.size) / target).toInt().coerceAtMost(amplitudes.size - 1)]
    }
}
