/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.slices;

import android.app.slice.Slice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import moe.chenxy.hyperpods.Constants;

public class SliceBroadcastReceiver extends BroadcastReceiver {
    private static SharedPreferences mSharedPrefs;
    private String action;
    private boolean enabled;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent == null || context == null) return;
            this.context = context;
            action = intent.getAction();
            if (action == null) return;
        } catch (NullPointerException e) {
            return;
        }

        final int extra = intent.getIntExtra(Constants.ACTION_PENDING_INTENT, Constants.EXTRA_NONE);

        switch (extra) {
            case Constants.EXTRA_ONEPOD_CHANGED:
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false);
                handleSliceChange(Constants.KEY_ONEPOD_MODE, enabled);
                return;
            case Constants.EXTRA_AUTO_PLAY_CHANGED:
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false);
                handleSliceChange(Constants.KEY_AUTO_PLAY, enabled);
                return;
            case Constants.EXTRA_AUTO_PAUSE_CHANGED:
                enabled = intent.getBooleanExtra(Slice.EXTRA_TOGGLE_STATE, false);
                handleSliceChange(Constants.KEY_AUTO_PAUSE, enabled);
                return;
            default:
                return;
        }
    }

    private void handleSliceChange(String key, boolean enabled) {
        try {
            if (context == null || key == null) return;
        } catch (NullPointerException e) {
            return;
        }

        SharedPreferences.Editor editor =
                context.getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE)
                        .edit();
        editor.putBoolean(key, enabled).apply();
    }
}
