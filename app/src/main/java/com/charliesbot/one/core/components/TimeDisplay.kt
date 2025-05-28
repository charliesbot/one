package com.charliesbot.one.core.components

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.formatDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeDisplay(
    title: String,
    shapes: ToggleButtonShapes,
    date: LocalDateTime,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit) = {},
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            style = TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                ),
                textAlign = TextAlign.Center,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedToggleButton(
            checked = false,
            onCheckedChange = { onClick() },
            shapes = shapes,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Text(
                text = formatDate(date, TimeFormat.DATE_TIME),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
private fun TimeDisplayPreview() {
    OneTheme {
        TimeDisplay(
            title = "Started",
            date = LocalDateTime.now(),
            onClick = {},
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
        )
    }
}
