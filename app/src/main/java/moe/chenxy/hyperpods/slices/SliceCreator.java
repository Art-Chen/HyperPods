/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.slices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import moe.chenxy.hyperpods.Constants;
import moe.chenxy.hyperpods.R;
import moe.chenxy.hyperpods.settings.MainSettingsActivity;

public class SliceCreator {
    private final int icon;
    private final String title;
    private final String summary;
    private final boolean enabled;
    private final String action;
    private final int extra;
    private final Context context;
    private final int type;

    public SliceCreator(
            int icon,
            String title,
            String summary,
            boolean enabled,
            String action,
            int extra,
            Context context,
            int type) {
        this.icon = icon;
        this.title = title;
        this.summary = summary;
        this.enabled = enabled;
        this.action = action;
        this.extra = extra;
        this.context = context;
        this.type = type;
    }

    protected ListBuilder.RowBuilder getSettingRow(Uri sliceUri) {
        ListBuilder.RowBuilder settingRow = new ListBuilder.RowBuilder(sliceUri);

        int ic = R.drawable.ic_dummy;
        if (icon != 0) ic = icon;
        IconCompat iconCompat = IconCompat.createWithResource(context, ic);

        settingRow.setTitle(title);
        if (summary != null) {
            settingRow.setSubtitle(summary);
        }

        SliceAction sa = null;
        if (type == Constants.SLICE_TOGGLE) {
            sa = getToggleSlice(iconCompat);
            settingRow.addEndItem(sa);
        } else if (type == Constants.SLICE_MAIN) {
            sa = getMainSettingsSlice(iconCompat, sliceUri);
            settingRow.setPrimaryAction(sa);
        } else {
            sa = getMainSettingsSlice(iconCompat, sliceUri);
            settingRow.setPrimaryAction(sa);
        }

        return settingRow;
    }

    private PendingIntent getBroadcastIntent() {
        final Intent intent =
                new Intent(context, SliceBroadcastReceiver.class)
                        .setAction(action)
                        .putExtra(action, extra);
        return PendingIntent.getBroadcast(
                context,
                0 /* requestCode */,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private SliceAction getToggleSlice(IconCompat iconCompat) {
        final CharSequence actionTitle = title;
        return SliceAction.createToggle(getBroadcastIntent(), iconCompat, actionTitle, enabled);
    }

    private SliceAction getMainSettingsSlice(IconCompat iconCompat, Uri sliceUri) {
        final Intent intent = new Intent(context, MainSettingsActivity.class).setAction(action);
        final PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context, sliceUri.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
        final CharSequence actionTitle = title;
        return new SliceAction(pendingIntent, iconCompat.toIcon(), actionTitle);
    }
}
