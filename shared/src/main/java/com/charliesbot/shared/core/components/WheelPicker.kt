package com.charliesbot.shared.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

private const val INFINITE_SCROLL_MULTIPLIER = 10_000
private const val INFINITE_SCROLL_MIDDLE = INFINITE_SCROLL_MULTIPLIER / 2

/**
 * A reusable wheel picker component using VerticalPager.
 *
 * @param items The list of items to display
 * @param initialIndex The initial selected index
 * @param onSelectedIndexChange Callback when the selected index changes
 * @param modifier Modifier for the component
 * @param visibleItemCount Number of visible items (must be odd for center alignment)
 * @param itemHeight Height of each item
 * @param infiniteScroll Whether to enable infinite scrolling (for hour/minute columns)
 * @param itemContent Content composable for each item
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5,
    itemHeight: Dp = 48.dp,
    infiniteScroll: Boolean = false,
    itemContent: @Composable (item: T) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    val pageCount = if (infiniteScroll) items.size * INFINITE_SCROLL_MULTIPLIER else items.size
    val initialPage = if (infiniteScroll) {
        INFINITE_SCROLL_MIDDLE - (INFINITE_SCROLL_MIDDLE % items.size) + initialIndex
    } else {
        initialIndex
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount }
    )

    // Haptic feedback on page change
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    }

    // Report selection changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val actualIndex = if (infiniteScroll) page % items.size else page
                onSelectedIndexChange(actualIndex)
            }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // VerticalPager
        VerticalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(itemHeight),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemCount / 2)),
            modifier = Modifier.height(itemHeight * visibleItemCount),
            beyondViewportPageCount = visibleItemCount / 2
        ) { page ->
            val actualIndex = if (infiniteScroll) page % items.size else page
            val item = items[actualIndex]

            WheelPickerItem(
                pagerState = pagerState,
                page = page,
                itemHeight = itemHeight
            ) {
                itemContent(item)
            }
        }
    }
}

@Composable
private fun WheelPickerItem(
    pagerState: PagerState,
    page: Int,
    itemHeight: Dp,
    content: @Composable () -> Unit
) {
    val pageOffset = pagerState.calculatePageOffset(page)

    // iOS-style fisheye effect: scale and alpha based on distance from center
    val scale = lerp(1f, 0.7f, abs(pageOffset).coerceIn(0f, 1f))
    val alpha = lerp(1f, 0.3f, abs(pageOffset).coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .height(itemHeight)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Calculate the offset of the page from the current page.
 * Returns 0f when the page is exactly centered, positive/negative based on direction.
 */
private fun PagerState.calculatePageOffset(page: Int): Float {
    return (currentPage - page) + currentPageOffsetFraction
}

@Preview(showBackground = true)
@Composable
private fun WheelPickerPreview() {
    val items = (1..12).toList()
    WheelPicker(
        items = items,
        initialIndex = 5,
        onSelectedIndexChange = {},
        infiniteScroll = true
    ) { item ->
        Text(
            text = item.toString(),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WheelPickerDatePreview() {
    val items = listOf("Today", "Yesterday", "Sat 24 Jan", "Fri 23 Jan", "Thu 22 Jan")
    WheelPicker(
        items = items,
        initialIndex = 0,
        onSelectedIndexChange = {},
        infiniteScroll = false
    ) { item ->
        Text(
            text = item,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
