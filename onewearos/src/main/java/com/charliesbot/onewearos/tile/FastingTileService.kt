package com.charliesbot.onewearos.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.FastingProgressUtil
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalHorologistApi::class)
class FastingTileService : SuspendingTileService(), KoinComponent {

    private val repository: FastingDataRepository by inject()

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val ID_FASTING_ICON = "fasting_icon"

        // Colors
        private const val COLOR_PRIMARY = 0xFF82B387.toInt()
        private const val COLOR_ON_SURFACE = 0xFFFFFFFF.toInt()
        private const val COLOR_SURFACE_VARIANT = 0xFF49454F.toInt()
        private const val COLOR_SECONDARY = 0xFFB0B0B0.toInt()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): Tile {
        val fastingData = repository.fastingDataItem.first()
        val deviceParams = requestParams.deviceConfiguration

        val layout = if (fastingData.isFasting) {
            createFastingLayout(fastingData, deviceParams)
        } else {
            createNotFastingLayout(deviceParams)
        }

        return Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(wrapWithClickAction(layout))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                ID_FASTING_ICON,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_notification_status)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createFastingLayout(
        fastingData: FastingDataItem,
        deviceParams: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        val progress = FastingProgressUtil.calculateFastingProgress(
            fastingData,
            System.currentTimeMillis()
        )
        val goal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)

        // Calculate elapsed time display (e.g., "2:30" for 2h 30m)
        val elapsedHours = progress.elapsedTimeMillis / (1000 * 60 * 60)
        val elapsedMinutes = (progress.elapsedTimeMillis / (1000 * 60)) % 60
        val elapsedTimeText = "$elapsedHours:${elapsedMinutes.toString().padStart(2, '0')}"

        // Calculate remaining time
        val remainingHours = progress.remainingTimeMillis / (1000 * 60 * 60)
        val remainingMinutes = (progress.remainingTimeMillis / (1000 * 60)) % 60
        val remainingText = if (progress.isComplete) {
            getString(R.string.tile_goal_reached)
        } else {
            getString(R.string.tile_remaining_format, remainingHours, remainingMinutes)
        }

        // Progress fraction (0.0 to 1.0)
        val progressFloat = (progress.progressPercentage / 100f).coerceIn(0f, 1f)

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                // Circular progress indicator as background
                CircularProgressIndicator.Builder()
                    .setProgress(progressFloat)
                    .setCircularProgressIndicatorColors(
                        ProgressIndicatorColors(
                            argb(COLOR_PRIMARY),
                            argb(COLOR_SURFACE_VARIANT)
                        )
                    )
                    .setStrokeWidth(dp(8f))
                    .build()
            )
            .addContent(
                // Main content column
                Column.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(Spacer.Builder().setHeight(dp(24f)).build())
                    // Title
                    .addContent(
                        Text.Builder()
                            .setText(getString(R.string.tile_title_fasting))
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(COLOR_ON_SURFACE))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(8f)).build())
                    // Main content row: Time on left, icon on right
                    .addContent(
                        Row.Builder()
                            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                            .addContent(
                                // Time display column
                                Column.Builder()
                                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                                    .addContent(
                                        Text.Builder()
                                            .setText(elapsedTimeText)
                                            .setFontStyle(
                                                FontStyle.Builder()
                                                    .setSize(sp(32f))
                                                    .setWeight(FONT_WEIGHT_BOLD)
                                                    .setColor(argb(COLOR_ON_SURFACE))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .addContent(
                                        Text.Builder()
                                            .setText(remainingText)
                                            .setFontStyle(
                                                FontStyle.Builder()
                                                    .setSize(sp(12f))
                                                    .setColor(argb(COLOR_SECONDARY))
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .addContent(Spacer.Builder().setWidth(dp(16f)).build())
                            // Icon
                            .addContent(
                                Image.Builder()
                                    .setResourceId(ID_FASTING_ICON)
                                    .setWidth(dp(32f))
                                    .setHeight(dp(32f))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(16f)).build())
                    // Goal label
                    .addContent(
                        Text.Builder()
                            .setText(getString(R.string.tile_goal_format, goal.durationDisplay))
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(COLOR_SECONDARY))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createNotFastingLayout(
        deviceParams: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                // Empty circular progress indicator as background
                CircularProgressIndicator.Builder()
                    .setProgress(0f)
                    .setCircularProgressIndicatorColors(
                        ProgressIndicatorColors(
                            argb(COLOR_PRIMARY),
                            argb(COLOR_SURFACE_VARIANT)
                        )
                    )
                    .setStrokeWidth(dp(8f))
                    .build()
            )
            .addContent(
                // Main content column
                Column.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                    .addContent(Spacer.Builder().setHeight(dp(40f)).build())
                    // Title
                    .addContent(
                        Text.Builder()
                            .setText(getString(R.string.tile_title_not_fasting))
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(14f))
                                    .setColor(argb(COLOR_ON_SURFACE))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(12f)).build())
                    // Icon
                    .addContent(
                        Image.Builder()
                            .setResourceId(ID_FASTING_ICON)
                            .setWidth(dp(48f))
                            .setHeight(dp(48f))
                            .build()
                    )
                    .addContent(Spacer.Builder().setHeight(dp(12f)).build())
                    // Tap to start label
                    .addContent(
                        Text.Builder()
                            .setText(getString(R.string.tile_tap_to_start))
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(12f))
                                    .setColor(argb(COLOR_SECONDARY))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun wrapWithClickAction(
        content: LayoutElementBuilders.LayoutElement
    ): LayoutElementBuilders.LayoutElement {
        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(content)
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(
                        Clickable.Builder()
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(packageName)
                                            .setClassName(MainActivity::class.java.name)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
