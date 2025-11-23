package moe.chenxy.hyperpods.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.NoiseControlMode
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AncSwitch(ancStatus: NoiseControlMode,
              onAncModeChange: (NoiseControlMode) -> Unit,
              supportedAncModes: List<NoiseControlMode>) {
    val isDarkMode = isSystemInDarkTheme()
    val tabs = listOf(
        TabIconRowItem(
            text = stringResource(R.string.off),
            icon = painterResource(R.drawable.noise_cancellation),
            show = supportedAncModes.contains(NoiseControlMode.OFF),
            tint = TabRowColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isDarkMode) Color.LightGray else Color.Gray,
                selectedBackgroundColor = Color.White,
                selectedContentColor = MiuixTheme.colorScheme.primary.copy(alpha = .65f)
            )
        ),
        TabIconRowItem(
            text = stringResource(R.string.noise_cancellation_title),
            icon = painterResource(R.drawable.noise_cancellation),
            show = supportedAncModes.contains(NoiseControlMode.NOISE_CANCELLATION)
        ),
        TabIconRowItem(
            text = stringResource(R.string.transparency_title),
            icon = painterResource(R.drawable.transparency),
            show = supportedAncModes.contains(NoiseControlMode.TRANSPARENCY)
        ),
        TabIconRowItem(
            text = stringResource(R.string.adaptive_title),
            icon = painterResource(R.drawable.adaptive),
            show = supportedAncModes.contains(NoiseControlMode.ADAPTIVE)
        )
    )

    TabIconRowWithContour(
        tabs = tabs,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
        selectedTabIndex = ancStatus.ordinal,
        onTabSelected = {
            onAncModeChange(NoiseControlMode.entries[it])
        },
        colors = TabRowColors(
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHighest,
            contentColor = MiuixTheme.colorScheme.onSurfaceContainerHighest,
            selectedBackgroundColor = Color.White,
            selectedContentColor = MiuixTheme.colorScheme.primary
        )
    )
}