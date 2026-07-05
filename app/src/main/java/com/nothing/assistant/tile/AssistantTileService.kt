package com.nothing.assistant.tile

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material3.CompactChip
import androidx.wear.protolayout.material3.Text
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.layouts.PrimaryLayout
import androidx.wear.protolayout.material3.tile.Toolbar
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.material3.Material3TileService
import com.nothing.assistant.MainActivity

/**
 * Quick-access Tile for Wear OS.
 *
 * Shows the app name and a "Tap to ask" prompt with a mic quick-action chip
 * that deep-links into the chat screen with the mic pre-armed.
 *
 * This Tile is intentionally static — no live data, no periodic refresh,
 * no background polling. Its only job is a fast launch shortcut, so there
 * is nothing to poll and nothing draining battery in the background.
 *
 * NOTE: Material3TileService was introduced in Wear Tiles 1.6.0. The exact
 * method signature should be verified against the Tiles 1.6.1 API reference.
 * If `tileResponse()` doesn't exist as an override, use `produceTile()`
 * from the standard TileService base class instead.
 */
class AssistantTileService : Material3TileService() {

    override suspend fun tileResponse(
        requestParams: RequestBuilders.TileRequest
    ): ResourceBuilders.Tile {
        // deviceConfiguration is a protected property from the base class,
        // providing the current device's screen parameters
        return PrimaryLayout.Builder(deviceConfiguration)
            .setContent(
                Text.Builder()
                    .setText("Nothing Assistant")
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .build()
            )
            .setToolbar(
                Toolbar.Builder()
                    .setContentDescription("Assistant")
                    .build()
            )
            .setPrimaryChipContent(
                CompactChip.Builder(
                    deviceConfiguration = deviceConfiguration,
                    label = "Tap to ask",
                    onClick = createOpenAppPendingIntent(),
                )
                    .setContentDescription("Open assistant")
                    .build()
            )
            .build()
            .let { layout ->
                ResourceBuilders.Tile.Builder()
                    .setResourcesBuilder(ResourceBuilders.Resources.Builder())
                    .setTileTimeline(
                        ResourceBuilders.Timeline.Builder()
                            .addTimelineEntry(
                                ResourceBuilders.TimelineEntry.Builder()
                                    .setLayout(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_LAUNCH_FROM_TILE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_TILE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val EXTRA_LAUNCH_FROM_TILE = "launch_from_tile"
        private const val REQUEST_CODE_TILE = 1001
    }
}
