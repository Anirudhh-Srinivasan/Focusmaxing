package com.topdawg.focusmaxxing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topdawg.focusmaxxing.ui.theme.ArcadeColors
import com.topdawg.focusmaxxing.ui.theme.FocusmaxxingTheme

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelShapePreview() = PreviewFrame {
    Box(Modifier.size(180.dp, 88.dp).clip(PixelShape()).background(ArcadeColors.Accent), contentAlignment = Alignment.Center) {
        Text("STEPPED", color = ArcadeColors.Background)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 420)
@Composable
private fun PixelButtonPreview() = PreviewFrame {
    PixelButton("START RUN", {}, kind = PixelButtonKind.Primary)
    PixelButton("JOIN CODE", {}, kind = PixelButtonKind.Secondary)
    PixelButton("LEAVE ROOM", {}, kind = PixelButtonKind.Danger)
    PixelButton("LOCKED", {}, enabled = false)
    PixelButton("SYNCING", {}, loading = true)
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelCardPreview() = PreviewFrame {
    PixelCard(accent = ArcadeColors.Accent) { Text("PLAYER CARD", color = ArcadeColors.Text) }
    PixelCard(accent = ArcadeColors.Gold, containerColor = ArcadeColors.SurfaceHigh) { Text("RANK BORDER", color = ArcadeColors.Text) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 330)
@Composable
private fun PixelTextFieldPreview() = PreviewFrame {
    PixelTextField("top_dawg", {}, "USERNAME", placeholder = "3–20 characters")
    PixelTextField("x", {}, "ROOM CODE", errorMessage = "Enter a valid six-character code.")
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelProgressBarPreview() = PreviewFrame {
    PixelProgressBar(0f, Modifier.fillMaxWidth(), contentDescription = "Empty health")
    PixelProgressBar(.58f, Modifier.fillMaxWidth(), color = ArcadeColors.Info, contentDescription = "Partial health")
    PixelProgressBar(1f, Modifier.fillMaxWidth(), color = ArcadeColors.Gold, contentDescription = "Full health")
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelDialogPreview() = FocusmaxxingTheme {
    PixelDialog(onDismissRequest = {}, title = "LEAVE SESSION", confirmButton = { PixelButton("LEAVE", {}, kind = PixelButtonKind.Danger, modifier = Modifier.width(100.dp)) }, dismissButton = { PixelButton("STAY", {}, kind = PixelButtonKind.Secondary, modifier = Modifier.width(100.dp)) }) {
        Text("Your unfinished segment will be lost.", color = ArcadeColors.Text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 380)
@Composable
private fun PixelTopBarPreview() = PreviewFrame {
    PixelTopBar("FOCUSMAXXING", navigation = { PixelButton("‹", {}, kind = PixelButtonKind.Secondary, modifier = Modifier.width(48.dp)) })
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 380)
@Composable
private fun PixelSnackbarPreview() = PreviewFrame {
    PixelSnackbar("Checkpoint saved. Nice work.", actionLabel = "UNDO", onAction = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelLoadingPreview() = PreviewFrame {
    PixelLoadingIndicator(contentDescription = "Loading public lobbies")
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PixelEmptyStatePreview() = PreviewFrame {
    PixelEmptyState("NO LOBBIES YET", "Create one and bring your squad.", sprite = ArcadeSprites.Controller) {
        PixelButton("CREATE LOBBY", {}, kind = PixelButtonKind.Primary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 270)
@Composable
private fun PixelSpriteGalleryPreview() = PreviewFrame {
    val sprites = listOf(ArcadeSprites.Flame, ArcadeSprites.Bolt, ArcadeSprites.Trophy, ArcadeSprites.Crown, ArcadeSprites.Controller, ArcadeSprites.Paw, ArcadeSprites.Lock, ArcadeSprites.Check, ArcadeSprites.Cross)
    sprites.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            row.forEach { sprite -> PixelSprite(sprite.rows, sprite.palette, sprite.description, Modifier.size(32.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 380)
@Composable
private fun RankBadgePreview() = PreviewFrame {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        RankBadge("Bronze", size = RankBadgeSize.Small)
        RankBadge("Diamond", size = RankBadgeSize.Medium)
    }
    RankBadge("Grandmaster", size = RankBadgeSize.Large)
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun StatChipPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip(ArcadeSprites.Flame, "12", label = "STREAK")
        StatChip(ArcadeSprites.Bolt, "4,250", label = "XP")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, widthDp = 380)
@Composable
private fun RankProgressPreview() = PreviewFrame {
    RankProgress("Gold", xp = 3_250, xpToNext = 1_750, progress = .43f)
}

@Composable
private fun PreviewFrame(content: @Composable ColumnScope.() -> Unit) = FocusmaxxingTheme {
    Column(
        Modifier.fillMaxSize().background(ArcadeColors.Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
