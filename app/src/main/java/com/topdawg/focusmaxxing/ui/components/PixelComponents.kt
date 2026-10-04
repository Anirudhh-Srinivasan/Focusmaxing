package com.topdawg.focusmaxxing.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.topdawg.focusmaxxing.ui.theme.ArcadeColors
import com.topdawg.focusmaxxing.ui.theme.ArcadeDimens
import com.topdawg.focusmaxxing.ui.theme.PixelHeadingFont

class PixelShape(
    private val step: Dp = ArcadeDimens.CornerStep,
    private val steps: Int = 2
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val unit = with(density) { step.toPx() }
        val corner = (unit * steps).coerceAtMost(minOf(size.width, size.height) / 2f)
        val path = Path().apply {
            moveTo(corner, 0f)
            lineTo(size.width - corner, 0f)
            for (index in steps downTo 1) {
                val y = (steps - index + 1) * unit
                val x = size.width - index * unit
                lineTo(x, y - unit)
                lineTo(x, y)
                lineTo(x + unit, y)
            }
            lineTo(size.width, size.height - corner)
            for (index in steps downTo 1) {
                val x = size.width - (steps - index + 1) * unit
                val y = size.height - index * unit
                lineTo(x + unit, y)
                lineTo(x, y)
                lineTo(x, y + unit)
            }
            lineTo(corner, size.height)
            for (index in steps downTo 1) {
                val y = size.height - (steps - index + 1) * unit
                val x = index * unit
                lineTo(x, y + unit)
                lineTo(x, y)
                lineTo(x - unit, y)
            }
            lineTo(0f, corner)
            for (index in steps downTo 1) {
                val x = (steps - index + 1) * unit
                val y = index * unit
                lineTo(x - unit, y)
                lineTo(x, y)
                lineTo(x, y - unit)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

enum class PixelButtonKind { Primary, Secondary, Danger }

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PixelButtonKind = PixelButtonKind.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = remember { PixelShape() }
    val pressedSource = remember { MutableInteractionSource() }
    val isPressed by pressedSource.collectIsPressedAsState()
    val isEnabled = enabled && !loading
    val fill = when {
        !isEnabled -> ArcadeColors.SurfaceHigh
        kind == PixelButtonKind.Primary -> ArcadeColors.Accent
        kind == PixelButtonKind.Danger -> ArcadeColors.Danger
        else -> ArcadeColors.Surface
    }
    val foreground = when {
        !isEnabled -> ArcadeColors.TextMuted
        kind == PixelButtonKind.Secondary -> ArcadeColors.Text
        else -> ArcadeColors.Background
    }
    val borderColor = when {
        !isEnabled -> ArcadeColors.Border
        kind == PixelButtonKind.Danger -> ArcadeColors.Danger
        kind == PixelButtonKind.Primary -> ArcadeColors.Accent
        else -> ArcadeColors.AccentDim
    }
    Box(
        modifier = modifier
            .heightIn(min = ArcadeDimens.MinimumTouchTarget)
            .drawHardShadow(shape, if (isEnabled) ArcadeColors.Background else Color.Transparent, ArcadeDimens.ShadowOffset),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ArcadeDimens.MinimumTouchTarget)
                .graphicsLayer { translationY = if (isPressed && isEnabled) ArcadeDimens.ShadowOffset.toPx() else 0f }
                .clip(shape)
                .background(fill)
                .border(ArcadeDimens.BorderWidth, borderColor, shape)
                .clickable(
                    interactionSource = pressedSource,
                    indication = null,
                    enabled = isEnabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = ArcadeDimens.Space4, vertical = ArcadeDimens.Space2),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                PixelLoadingIndicator(Modifier.size(20.dp), color = foreground, contentDescription = "Loading")
                Spacer(Modifier.width(ArcadeDimens.Space2))
            } else if (leading != null) {
                leading()
                Spacer(Modifier.width(ArcadeDimens.Space2))
            }
            Text(text, color = foreground, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    containerColor: Color = ArcadeColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember { PixelShape() }
    Column(
        modifier = modifier
            .drawHardShadow(shape, ArcadeColors.Background, ArcadeDimens.ShadowOffset)
            .clip(shape)
            .background(containerColor)
            .border(ArcadeDimens.BorderWidth, accent ?: ArcadeColors.Border, shape)
            .padding(ArcadeDimens.Space4),
        content = content
    )
}

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingMessage: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val shape = remember { PixelShape(steps = 1) }
    Column(modifier) {
        Text(label, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(ArcadeDimens.Space1))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ArcadeDimens.MinimumTouchTarget)
                .clip(shape)
                .background(if (enabled) ArcadeColors.SurfaceHigh else ArcadeColors.Surface)
                .border(ArcadeDimens.BorderWidth, if (errorMessage != null) ArcadeColors.Danger else ArcadeColors.Border, shape)
                .padding(horizontal = ArcadeDimens.Space3, vertical = ArcadeDimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    minLines = minLines,
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ArcadeColors.Text),
                    cursorBrush = SolidColor(ArcadeColors.Accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        val message = errorMessage ?: supportingMessage
        if (message != null) {
            Spacer(Modifier.height(ArcadeDimens.Space1))
            Text(message, color = if (errorMessage != null) ArcadeColors.Danger else ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 12,
    color: Color = ArcadeColors.Accent,
    contentDescription: String = "Progress"
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Canvas(modifier.heightIn(min = 12.dp).semantics { this.contentDescription = "$contentDescription ${(safeProgress * 100).toInt()} percent" }) {
        val gap = 2.dp.toPx()
        val widthPer = (size.width - gap * (segments - 1)) / segments
        val filled = (safeProgress * segments).toInt().coerceIn(0, segments)
        for (index in 0 until segments) {
            val left = index * (widthPer + gap)
            drawRect(
                color = if (index < filled) color else ArcadeColors.SurfaceHigh,
                topLeft = Offset(left, 0f),
                size = Size(widthPer, size.height)
            )
            drawRect(
                color = ArcadeColors.Border,
                topLeft = Offset(left, 0f),
                size = Size(widthPer, size.height),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        PixelCard(modifier.fillMaxWidth().windowInsetsPadding(safeWindowInsets()), accent = ArcadeColors.Accent) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = ArcadeColors.Text)
            Spacer(Modifier.height(ArcadeDimens.Space3))
            content()
            Spacer(Modifier.height(ArcadeDimens.Space4))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2, Alignment.End)) {
                if (dismissButton != null) Box(Modifier.weight(1f)) { dismissButton() }
                Box(Modifier.weight(1f)) { confirmButton() }
            }
        }
    }
}

/**
 * Insets for content pinned to the top of the screen: the status bar plus the display
 * cutout (notch/camera punch-hole), and the side system bars when in landscape.
 * Values are read from the window, so no system bar heights are ever hardcoded.
 */
@Composable
fun topWindowInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

/**
 * Insets for content pinned to the bottom of the screen: the navigation/gesture bar and the
 * keyboard (IME), whichever reaches higher into the window.
 */
@Composable
fun bottomWindowInsets(): WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

/**
 * Full-screen insets: keeps content clear of the status bar, navigation bar, display cutout
 * and the keyboard. Insets already applied by an ancestor are not applied twice.
 */
@Composable
fun safeWindowInsets(): WindowInsets = WindowInsets.safeDrawing

@Composable
fun PixelTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        // Background first so the bar colour extends behind the status bar / cutout,
        // then the system bar inset so the bar content starts below them.
        modifier.fillMaxWidth().background(ArcadeColors.Background)
            .windowInsetsPadding(topWindowInsets())
            .heightIn(min = 56.dp)
            .padding(horizontal = ArcadeDimens.Space3, vertical = ArcadeDimens.Space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigation?.invoke()
        Text(title, modifier = Modifier.weight(1f), color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions?.invoke(this)
    }
}

/**
 * [Scaffold] wrapper that handles window insets in one place:
 * the top bar clears the status bar and display cutout, while the content slot receives
 * padding that keeps it above the navigation bar and the keyboard (IME).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = safeWindowInsets(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { actions?.invoke(this) },
                windowInsets = topWindowInsets(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArcadeColors.Background)
            )
        },
        content = content
    )
}

@Composable
fun PixelSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    PixelCard(modifier.fillMaxWidth(), accent = ArcadeColors.Info, containerColor = ArcadeColors.SurfaceHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = ArcadeColors.Text)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(ArcadeDimens.Space2))
                PixelButton(actionLabel, onAction, kind = PixelButtonKind.Secondary, modifier = Modifier.widthIn(min = ArcadeDimens.MinimumTouchTarget))
            }
        }
    }
}

@Composable
fun PixelLoadingIndicator(
    modifier: Modifier = Modifier.size(48.dp),
    color: Color = ArcadeColors.Accent,
    contentDescription: String = "Loading"
) {
    val transition = rememberInfiniteTransition(label = "pixel-loader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 600; 3f at 600 },
            repeatMode = RepeatMode.Restart
        ),
        label = "pixel-loader-phase"
    )
    Row(modifier.semantics { this.contentDescription = contentDescription }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val active = phase.toInt() % 3 == index
            Box(Modifier.size(10.dp).background(if (active) color else ArcadeColors.Border))
        }
    }
}

@Composable
fun PixelEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    sprite: ArcadeSprite? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(modifier.fillMaxWidth().padding(ArcadeDimens.Space6), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ArcadeDimens.Space3)) {
        if (sprite != null) PixelSprite(sprite.rows, sprite.palette, sprite.description, Modifier.size(56.dp))
        Text(title, color = ArcadeColors.Text, style = MaterialTheme.typography.titleMedium)
        Text(message, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
        action?.invoke()
    }
}

data class ArcadeSprite(val rows: List<String>, val palette: Map<Char, Color>, val description: String)

object ArcadeSprites {
    private val green = ArcadeColors.Accent
    private val amber = ArcadeColors.Warning
    private val red = ArcadeColors.Danger
    private val blue = ArcadeColors.Info

    val Flame = ArcadeSprite(listOf("...GG...", "..GGG...", ".GGGGG..", ".GGYGG..", ".GYYY G.", "..YYY...", "..Y.....", "........").map { it.replace(" ", "") }, mapOf('G' to green, 'Y' to amber), "Streak flame")
    val Bolt = ArcadeSprite(listOf("...Y..", "..YY..", ".YYYY.", "...YY.", "..YY..", ".YY..."), mapOf('Y' to amber), "Experience bolt")
    val Trophy = ArcadeSprite(listOf(".YYYYYY.", "YYYYYYYY", "YY....YY", ".YY..YY.", "..YYYY..", "...YY...", ".YYYYYY.", "YYYYYYYY"), mapOf('Y' to amber), "Trophy")
    val Crown = ArcadeSprite(listOf("Y..Y..Y", "YYYYYYY", ".YYYYY.", "..YYY..", "YYYYYYY", "YYYYYYY"), mapOf('Y' to amber), "Host crown")
    val Controller = ArcadeSprite(listOf("..BBBBBB..", ".BBBBBBBB.", "BBBBBBBBBB", "BB.BB.BBBB", "BBBBBBBBBB", ".BBBBBBBB."), mapOf('B' to blue), "Game controller")
    val Paw = ArcadeSprite(listOf("..GG..GG..", ".GGGGGGGG.", "GGGGGGGGGG", "GGGGGGGGGG", ".GGGGGGGG.", "..GGGGGG..", "...GGGG..."), mapOf('G' to green), "Top Dawg pixel mascot")
    val Lock = ArcadeSprite(listOf("..YYYY..", ".YY..YY.", ".YY..YY.", "YYYYYYYY", "YY....YY", "YY.YY.YY", "YY.YY.YY", "YYYYYYYY"), mapOf('Y' to amber), "Locked")
    val Check = ArcadeSprite(listOf("......G.", ".....GG.", "G...GG..", "GG.GG...", ".GGG....", "..G....."), mapOf('G' to green), "Complete")
    val Cross = ArcadeSprite(listOf("R......R", ".R....R.", "..R..R..", "...RR...", "...RR...", "..R..R..", ".R....R.", "R......R"), mapOf('R' to red), "Not complete")
}

@Composable
fun PixelSprite(rows: List<String>, palette: Map<Char, Color>, contentDescription: String, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        if (rows.isEmpty()) return@Canvas
        val columns = rows.maxOf { it.length }.coerceAtLeast(1)
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows.size
        rows.forEachIndexed { row, pixels -> pixels.forEachIndexed { column, pixel ->
            palette[pixel]?.let { color -> drawRect(color, Offset(column * cellWidth, row * cellHeight), Size(cellWidth, cellHeight)) }
        } }
    }
}

enum class RankBadgeSize(val icon: Dp, val minHeight: Dp) { Small(20.dp, 32.dp), Medium(32.dp, 44.dp), Large(64.dp, 80.dp) }

fun rankColor(rank: String): Color = when (rank.replace(" ", "").lowercase()) {
    "bronze" -> ArcadeColors.Bronze
    "silver" -> ArcadeColors.Silver
    "gold" -> ArcadeColors.Gold
    "platinum" -> ArcadeColors.Platinum
    "diamond" -> ArcadeColors.Diamond
    "master" -> ArcadeColors.Master
    "grandmaster" -> ArcadeColors.Grandmaster
    else -> ArcadeColors.TextMuted
}

@Composable
fun RankBadge(rank: String, modifier: Modifier = Modifier, size: RankBadgeSize = RankBadgeSize.Medium, showName: Boolean = true) {
    val color = rankColor(rank)
    val shield = listOf("..OO..", ".OOOO.", "OOOOOO", "OOOOOO", ".OOOO.", "..OO..")
    Row(modifier.heightIn(min = size.minHeight).semantics { contentDescription = "$rank rank" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)) {
        PixelSprite(shield, mapOf('O' to color), "$rank rank badge", Modifier.size(size.icon))
        if (showName) Text(rank, color = color, style = if (size == RankBadgeSize.Large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatChip(sprite: ArcadeSprite, value: String, modifier: Modifier = Modifier, label: String? = null) {
    Row(modifier.heightIn(min = ArcadeDimens.MinimumTouchTarget).clip(PixelShape(steps = 1)).background(ArcadeColors.SurfaceHigh).border(ArcadeDimens.BorderWidth, ArcadeColors.Border, PixelShape(steps = 1)).padding(horizontal = ArcadeDimens.Space2, vertical = ArcadeDimens.Space1), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArcadeDimens.Space2)) {
        PixelSprite(sprite.rows, sprite.palette, sprite.description, Modifier.size(20.dp))
        Column {
            Text(value, color = ArcadeColors.Text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (label != null) Text(label, color = ArcadeColors.TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RankProgress(rank: String, xp: Int, xpToNext: Int, progress: Float, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            RankBadge(rank, size = RankBadgeSize.Small)
            Text("$xp XP", color = ArcadeColors.Text, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(ArcadeDimens.Space2))
        PixelProgressBar(progress, Modifier.fillMaxWidth().height(12.dp), contentDescription = "Rank progress")
        Spacer(Modifier.height(ArcadeDimens.Space1))
        Text("$xpToNext XP to next rank", color = ArcadeColors.TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Modifier.drawHardShadow(shape: Shape, color: Color, offset: Dp): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val path = (shape.createOutline(size, layoutDirection, this) as? Outline.Generic)?.path ?: return@drawBehind
    val px = offset.toPx()
    translate(left = px, top = px) { drawPath(path, color) }
}
