/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.slices

import android.app.slice.Slice
import android.content.Context
import com.android.bluetooth.bthelper.Constants

class SliceBroadcastReceiver : BroadcastReceiver() {
    private var action: String? = null
    private var enabled = false
    private var context: Context? = null
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent == null || context == null) return
            this.context = context
            action = intent.getAction()
            if (action == null) return
        } catch (e: NullPointerException) {
            return
        }
        when (action) {
            Constants.ACTION_ONEPOD_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_ONEPOD_MODE, enabled)
                return
            }

            Constants.ACTION_AUTO_PLAY_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_AUTO_PLAY, enabled)
                return
            }

            Constants.ACTION_AUTO_PAUSE_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_AUTO_PAUSE, enabled)
                return
            }

            Constants.ACTION_LOW_LATENCY_AUDIO_CHANGED -> {
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false)
                handleSliceChange(Constants.KEY_LOW_LATENCY_AUDIO, enabled)
                return
            }

            else -> return
        }
    }

    private fun handleSliceChange(key: String?, enabled: Boolean) {
        try {
            if (context == null || key == null) return
        } catch (e: NullPointerException) {
            return
        }
        val editor: SharedPreferences.Editor = context!!.getSharedPreferences(
            Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE
        )
            .edit()
        editor.putBoolean(key, enabled).apply()
    }

    companion object {
        private val mSharedPrefs: SharedPreferences? = null
    }
}