package moe.chenxy.hyperpods.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.ui.components.AncSwitch
import moe.chenxy.hyperpods.ui.components.PodStatus
import moe.chenxy.hyperpods.ui.components.PodsInfoPage
import moe.chenxy.hyperpods.ui.components.PressAndHoldSettingPage
import moe.chenxy.hyperpods.ui.components.RenamePod
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.AirPodsModels.getModelByModelNumber
import moe.chenxy.hyperpods.utils.AirPodsPro3
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderColors
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.getWindowSize

@Composable
fun PodDetailPage(
    topAppBarScrollBehavior: ScrollBehavior,
    padding: PaddingValues,
    batteryParams: BatteryParams,
    earDetectionParams: EarDetectionParams,
    earDetectionEnable: Boolean,
    onEarDetectionChanged: (Boolean) -> Unit,
    autoSwitchToSpeaker: Boolean,
    onAutoSwitchToSpeakerChange: (Boolean) -> Unit,
    personlizedVolume: Boolean,
    onPersonlizedVolumeChange: (Boolean) -> Unit,
    conversationAwareness: Boolean,
    onConversationAwarenessChange: (Boolean) -> Unit,
    loudSoundReduction: Boolean,
    onLoudSoundReductionChange: (Boolean) -> Unit,
    adjustVolumeBySwiper: Boolean,
    onAdjustVolumeBySwiperChange: (Boolean) -> Unit,
    caseChargingSound: Boolean,
    onCaseChargingSoundChange: (Boolean) -> Unit,
    adaptiveAudioLevel: Float,
    onAdaptiveAudioLevelChange: (Float) -> Unit,
    longPressModeLeft: Int,
    onLongPressModeLeftChange: (Int) -> Unit,
    longPressModeRight: Int,
    onLongPressModeRightChange: (Int) -> Unit,
    onListeningModeChange: (Byte) -> Unit,
    ancMode: NoiseControlMode,
    onAncModeChange: (NoiseControlMode) -> Unit,
    podsInfo: AACPManager.Companion.AirPodsInformation?,
    onNameChange: (String) -> Unit,
    microphoneMode: Int,
    onMicrophoneModeChange: (Int) -> Unit,
    noiseCancellationSingleAirPod: Boolean,
    onNoiseCancellationSingleAirPodChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .height(getWindowSize().height.dp)
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
    ) {
        item {
            val cardModifier = Modifier
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            val titleModifier = Modifier

            Card(
                modifier = Modifier
                    .padding(12.dp)
            ) {
                val model = if (podsInfo != null) getModelByModelNumber(podsInfo.modelNumber) ?: AirPodsPro3() else AirPodsPro3()
                PodStatus(batteryParams, earDetectionParams, modifier = Modifier.padding(12.dp), model = model)
            }

            AncSwitch(ancMode, onAncModeChange, listOf(
                NoiseControlMode.OFF,
                NoiseControlMode.NOISE_CANCELLATION,
                NoiseControlMode.TRANSPARENCY,
                NoiseControlMode.ADAPTIVE,
            ))

            // Press & Hold Settings
            PressAndHoldSettingPage(
                longPressModeLeft,
                onLongPressModeLeftChange,
                longPressModeRight,
                onLongPressModeRightChange,
                onListeningModeChange,
                titleModifier,
                cardModifier,
            )

            // Audio
            SmallTitle(stringResource(R.string.audio_title), modifier = titleModifier)
            Card(
                modifier = cardModifier
            ) {
                SuperSwitch(
                    title = stringResource(R.string.personlized_volume_title),
                    summary = stringResource(R.string.personlized_volume_summary),
                    checked = personlizedVolume,
                    onCheckedChange = onPersonlizedVolumeChange
                )
                SuperSwitch(
                    title = stringResource(R.string.conversational_awareness_title),
                    summary = stringResource(R.string.conversational_awareness_summary),
                    checked = conversationAwareness,
                    onCheckedChange = onConversationAwarenessChange
                )
                SuperSwitch(
                    title = stringResource(R.string.loud_sound_reduction_title),
                    summary = stringResource(R.string.loud_sound_reduction_summary),
                    checked = loudSoundReduction,
                    onCheckedChange = onLoudSoundReductionChange
                )

                AnimatedVisibility(
                    visible = ancMode == NoiseControlMode.ADAPTIVE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .fillMaxWidth()
                            .padding(PaddingValues(16.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.adaptive_audio_title),
                                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = adaptiveAudioLevel,
                                onValueChange = onAdaptiveAudioLevelChange,
                                modifier = Modifier.padding(top = 8.dp),
                                effect = true,
                                showKeyPoints = true,
                                keyPoints = listOf(0f, 0.5f, 1f),
                                colors = SliderColors(
                                    foregroundColor = MiuixTheme.colorScheme.secondaryVariant,
                                    disabledForegroundColor = MiuixTheme.colorScheme.disabledPrimarySlider,
                                    backgroundColor = MiuixTheme.colorScheme.secondaryVariant,
                                    keyPointColor = Color(0x4DA3B3CD),
                                    keyPointForegroundColor = Color(0x4DA3B3CD)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.less_noise),
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                                Text(
                                    stringResource(R.string.more_noise),
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant
                                )
                            }
                        }
                    }
                }

                SuperSwitch(
                    title = stringResource(R.string.adjust_volume_by_swiper_title),
                    summary = stringResource(R.string.adjust_volume_by_swiper_summary),
                    checked = adjustVolumeBySwiper,
                    onCheckedChange = onAdjustVolumeBySwiperChange
                )
            }

//            SmallTitle(stringResource(R.string.misc_title), modifier = titleModifier)
//            Card(
//                modifier = cardModifier
//            ) {
//                SuperSwitch(
//                    title = stringResource(R.string.case_charging_sound_title),
//                    checked = caseChargingSound,
//                    onCheckedChange = onCaseChargingSoundChange
//                )
//            }

            // Auto Ear-Detection
            SmallTitle(stringResource(R.string.ear_detection_title), modifier = titleModifier)
            Card(
                modifier = cardModifier
            ) {
                SuperSwitch(
                    title = stringResource(R.string.ear_detection_title),
                    summary = stringResource(R.string.ear_detection_summary),
                    checked = earDetectionEnable,
                    onCheckedChange = onEarDetectionChanged
                )
                AnimatedVisibility(
                    visible = earDetectionEnable,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SuperSwitch(
                        title = stringResource(R.string.auto_switch_to_speaker_title),
                        summary = stringResource(R.string.auto_switch_to_speaker_summary),
                        checked = autoSwitchToSpeaker,
                        onCheckedChange = onAutoSwitchToSpeakerChange,
                    )
                }
            }

            // Mic Phone
            Card(
                modifier = cardModifier
            ) {
                SuperDropdown(
                    title = stringResource(R.string.microphone),
                    items = listOf(stringResource(R.string.microphone_auto),
                        stringResource(R.string.microphone_left),
                        stringResource(R.string.microphone_right)
                    ),
                    selectedIndex = microphoneMode,
                    onSelectedIndexChange = onMicrophoneModeChange,
                )
            }

            // Single Pod ANC Mode
            Card(
                modifier = cardModifier
            ) {
                SuperSwitch(
                    title = stringResource(R.string.noise_cancellation_single_airpod),
                    summary = stringResource(R.string.noise_cancellation_single_airpod_description),
                    checked = noiseCancellationSingleAirPod,
                    onCheckedChange = onNoiseCancellationSingleAirPodChange,
                )
            }

            // Rename AirPods & Pods Info
            podsInfo?.let {
                RenamePod(cardModifier = cardModifier, onNameChange = onNameChange, deviceName = podsInfo.name)
                PodsInfoPage(
                    titleModifier = titleModifier,
                    cardModifier = cardModifier,
                    info = it
                )
            }
        }
    }
}