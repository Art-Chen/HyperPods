package moe.chenxy.hyperpods.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import moe.chenxy.hyperpods.R
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults.buttonColorsPrimary
import top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Cancel
import top.yukonga.miuix.kmp.icon.icons.useful.Confirm
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RenamePod(cardModifier: Modifier, onNameChange: (String) -> Unit, deviceName: String) {
    val showBottomSheet = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf(deviceName) }

    Card(modifier = cardModifier) {
        BasicComponent(
            title = stringResource(R.string.rename_title),
            titleColor = BasicComponentColors(
                color = MiuixTheme.colorScheme.primary,
                disabledColor = MiuixTheme.colorScheme.disabledOnPrimary
            ),
            onClick = {
                showBottomSheet.value = !showBottomSheet.value
            }
        )
    }

    CustomSuperBottomSheet(
        show = showBottomSheet,
        onDismissRequest = {
            focusRequester.freeFocus()
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
                    if (text != deviceName) {
                        onNameChange(text)
                    }
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Useful.Confirm,
                    contentDescription = "Confirm"
                )
            }
        },
        title = stringResource(R.string.rename_title)
    ) {
        TextField(
            value = text,
            label = deviceName,
            useLabelAsPlaceholder = true,
            singleLine = true,
            modifier = Modifier.padding(top = 10.dp, bottom = 40.dp)
                .focusRequester(focusRequester = focusRequester),
            onValueChange = {
                text = it
            },
        )

        LaunchedEffect(Unit) {
            delay(500)
            focusRequester.requestFocus()
        }
    }
}