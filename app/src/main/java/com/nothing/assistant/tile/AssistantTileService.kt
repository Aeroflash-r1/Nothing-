package com.nothing.assistant.tile

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.protolayout.material.tile.Toolbar
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
 */
class AssistantTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val tile = buildTile()
        return Futures.immediateFuture(tile)
    }

    private fun buildTile(): TileBuilders.Tile {
        val layout = PrimaryLayout.Builder(deviceConfiguration)
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

        return TileBuilders.Tile.Builder()
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
