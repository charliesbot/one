package com.charliesbot.shared.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.formatDate
import java.time.LocalDateTime

@Composable
fun TimeInfoDisplay(
    title: String, date: LocalDateTime, onClick: (() -> Unit)? = null, isForWear: Boolean = false
) {
    val verticalSpace = if (isForWear) 2.dp else (-5).dp
    val textColor =
        if (isForWear) WearMaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val dateFormat = if (isForWear) TimeFormat.TIME else TimeFormat.DATE_TIME

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpace),
        modifier = Modifier.clickable(enabled = onClick != null, onClick = onClick ?: {})
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.W500,
            color = textColor
        )
        Text(
            text = formatDate(date, dateFormat),
            fontSize = if (isForWear) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        if (onClick != null) {
            Text("Edit Start", fontSize = 10.sp, color = textColor)
        }

    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
private fun TimeInfoDisplayPreview() {
    TimeInfoDisplay(title = "Started", date = LocalDateTime.now(), onClick = {})
}
