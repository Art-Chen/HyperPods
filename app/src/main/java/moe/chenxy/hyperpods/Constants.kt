/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods

object Constants {
    /* Authority (package name) */
    const val AUTHORITY_BTHELPER = "com.android.bluetooth.bthelper"

    /* Slices Paths */
    const val PATH_BTHELPER = "bthelper"
    const val SLICE_BTHELPER = "/" + PATH_BTHELPER

    /* Slices Intent Action */
    const val ACTION_ONEPOD_CHANGED = "com.android.bluetooth.bthelper.action.ONEPOD_CHANGED"
    const val ACTION_AUTO_PLAY_CHANGED = "com.android.bluetooth.bthelper.action.AUTO_PLAY_CHANGED"
    const val ACTION_AUTO_PAUSE_CHANGED = "com.android.bluetooth.bthelper.action.AUTO_PAUSE_CHANGED"
    const val ACTION_LOW_LATENCY_AUDIO_CHANGED =
        "com.android.bluetooth.bthelper.action.LOW_LATENCY_AUDIO_CHANGED"

    /* Shared Preferences */
    const val PREFERENCES_BTHELPER = AUTHORITY_BTHELPER + "_preferences"

    /* Shared Preferences Keys */
    const val KEY_ONEPOD_MODE = "onepod_mode_pref"
    const val KEY_AUTO_PLAY = "auto_play_pref"
    const val KEY_AUTO_PAUSE = "auto_pause_pref"
    const val KEY_LOW_LATENCY_AUDIO = "low_latency_audio_pref"
}