package com.charliesbot.one.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.ui.theme.OneTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(date: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.ENGLISH)
    return date.format(formatter)
}

@Composable
fun FastingTimeAction(title: String, date: LocalDateTime, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-5).dp),
        modifier = Modifier.clickable(onClick = onClick ?: {})
    ) {
        Text(text = title.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.W500)
        Text(text = formatDate(date), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (onClick != null) {
            Text("Edit Start", fontSize = 10.sp)
        }

    }
}

@Preview(showBackground = true)
@Composable
fun FastingTimeActionPreview() {
    OneTheme {
        FastingTimeAction(
            title = "Started",
            date = LocalDateTime.now(),
            onClick = {}
        )
    }
}