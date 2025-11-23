package moe.chenxy.hyperpods.ui.components

// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import org.jetbrains.compose.resources.vectorResource
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Blocklist
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollHorizontal

data class TabIconRowItem(
    val text: String,
    val icon: Painter,
    val show: Boolean = true,
    val tint: TabRowColors? = null
)

/**
 * A [TabIconRowWithContour] with Miuix style.
 *
 * @param tabs The text to be displayed in the [TabRow].
 * @param selectedTabIndex The selected tab index of the [TabRow]
 * @param modifier The modifier to be applied to the [TabRow].
 * @param colors The colors of the [TabRow].
 * @param minWidth The minimum width of the tab in [TabRow].
 * @param maxWidth The maximum width of the tab in [TabRow].
 * @param height The height of the [TabRow].
 * @param cornerRadius The round corner radius of the tab in [TabRow].
 * @param onTabSelected The callback when a tab is selected.
 */
@Composable
fun TabIconRowWithContour(
    tabs: List<TabIconRowItem>,
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(),
    minWidth: Dp = TabRowDefaults.TabRowWithContourMinWidth,
    maxWidth: Dp = TabRowDefaults.TabRowWithContourMaxWidth,
    height: Dp = TabRowDefaults.TabRowWithContourHeight,
    cornerRadius: Dp = TabRowDefaults.TabRowWithContourCornerRadius,
    onTabSelected: ((Int) -> Unit)? = null,
) {
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val contourPadding = 5.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(modifier)
    ) {
        val lazyRowAvailableWidth = this.maxWidth - (contourPadding * 2)
        val config = rememberTabRowConfig(tabs, minWidth, maxWidth, cornerRadius, contourPadding, lazyRowAvailableWidth)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContinuousRoundedRectangle(cornerRadius + contourPadding))
                .background(color = colors.backgroundColor(false))
                .padding(contourPadding)
        ) {
            LazyRow(
                state = config.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollHorizontal()
                    .clip(ContinuousRoundedRectangle(cornerRadius)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(contourPadding),
                overscrollEffect = null
            ) {
                itemsIndexed(tabs) { index, tabItem ->
                    if (tabItem.show) {
                        TabItemWithContour(
                            item = tabItem,
                            isSelected = selectedTabIndex == index,
                            onClick = { currentOnTabSelected?.invoke(index) },
                            enabled = currentOnTabSelected != null,
                            globalColors = colors,
                            shape = config.shape,
                            width = config.tabWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItemWithContour(
    item: TabIconRowItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    globalColors: TabRowColors,
    shape: ContinuousRoundedRectangle,
    width: Dp
) {
    val colors = item.tint ?: globalColors
    // 背景色动画
    val bgColor by animateColorAsState(
        targetValue = colors.backgroundColor(isSelected),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "TabBgAnimation"
    )

    // 内容颜色动画
    val contentColor by animateColorAsState(
        targetValue = colors.contentColor(isSelected),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "TabContentAnimation"
    )

    // 缩放动画（选中时轻微放大）
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.99f, stiffness = Spring.StiffnessLow),
        label = "TabScaleAnimation"
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .clip(shape)
            .background(bgColor)
            .scale(scale = scale)
            .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .semantics { role = Role.Tab },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                item.icon,
                contentDescription = item.text,
                tint = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Text(
                text = item.text,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Base configuration for TabRow implementations.
 */
private data class TabRowConfig(
    val tabWidth: Dp,
    val shape: ContinuousRoundedRectangle,
    val listState: androidx.compose.foundation.lazy.LazyListState
)

/**
 * Prepare common TabRow configuration.
 * @param lazyRowAvailableWidth The actual width available for the LazyRow's content (tabs + inter-tab spacing).
 */
@Composable
private fun rememberTabRowConfig(
    tabs: List<TabIconRowItem>,
    minWidth: Dp,
    maxWidth: Dp,
    cornerRadius: Dp,
    spacing: Dp,
    lazyRowAvailableWidth: Dp
): TabRowConfig {
    val listState = rememberLazyListState()
    val count = remember { mutableIntStateOf(0) }
    if (count.intValue == 0) {
        for (tab in tabs) {
            if (tab.show) count.intValue++
        }
    }
    val tabWidth = remember(count.intValue, minWidth, maxWidth, lazyRowAvailableWidth, spacing) {
        calculateTabWidth(count.intValue, minWidth, maxWidth, spacing, lazyRowAvailableWidth)
    }
    val shape = remember(cornerRadius) { ContinuousRoundedRectangle(cornerRadius) }

    return TabRowConfig(tabWidth, shape, listState)
}

private fun calculateTabWidth(
    tabCount: Int,
    minWidth: Dp,
    maxWidth: Dp,
    spacing: Dp,
    availableWidth: Dp
): Dp {
    if (tabCount == 0) return minWidth

    val totalSpacing = if (tabCount > 1) (tabCount - 1) * spacing else 0.dp
    val contentWidth = availableWidth - totalSpacing

    return if (contentWidth <= 0.dp) {
        minWidth
    } else {
        val idealWidth = contentWidth / tabCount
        when {
            idealWidth < minWidth -> minWidth
            idealWidth > maxWidth -> {
                val totalMaxWidth = maxWidth * tabCount + totalSpacing
                if (totalMaxWidth < availableWidth) {
                    idealWidth
                } else {
                    maxWidth
                }
            }

            else -> idealWidth
        }
    }
}

object TabRowDefaults {

    /**
     * The default height of the [TabRow].
     */
    val TabRowHeight = 54.dp

    /**
     * The default height of the [TabRowWithContour].
     */
    val TabRowWithContourHeight = 65.dp

    /**
     * The default corner radius of the [TabRow].
     */
    val TabRowCornerRadius = 12.dp

    /**
     * The default corner radius of the [TabRowWithContour].
     */
    val TabRowWithContourCornerRadius = 8.dp

    /**
     * The default minimum width of the [TabRow].
     */
    val TabRowMinWidth = 76.dp

    /**
     * The default minimum width of the [TabRowWithContour].
     */
    val TabRowWithContourMinWidth = 62.dp

    /**
     * The default maximum width of the tab in [TabRow].
     */
    val TabRowMaxWidth = 98.dp

    /**
     * The default minimum width of the tab in [TabRowWithContour].
     */
    val TabRowWithContourMaxWidth = 84.dp

    /**
     * The default colors for the [TabRow].
     */
    @Composable
    fun tabRowColors(
        backgroundColor: Color = MiuixTheme.colorScheme.background,
        contentColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        selectedBackgroundColor: Color = MiuixTheme.colorScheme.surface,
        selectedContentColor: Color = MiuixTheme.colorScheme.onSurface
    ): TabRowColors = TabRowColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor
    )
}

@Immutable
class TabRowColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val selectedBackgroundColor: Color,
    private val selectedContentColor: Color
) {
    @Stable
    internal fun backgroundColor(selected: Boolean): Color =
        if (selected) selectedBackgroundColor else backgroundColor

    @Stable
    internal fun contentColor(selected: Boolean): Color =
        if (selected) selectedContentColor else contentColor
}