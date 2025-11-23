package moe.chenxy.hyperpods.utils.data

object HyperPodsAction {
    const val ACTION_PODS_UI_INIT = "chen.action.hyperpods.ui_init"
    const val ACTION_PODS_CONNECTED = "chen.action.hyperpods.pods_connected"
    const val ACTION_PODS_DISCONNECTED = "chen.action.hyperpods.pods_disconnected"
    const val ACTION_PODS_BATTERY_CHANGED = "chen.action.hyperpods.pods_battery_changed"

    const val ACTION_ANC_SELECT = "chen.action.hyperpods.anc_select"
    const val ACTION_PODS_ANC_CHANGED = "chen.action.hyperpods.pods_anc_select"
    const val ACTION_EAR_DETECTION_STATUS_CHANGED = "chen.action.hyperpods.ear_detection_status_changed"
    @Deprecated("Use ACTION_PODS_SETTINGS_CHANGED instead")
    const val ACTION_EAR_DETECTION_SWITCH_CHANGED = "chen.action.hyperpods.ear_detection_switch_changed"
    const val ACTION_GET_PODS_MAC = "chen.action.hyperpods.get_pods_mac"
    const val ACTION_PODS_MAC_RECEIVED = "chen.action.hyperpods.got_pods_mac"
    const val ACTION_PODS_SETTINGS_CHANGED = "chen.action.hyperpods.preference_changed"
    const val ACTION_PODS_STATUS_CHANGED = "chen.action.hyperpods.pods_status_changed"
}