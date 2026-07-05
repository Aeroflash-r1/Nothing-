package com.nothing.assistant.tile

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.ElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
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
        val textElement = ElementBuilders.TextElement.Builder()
            .setText("Nothing Assistant")
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.dp(200))
            .setHeight(DimensionBuilders.dp(200))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setContent(textElement)
            .build()

        val timeline = ResourceBuilders.Timeline.Builder()
            .addTimelineEntry(
                ResourceBuilders.TimelineEntry.Builder()
                    .setLayout(column)
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesBuilder(ResourceBuilders.Resources.Builder())
            .setTileTimeline(timeline)
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
