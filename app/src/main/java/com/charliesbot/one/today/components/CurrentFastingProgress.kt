package com.charliesbot.one.today.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.core.components.FastingProgressBar
import com.charliesbot.one.ui.theme.OneTheme

@Composable
fun CurrentFastingProgress(
    elapsedTimeText: String = "00:00:00",
) {
    FastingProgressBar(
        progress = 0.8f,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        innerContent = {
            Text(
                text = elapsedTimeText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CurrentFastingProgressPreview() {
    OneTheme {
        CurrentFastingProgress()
    }
}