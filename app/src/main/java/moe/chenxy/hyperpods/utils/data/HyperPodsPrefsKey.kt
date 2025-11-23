package moe.chenxy.hyperpods.utils.data

import moe.chenxy.hyperpods.utils.AACPManager
import kotlin.experimental.or

object HyperPodsPrefsKey {
    const val EAR_DETECTION = "ear_detection"
    const val EAR_DETECTION_SWITCH_SPEAKER = "ear_detection_switch_speaker"
    const val PERSONLIZED_VOLUME = "personlized_volume"
    const val CONVERSATION_AWARENESS = "conversation_awareness"
    const val LOUD_SOUND_REDUCTION = "loud_sound_reduction"
    const val ADJUST_VOLUME_BY_SWIPER = "adjust_volume_by_swiper"
    const val CASE_CHARGING_SOUND = "case_charging_sound"
    const val ADAPTIVE_AUDIO_LEVEL = "adaptive_audio_level"
    const val LISTENING_MODE_BYTE = "listening_mode_byte"
    const val LONG_PRESS_MODE_LEFT = "long_press_mode_left"
    const val LONG_PRESS_MODE_RIGHT = "long_press_mode_right"
    const val SINGLE_POD_ANC = "single_pod_anc"

    val preferenceDefaultMap = hashMapOf(
        Pair(EAR_DETECTION, true),
        Pair(EAR_DETECTION_SWITCH_SPEAKER, true),
        Pair(PERSONLIZED_VOLUME, true),
        Pair(CONVERSATION_AWARENESS, true),
        Pair(LOUD_SOUND_REDUCTION, true),
        Pair(ADJUST_VOLUME_BY_SWIPER, true),
        Pair(CASE_CHARGING_SOUND, true),
        Pair(ADAPTIVE_AUDIO_LEVEL, 50),
        Pair(LISTENING_MODE_BYTE, (AACPManager.Companion.ListeningMode.NC.value or AACPManager.Companion.ListeningMode.TRANSPARENCY.value).toInt()),
        Pair(LONG_PRESS_MODE_LEFT, 0),
        Pair(LONG_PRESS_MODE_RIGHT, 0),
        Pair(SINGLE_POD_ANC, false),
    )
}