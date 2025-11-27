package moe.chenxy.hyperpods.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.highcapable.yukihookapi.hook.factory.prefs
import kotlinx.coroutines.delay
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.LongPressEnableMode
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.AACPManager.Companion.ListeningMode
import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Confirm
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Composable
fun ListeningModeItem(title: String, image: Painter, tint: Color = LocalContentColor.current, bytes: MutableState<Byte>, check: Byte) {
    val checkBoxColors = CheckboxColors(
        checkedForegroundColor = MiuixTheme.colorScheme.primary,
        uncheckedForegroundColor = MiuixTheme.colorScheme.secondary,
        disabledCheckedForegroundColor = MiuixTheme.colorScheme.disabledPrimary,
        disabledUncheckedForegroundColor = MiuixTheme.colorScheme.disabledSecondary,
        checkedBackgroundColor = Color.Transparent,
        uncheckedBackgroundColor = Color.Transparent,
        disabledCheckedBackgroundColor = Color.Transparent,
        disabledUncheckedBackgroundColor = Color.Transparent
    )
    fun onClick() {
        bytes.value = if (bytes.value and check == check) {
            bytes.value and check.inv()
        } else {
            bytes.value or check
        }
    }
    BasicComponent(
        title = title,
        onClick = { onClick() },
        leftAction = {
            Icon(image, contentDescription = title, tint = tint, modifier = Modifier
                .height(24.dp)
                .padding(end = 12.dp))
        },
        rightActions = {
            Checkbox(
                checked = bytes.value and check == check,
                onCheckedChange = {
                    onClick()
                },
                colors = checkBoxColors
            )
        }
    )
}

@Composable
fun PressAndHoldSettingPage(
    longPressModeLeft: Int,
    onLongPressModeLeftChange: (Int) -> Unit,
    longPressModeRight: Int,
    onLongPressModeRightChange: (Int) -> Unit,
    onListeningModeChange: (Byte) -> Unit,
    titleModifier: Modifier,
    cardModifier: Modifier,
) {
    var settingPodIndex by remember { mutableIntStateOf(0) }
    val settingMode = listOf(
        stringResource(R.string.set_noise_mode),
        stringResource(R.string.set_voice_assist)
    )
    var settingModeIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var listeningModeConfigByte = remember { mutableStateOf(context.prefs().getInt(HyperPodsPrefsKey.LISTENING_MODE_BYTE, 0).toByte()) }

    fun getLongPressMode(isLeft: Boolean): (Int) -> Unit {
        return if (isLeft) onLongPressModeLeftChange else onLongPressModeRightChange
    }

    val showBottomSheet = remember { mutableStateOf(false) }

    SmallTitle(stringResource(R.string.press_and_hold_settings), modifier = titleModifier.padding(top = 12.dp))
    Card(
        modifier = cardModifier
    ) {
        SuperArrow(stringResource(R.string.left_pod),
            rightText = settingMode[longPressModeLeft], onClick = {
                settingPodIndex = 0
                showBottomSheet.value = true
            }
        )
        SuperArrow(stringResource(R.string.right_pod), rightText = settingMode[longPressModeRight], onClick = {
            settingPodIndex = 1
            showBottomSheet.value = true
        })
    }


    CustomSuperBottomSheet(
        show = showBottomSheet,
        backgroundColor = MiuixTheme.colorScheme.background,
        onDismissRequest = {
            showBottomSheet.value = false
        },
        leftAction = {
            IconButton(
                onClick = { showBottomSheet.value = false },
            ) {
                Icon(
                    imageVector = MiuixIcons.Useful.Cancel,
                    contentDescription = "Cancel"
                )
            }
        },
        rightAction = {
            IconButton(
                onClick = {
                    showBottomSheet.value = false
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Useful.Confirm,
                    contentDescription = "Confirm"
                )
            }
        },
        title = stringResource(R.string.press_and_hold_settings)
    ) {
        val checkBoxColors = CheckboxColors(
            checkedForegroundColor = MiuixTheme.colorScheme.primary,
            uncheckedForegroundColor = MiuixTheme.colorScheme.secondary,
            disabledCheckedForegroundColor = MiuixTheme.colorScheme.disabledPrimary,
            disabledUncheckedForegroundColor = MiuixTheme.colorScheme.disabledSecondary,
            checkedBackgroundColor = Color.Transparent,
            uncheckedBackgroundColor = Color.Transparent,
            disabledCheckedBackgroundColor = Color.Transparent,
            disabledUncheckedBackgroundColor = Color.Transparent
        )

        val targetAlpha = if (settingModeIndex == 0) 1f else 0f
        val targetScale = if (settingModeIndex == 0) 1f else 0.9f
        val targetTranslationY = if (settingModeIndex == 0) 0.dp else 16.dp

        val animateAlpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
            label = "AlphaAnimation"
        )

        val animateScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
            label = "ScaleAnimation"
        )

        val animateY by animateDpAsState(
            targetValue = targetTranslationY,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
            label = "ScaleAnimation"
        )

        Card {
            BasicComponent(
                title = stringResource(R.string.set_noise_mode),
                onClick = {
                    settingModeIndex = 0
                },
                rightActions = {
                    Checkbox(
                        checked = settingModeIndex == 0,
                        onCheckedChange = {
                            settingModeIndex = 0
                        },
                        colors = checkBoxColors
                    )
                }
            )
            BasicComponent(
                title = stringResource(R.string.set_voice_assist),
                onClick = {
                    settingModeIndex = 1
                },
                rightActions = {
                    Checkbox(
                        checked = settingModeIndex == 1,
                        onCheckedChange = {
                            settingModeIndex = 1
                        },
                        colors = checkBoxColors
                    )
                }
            )
        }

        SmallTitle(text = stringResource(R.string.set_noise_mode), insideMargin = PaddingValues(16.dp, 8.dp), modifier = Modifier.alpha(animateAlpha))

        Box(
            modifier = Modifier
                .alpha(animateAlpha)
                .offset(y = animateY)
                .graphicsLayer(scaleX = animateScale, scaleY = animateScale)
        ) {
            Card(
                modifier = Modifier.padding(0.dp)
            ) {
                ListeningModeItem(
                    check = ListeningMode.OFF.value,
                    title = stringResource(R.string.off),
                    image = painterResource(id = R.drawable.noise_cancellation),
                    tint = if (isSystemInDarkTheme()) Color.LightGray else Color.Gray,
                    bytes = listeningModeConfigByte
                )
                ListeningModeItem(
                    check = ListeningMode.NC.value,
                    title = stringResource(R.string.noise_cancellation_title),
                    image = painterResource(id = R.drawable.noise_cancellation),
                    bytes = listeningModeConfigByte
                )
                ListeningModeItem(
                    check = ListeningMode.TRANSPARENCY.value,
                    title = stringResource(R.string.transparency_title),
                    image = painterResource(id = R.drawable.transparency),
                    bytes = listeningModeConfigByte
                )
                ListeningModeItem(
                    check = ListeningMode.ADAPTIVE.value,
                    title = stringResource(R.string.adaptive_title),
                    image = painterResource(id = R.drawable.adaptive),
                    bytes = listeningModeConfigByte
                )
            }
        }
        Text(stringResource(R.string.listen_mode_summary), modifier = Modifier.padding(16.dp).alpha(animateAlpha).graphicsLayer(scaleX = animateScale, scaleY = animateScale), color = MiuixTheme.colorScheme.secondary, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
@Preview
fun PressAndHoldSettingPagePreview() {
    PressAndHoldSettingPage(
        longPressModeLeft = 0,
        onLongPressModeLeftChange = {},
        longPressModeRight = 0,
        onLongPressModeRightChange = {},
        titleModifier = Modifier,
        cardModifier = Modifier,
        onListeningModeChange = {}
    )
}