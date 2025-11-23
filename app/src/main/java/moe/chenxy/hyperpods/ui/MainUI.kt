package moe.chenxy.hyperpods.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.highcapable.yukihookapi.hook.factory.prefs
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import moe.chenxy.hyperpods.MainActivity
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.NoiseControlMode
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Info
import top.yukonga.miuix.kmp.icon.icons.useful.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Volatile
var restoreAncJob: Job? = null

var currentPodsInfo: AACPManager.Companion.AirPodsInformation? = null

fun setBoolPrefsAndSync(context: Context, prefKey: String, value: Boolean) {
    context.prefs().edit {
        putBoolean(prefKey, value)
    }
    syncToController(context, prefKey)
}

fun syncToController(context: Context, prefKey: String) {
    Intent(HyperPodsAction.ACTION_PODS_SETTINGS_CHANGED).apply {
        this.putExtra("key", prefKey)
        context.sendBroadcast(this)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(FlowPreview::class)
@Composable
fun MainUI() {
    val topAppBarScrollBehavior0 = MiuixScrollBehavior(rememberTopAppBarState())
    val topAppBarScrollBehavior1 = MiuixScrollBehavior(rememberTopAppBarState())

    val topAppBarScrollBehaviorList = listOf(
        topAppBarScrollBehavior0, topAppBarScrollBehavior1
    )

    val pagerState = rememberPagerState(pageCount = { 2 })
    var targetPage by remember { mutableIntStateOf(pagerState.currentPage) }
    val coroutineScope = rememberCoroutineScope()

    val currentScrollBehavior = when (pagerState.currentPage) {
        0 -> topAppBarScrollBehaviorList[0]
        else -> topAppBarScrollBehaviorList[1]
    }

    val mainTitle = remember { mutableStateOf("") }
    val aboutTitle = stringResource(R.string.about_hyperpods)
    val currentTitle = when (pagerState.currentPage) {
        0 -> mainTitle.value
        else -> aboutTitle
    }

    val items = listOf(
        NavigationItem(stringResource(R.string.pod_info), MiuixIcons.Useful.Settings),
        NavigationItem(stringResource(R.string.about_hyperpods), MiuixIcons.Useful.Info),
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.debounce(150).collectLatest {
            targetPage = pagerState.currentPage
        }
    }
    val context = LocalContext.current

    val earDetectionEnable = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.EAR_DETECTION, true)) }
    val earDetectionParams = remember { mutableStateOf(EarDetectionParams()) }
    val batteryParams = remember { mutableStateOf(BatteryParams()) }
    val autoSwitchToSpeaker = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.EAR_DETECTION_SWITCH_SPEAKER, true)) }
    val personlizedVolume = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.PERSONLIZED_VOLUME, false)) }
    val caseChargingSound = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.CASE_CHARGING_SOUND, true)) }
    val adaptiveAudioLevel = remember { mutableFloatStateOf(context.prefs().getFloat(HyperPodsPrefsKey.ADAPTIVE_AUDIO_LEVEL, 0.5f)) }
    val adjustVolumeBySwiper = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.ADJUST_VOLUME_BY_SWIPER, false)) }
    val conversationAwareness = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.CONVERSATION_AWARENESS, false)) }
    val loudSoundReduction = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.LOUD_SOUND_REDUCTION, false)) }
    val longPressModeLeft = remember { mutableIntStateOf(context.prefs().getInt(HyperPodsPrefsKey.LONG_PRESS_MODE_LEFT, 0)) }
    val longPressModeRight = remember { mutableIntStateOf(context.prefs().getInt(HyperPodsPrefsKey.LONG_PRESS_MODE_RIGHT, 0)) }
    val noiseCancellationSingleAirPod = remember { mutableStateOf(context.prefs().getBoolean(HyperPodsPrefsKey.SINGLE_POD_ANC, false)) }
    val canShowDetailPage = remember { mutableStateOf(false) }
    val ancMode = remember { mutableStateOf(NoiseControlMode.OFF) }
    val realAncMode = remember { mutableStateOf(NoiseControlMode.OFF) }
    val microphoneMode = remember { mutableStateOf(0) }
    val init = remember { mutableStateOf(false) }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                HyperPodsAction.ACTION_PODS_ANC_CHANGED -> {
                    restoreAncJob?.cancel()
                    ancMode.value =
                        NoiseControlMode.entries.toTypedArray()[p1.getIntExtra("status", 1) - 1]
                    realAncMode.value =
                        NoiseControlMode.entries.toTypedArray()[p1.getIntExtra("status", 1) - 1]
                }

                HyperPodsAction.ACTION_EAR_DETECTION_STATUS_CHANGED -> {
                    earDetectionParams.value =
                        p1.getParcelableExtra("status", EarDetectionParams::class.java)!!
                }

                HyperPodsAction.ACTION_PODS_BATTERY_CHANGED -> {
                    batteryParams.value = p1.getParcelableExtra("status", BatteryParams::class.java)!!
                }

                HyperPodsAction.ACTION_PODS_CONNECTED -> {
                    val deviceInfo = p1.getParcelableExtra("device_info", AACPManager.Companion.AirPodsInformation::class.java)
                    val deviceName = p1.getStringExtra("device_name")
                    mainTitle.value = deviceInfo?.name ?: (deviceName ?: "")
                    canShowDetailPage.value = true
                    Log.i("Art_Chen", "pod connected deviceName: ${mainTitle.value}")
                    currentPodsInfo = deviceInfo
                }

                HyperPodsAction.ACTION_PODS_DISCONNECTED -> {
                    mainTitle.value = ""
                    canShowDetailPage.value = false
                    if (p0 is MainActivity) {
                        p0.finish()
                    }
                }
            }
        }
    }

    if (!init.value) {
        context.registerReceiver(broadcastReceiver, IntentFilter().apply {
            this.addAction(HyperPodsAction.ACTION_PODS_ANC_CHANGED)
            this.addAction(HyperPodsAction.ACTION_EAR_DETECTION_STATUS_CHANGED)
            this.addAction(HyperPodsAction.ACTION_PODS_BATTERY_CHANGED)
            this.addAction(HyperPodsAction.ACTION_PODS_CONNECTED)
            this.addAction(HyperPodsAction.ACTION_PODS_DISCONNECTED)
        }, Context.RECEIVER_EXPORTED)

        context.sendBroadcast(Intent(HyperPodsAction.ACTION_PODS_UI_INIT))
        init.value = true
    }

    fun setAncMode(mode: NoiseControlMode) {
        if (restoreAncJob?.isActive == true) {
            restoreAncJob?.cancel()
        }
        Intent(HyperPodsAction.ACTION_ANC_SELECT).apply {
            this.putExtra("status", mode.ordinal + 1)
            context.sendBroadcast(this)
        }
        restoreAncJob = CoroutineScope(Dispatchers.Default).launch {
            ancMode.value = mode
            // Wait the AirPods return the new ANC status, if timeout then restore the ui to old ANC mode
            delay(3000)
            ancMode.value = realAncMode.value
        }
    }

    fun setEarDetection(main: Boolean, disconnect: Boolean) {
        context.prefs().edit {
            putBoolean(HyperPodsPrefsKey.EAR_DETECTION, main)
            putBoolean(HyperPodsPrefsKey.EAR_DETECTION_SWITCH_SPEAKER, disconnect)
        }
        syncToController(context = context, HyperPodsPrefsKey.EAR_DETECTION)
    }

    fun renameAirPods(it: String) {
        Log.i("Art_Chen", "Try rename to $it")
    }

    fun onMicrophoneModeChange(it: Int) {
        microphoneMode.value = it

        context.prefs().edit {
            putInt(HyperPodsPrefsKey.MICROPHONE_MODE, it)
        }
        syncToController(context, HyperPodsPrefsKey.MICROPHONE_MODE)
    }

    fun onListeningModeChange(it: Byte) {
        context.prefs().edit {
            putInt(HyperPodsPrefsKey.LISTENING_MODE_BYTE, it.toInt())
        }
        syncToController(context, HyperPodsPrefsKey.LISTENING_MODE_BYTE)
    }

    fun onNoiseCancellationSingleAirPodChange(it: Boolean) {
        noiseCancellationSingleAirPod.value = it

        setBoolPrefsAndSync(context = context, prefKey = HyperPodsPrefsKey.SINGLE_POD_ANC, value = it)
    }

    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = if (currentScrollBehavior.state.heightOffset > -1) Color.Transparent else MiuixTheme.colorScheme.background,
        tint = HazeTint(
            MiuixTheme.colorScheme.background.copy(
                if (currentScrollBehavior.state.heightOffset > -1) 1f
                else lerp(1f, 0.85f, (currentScrollBehavior.state.heightOffset + 1) / -143f)
            )
        )
    )
    val hazeStyleNavBar = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(
            MiuixTheme.colorScheme.background.copy(0.5f)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BoxWithConstraints {
                if (maxWidth > 840.dp) {
                    SmallTopAppBar(
                        color = Color.Transparent,
                        title = currentTitle,
                        modifier = Modifier
                            .hazeEffect(
                                hazeState
                            ) {
                                style = hazeStyle
                                blurRadius = 25.dp
                                noiseFactor = 0f
                                progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0.5f)
                            },
                        scrollBehavior = currentScrollBehavior
                    )
                } else {
                    TopAppBar(
                        color = Color.Transparent,
                        title = currentTitle,
                        scrollBehavior = currentScrollBehavior,
                        modifier = Modifier
                            .hazeEffect(
                                hazeState
                            ) {
                                style = hazeStyle
                                blurRadius = 25.dp
                                noiseFactor = 0f
                                progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                            }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(
                        hazeState
                    ) {
                        style = hazeStyleNavBar
                        blurRadius = 25.dp
                        noiseFactor = 0f
                    },
                items = items,
                selected = targetPage,
                onClick = { index ->
                    if (index in 0..2) {
                        targetPage = index
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                }
            )
        },
    ) { padding ->
        AppHorizontalPager(
            modifier = Modifier
                .imePadding()
                .hazeSource(state = hazeState),
            pagerState = pagerState,
            topAppBarScrollBehaviorList = topAppBarScrollBehaviorList,
            padding = padding,
            canShowDetailPage = canShowDetailPage.value,
            batteryParams = batteryParams.value,
            earDetectionParams = earDetectionParams.value,
            earDetectionEnable = earDetectionEnable.value,
            onEarDetectionChanged = {
                earDetectionEnable.value = it
                setEarDetection(it, it && autoSwitchToSpeaker.value)
            },
            autoSwitchToSpeaker = autoSwitchToSpeaker.value,
            onAutoSwitchToSpeakerChange = {
                autoSwitchToSpeaker.value = it
                setEarDetection(earDetectionEnable.value, earDetectionEnable.value && it)
            },
            personlizedVolume = personlizedVolume.value,
            onPersonlizedVolumeChange = {
                personlizedVolume.value = it
                setBoolPrefsAndSync(context, HyperPodsPrefsKey.PERSONLIZED_VOLUME, it)
            },
            conversationAwareness = conversationAwareness.value,
            onConversationAwarenessChange = {
                conversationAwareness.value = it
                setBoolPrefsAndSync(context, HyperPodsPrefsKey.CONVERSATION_AWARENESS, it)
            },
            loudSoundReduction = loudSoundReduction.value,
            onLoudSoundReductionChange = {
                loudSoundReduction.value = it
                setBoolPrefsAndSync(context, HyperPodsPrefsKey.LOUD_SOUND_REDUCTION, it)
            },
            adjustVolumeBySwiper = adjustVolumeBySwiper.value,
            onAdjustVolumeBySwiperChange = {
                adjustVolumeBySwiper.value = it
                setBoolPrefsAndSync(context, HyperPodsPrefsKey.ADJUST_VOLUME_BY_SWIPER, it)
            },
            caseChargingSound = caseChargingSound.value,
            onCaseChargingSoundChange = {
                caseChargingSound.value = it
                setBoolPrefsAndSync(context, HyperPodsPrefsKey.CASE_CHARGING_SOUND, it)
            },
            adaptiveAudioLevel = adaptiveAudioLevel.floatValue,
            onAdaptiveAudioLevelChange = {
                adaptiveAudioLevel.floatValue = it
                context.prefs().edit {
                    putFloat(HyperPodsPrefsKey.ADAPTIVE_AUDIO_LEVEL, it)
                }
                syncToController(context, HyperPodsPrefsKey.ADAPTIVE_AUDIO_LEVEL)
            },
            longPressModeLeft = longPressModeLeft.intValue,
            onLongPressModeLeftChange = {
                longPressModeLeft.intValue = it
                context.prefs().edit {
                    putInt(HyperPodsPrefsKey.LONG_PRESS_MODE_LEFT, it)
                }
                syncToController(context, HyperPodsPrefsKey.LONG_PRESS_MODE_LEFT)
            },
            longPressModeRight = longPressModeRight.intValue,
            onLongPressModeRightChange = {
                longPressModeRight.intValue = it
                context.prefs().edit {
                    putInt(HyperPodsPrefsKey.LONG_PRESS_MODE_RIGHT, it)
                }
                syncToController(context, HyperPodsPrefsKey.LONG_PRESS_MODE_RIGHT)
            },
            ancMode = ancMode.value,
            onAncModeChange = {
                setAncMode(it)
            },
            onNameChange = {
                renameAirPods(it)
            },
            microphoneMode = microphoneMode.value,
            onMicrophoneModeChange = {
                onMicrophoneModeChange(it)
            },
            onListeningModeChange = {
                onListeningModeChange(it)
            },
            onNoiseCancellationSingleAirPodChange = {
                onNoiseCancellationSingleAirPodChange(it)
            },
            noiseCancellationSingleAirPod = noiseCancellationSingleAirPod.value
        )
    }
}

@Composable
fun AppHorizontalPager(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    topAppBarScrollBehaviorList: List<ScrollBehavior>,
    padding: PaddingValues,
    canShowDetailPage: Boolean,
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
    ancMode: NoiseControlMode,
    onAncModeChange: (NoiseControlMode) -> Unit,
    onNameChange: (String) -> Unit,
    microphoneMode: Int,
    onMicrophoneModeChange: (Int) -> Unit,
    onListeningModeChange: (Byte) -> Unit,
    onNoiseCancellationSingleAirPodChange: (Boolean) -> Unit,
    noiseCancellationSingleAirPod: Boolean,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        pageContent = { page ->
            when (page) {
                0 -> Crossfade(canShowDetailPage, label = "MainUIShowDetailAnim") { value ->
                        if (value) {
                            PodDetailPage(
                                topAppBarScrollBehavior = topAppBarScrollBehaviorList[0],
                                padding = padding,
                                batteryParams = batteryParams,
                                earDetectionParams = earDetectionParams,
                                earDetectionEnable = earDetectionEnable,
                                onEarDetectionChanged = onEarDetectionChanged,
                                autoSwitchToSpeaker = autoSwitchToSpeaker,
                                onAutoSwitchToSpeakerChange = onAutoSwitchToSpeakerChange,
                                personlizedVolume = personlizedVolume,
                                onPersonlizedVolumeChange = onPersonlizedVolumeChange,
                                conversationAwareness = conversationAwareness,
                                onConversationAwarenessChange = onConversationAwarenessChange,
                                loudSoundReduction = loudSoundReduction,
                                onLoudSoundReductionChange = onLoudSoundReductionChange,
                                adjustVolumeBySwiper = adjustVolumeBySwiper,
                                onAdjustVolumeBySwiperChange = onAdjustVolumeBySwiperChange,
                                caseChargingSound = caseChargingSound,
                                onCaseChargingSoundChange = onCaseChargingSoundChange,
                                adaptiveAudioLevel = adaptiveAudioLevel,
                                onAdaptiveAudioLevelChange = onAdaptiveAudioLevelChange,
                                longPressModeLeft = longPressModeLeft,
                                onLongPressModeLeftChange = onLongPressModeLeftChange,
                                longPressModeRight = longPressModeRight,
                                onLongPressModeRightChange = onLongPressModeRightChange,
                                ancMode = ancMode,
                                onAncModeChange = onAncModeChange,
                                podsInfo = currentPodsInfo,
                                onNameChange = onNameChange,
                                microphoneMode = microphoneMode,
                                onMicrophoneModeChange = onMicrophoneModeChange,
                                onListeningModeChange = onListeningModeChange,
                                onNoiseCancellationSingleAirPodChange = onNoiseCancellationSingleAirPodChange,
                                noiseCancellationSingleAirPod = noiseCancellationSingleAirPod,
                            )
                        } else {
                            WaitingPodsPage()
                        }
                    }

                1 -> AboutPage(
                    topAppBarScrollBehavior = topAppBarScrollBehaviorList[1],
                    padding = padding
                )
            }
        }
    )
}

//@Composable
//@Preview
//fun PodDetailPreview() {
//    val ancMode = remember { mutableStateOf(NoiseControlMode.OFF) }
//    val earDetectionEnable = remember { mutableStateOf(true) }
//    val autoSwitchToSpeaker = remember { mutableStateOf(true) }
//    val earDetectionParams = remember { mutableStateOf(EarDetectionParams()) }
//    val batteryParams = remember { mutableStateOf(BatteryParams()) }
//    AppTheme {
//        Scaffold {
//            PodDetailPage(
//                topAppBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState()),
//                padding = PaddingValues(),
//                earDetectionParams = earDetectionParams.value,
//                batteryParams = batteryParams.value,
//                earDetectionEnable = earDetectionEnable.value,
//                onEarDetectionChanged = { earDetectionEnable.value = it },
//                autoSwitchToSpeaker = autoSwitchToSpeaker.value,
//                onAutoSwitchToSpeakerChange = { autoSwitchToSpeaker.value = it },
//                ancMode = ancMode.value,
//                onAncModeChange = { ancMode.value = it },
//            )
//        }
//    }
//}

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable
fun Float.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }