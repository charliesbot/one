package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            style = TextStyle(
                platformStyle = PlatformTextStyle(
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
            interactionSource = interactionSource,
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
    TimeDisplay(
        title = "Started",
        date = LocalDateTime.now(),
        onClick = {},
        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
    )
}
