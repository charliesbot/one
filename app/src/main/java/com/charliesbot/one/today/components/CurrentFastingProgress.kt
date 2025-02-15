package com.charliesbot.one.today.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.core.components.FastingProgressBar
import com.charliesbot.one.ui.theme.OneTheme

@Composable
fun FastingStatusIndicator(
    isFasting: Boolean,
    elapsedTimeText: String = "00:00:00",
) {
    val headerLabel = if (isFasting) "Remaining (82%)" else "UPCOMING FAST"
    val timeLabel = if (isFasting) elapsedTimeText else "16 hours"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = headerLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = timeLabel,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CurrentFastingProgress(
    elapsedTimeText: String = "00:00:00",
    isFasting: Boolean = false
) {
    FastingProgressBar(
        progress = 0.8f,
        strokeWidth = 30.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        innerContent = {
            FastingStatusIndicator(isFasting, elapsedTimeText)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CurrentFastingProgressPreview() {
    OneTheme {
        Column(verticalArrangement = Arrangement.spacedBy(50.dp)) {
            CurrentFastingProgress(isFasting = false)
            CurrentFastingProgress(isFasting = true)
        }
    }
}