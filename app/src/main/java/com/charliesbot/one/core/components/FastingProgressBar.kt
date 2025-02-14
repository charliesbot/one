package com.charliesbot.one.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charliesbot.one.ui.theme.OneTheme

@Composable
fun FastingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 45.dp,
    innerContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .aspectRatio(1f)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2 - strokeWidth.toPx() / 2

            drawCircle(
                color = trackColor,
                radius = radius,
                style = Stroke(width = strokeWidth.toPx())
            )

            val sweepAngle = progress * 360f

            drawArc(
                color = indicatorColor,
                startAngle = -90f, // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        innerContent()
    }
}

@Preview(showBackground = true)
@Composable
fun FastingProgressBarPreview() {
    OneTheme {
        FastingProgressBar(0.8f, modifier = Modifier.size(200.dp))
    }
}