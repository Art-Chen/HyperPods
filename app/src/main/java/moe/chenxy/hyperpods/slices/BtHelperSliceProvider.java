/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.slices;

import android.annotation.ColorInt;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;

import moe.chenxy.hyperpods.Constants;
import moe.chenxy.hyperpods.R;

public class BtHelperSliceProvider extends SliceProvider {
    private Context mContext;
    private static SharedPreferences mSharedPrefs;

    /**
     * Constant representing infinity.
     */
    private static final long INFINITY = -1;

    @Override
    public boolean onCreateSliceProvider() {
        try {
            mContext = getContext();
            mSharedPrefs =
                    mContext.getSharedPreferences(
                            Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);
        } catch (NullPointerException e) {
        }
        return true;
    }

    @Override
    public Slice onBindSlice(Uri sliceUri) {
        if (mContext == null) {
            return null;
        }
        final String path = sliceUri.getPath();
        switch (path) {
            case Constants.SLICE_BTHELPER:
                return createBtHelperSlice(sliceUri);
        }
        return null;
    }

    private Slice createBtHelperSlice(Uri sliceUri) {
        try {
            if (mContext == null) return null;
        } catch (NullPointerException e) {
        }

        /*
                final String ONEPOD_TITLE = mContext.getString(R.string.onepod_mode_title);
                final boolean onePodModeEnabled = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false);

                final String AUTO_PLAY_TITLE = mContext.getString(R.string.auto_play_title);
                final boolean autoPlayEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false);

                final String AUTO_PAUSE_TITLE = mContext.getString(R.string.auto_pause_title);
                final boolean autoPauseEnabled = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false);
        */

        final String MORE_SETTINGS_TITLE = mContext.getString(R.string.more_settings_title);
        final String MORE_SETTINGS_SUBTITLE = mContext.getString(R.string.more_settings_subtitle);

        ListBuilder listBuilder = new ListBuilder(mContext, sliceUri, INFINITY);

        /*
                listBuilder.addRow(new SliceCreator(
                        0,
                        ONEPOD_TITLE,
                        null,
                        onePodModeEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_ONEPOD_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));

                listBuilder.addRow(new SliceCreator(
                        0,
                        AUTO_PLAY_TITLE,
                        null,
                        autoPlayEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PLAY_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));

                listBuilder.addRow(new SliceCreator(
                        0,
                        AUTO_PAUSE_TITLE,
                        null,
                        autoPauseEnabled,
                        Constants.ACTION_PENDING_INTENT,
                        Constants.EXTRA_AUTO_PAUSE_CHANGED,
                        mContext,
                        Constants.SLICE_TOGGLE
                    ).getSettingRow(sliceUri));
        */

        listBuilder.addRow(
                new SliceCreator(
                                R.drawable.ic_chevron_right,
                                MORE_SETTINGS_TITLE,
                                MORE_SETTINGS_SUBTITLE,
                                false,
                                Constants.ACTION_PENDING_INTENT,
                                Constants.EXTRA_NONE,
                                mContext,
                                Constants.SLICE_MAIN)
                        .getSettingRow(sliceUri));

        listBuilder.setAccentColor(getColorAccentDefaultColor(mContext));
        return listBuilder.build();
    }

    @ColorInt
    private static int getColorAccentDefaultColor(Context context) {
        return getColorAttrDefaultColor(context, android.R.attr.colorAccent, 0);
    }

    private static int getColorAttrDefaultColor(Context context, int attr, @ColorInt int defValue) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {attr});
        @ColorInt int colorAccent = ta.getColor(0, defValue);
        ta.recycle();
        return colorAccent;
    }
}
