package com.charliesbot.one.core.components

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.formatDate
import java.time.LocalDateTime

@Composable
fun TimeDisplay(
    title: String, date: LocalDateTime, onClick: (() -> Unit) = {}
) {
    val verticalSpace = (-5).dp
    val textColor =
        MaterialTheme.colorScheme.onSurface
    val dateFormat = TimeFormat.DATE_TIME

    Column(
        modifier = Modifier
            .background(color = androidx.compose.ui.graphics.Color(Color.RED))
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(verticalSpace),
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .background(color = androidx.compose.ui.graphics.Color(Color.YELLOW))
                .padding(0.dp),
            style = TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = textColor
        )
        Box(modifier = Modifier.background(color = androidx.compose.ui.graphics.Color(Color.GREEN))) {
            Button(onClick = onClick) {
                Text(
                    text = formatDate(date, dateFormat),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
private fun TimeDisplayPreview() {
    OneTheme {
        TimeDisplay(title = "Started", date = LocalDateTime.now(), onClick = {})
    }
}
