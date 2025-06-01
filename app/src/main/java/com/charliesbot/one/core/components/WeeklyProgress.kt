package com.charliesbot.one.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.R
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.shared.core.components.FastingProgressBar

@Composable
fun WeeklyProgress(modifier: Modifier = Modifier) {
    val daysOfWeek: List<Int> = listOf(
        R.string.monday,
        R.string.tuesday,
        R.string.wednesday,
        R.string.thursday,
        R.string.friday,
        R.string.saturday,
        R.string.sunday
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        daysOfWeek.forEach { dayResId ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(dayResId).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.size(4.dp))
                FastingProgressBar(
                    progress = 0.1f,
                    modifier = Modifier.size(25.dp),
                    strokeWidth = 5.dp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyProgressPreview() {
    OneTheme {
        Box(modifier = Modifier.width(300.dp)) {
            WeeklyProgress(modifier = Modifier.fillMaxWidth())
        }
    }
}