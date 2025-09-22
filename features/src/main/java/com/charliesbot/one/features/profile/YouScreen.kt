package com.charliesbot.one.features.profile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.core.components.FastingMonthCalendar
import com.charliesbot.shared.core.components.createMockFastingData
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.YearMonth

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YouScreen(viewModel: YouViewModel = koinViewModel()) {
    val interactionSources = List(size = 2) { MutableInteractionSource() }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Profile")
            ButtonGroup(
                overflowIndicator = {},
                expandedRatio = 0f,
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                customItem(
                    buttonGroupContent = {
                        FilledIconButton(
                            onClick = {},
                            modifier = Modifier.animateWidth(interactionSource = interactionSources[0]),
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = interactionSources[0],
                        ) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = null,
                            )
                        }
                    },
                    menuContent = {}
                )
                customItem(
                    buttonGroupContent = {
                        FilledIconButton(
                            onClick = {},
                            modifier = Modifier.animateWidth(interactionSource = interactionSources[1]),
                            shapes = IconButtonDefaults.shapes(),
                            interactionSource = interactionSources[1],
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                            )
                        }
                    },
                    menuContent = {}
                )
            }
            Card(modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)) {
                FastingMonthCalendar(
                    yearMonth = YearMonth.of(2025, 9),
                    fastingData = createMockFastingData(YearMonth.of(2025, 9)),
                    firstDayOfWeek = DayOfWeek.SUNDAY
                )
            }
        }
    }
}
