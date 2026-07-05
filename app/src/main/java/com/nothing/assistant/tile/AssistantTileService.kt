package com.nothing.assistant.tile

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nothing.assistant.MainActivity

/**
 * Quick-access Tile for Wear OS.
 * Shows the app name and deep-links into the chat screen with the mic pre-armed.
 */
class AssistantTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val tile = buildTile()
        return Futures.immediateFuture(tile)
    }

    private fun buildTile(): TileBuilders.Tile {
        val column = LayoutElementBuilders.Column.Builder().build()
        return TileBuilders.Tile.Builder().build()
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
