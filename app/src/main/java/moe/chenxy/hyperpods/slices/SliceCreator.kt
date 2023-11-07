/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.slices

import android.content.Context
import android.net.Uri

class SliceCreator(
    private val title: String, private val summary: String?,
    private val enabled: Boolean, private val action: String, private val context: Context?
) {
    fun getSettingRow(sliceUri: Uri): RowBuilder {
        val settingRow = RowBuilder(sliceUri)
        settingRow.setTitle(title)
        if (summary != null) {
            settingRow.setSubtitle(summary)
        }
        settingRow.addEndItem(
            SliceAction.createToggle(
                getBroadcastIntent(action),
                null /* actionTitle */, enabled
            )
        )
        settingRow.setPrimaryAction(getMainSettingsActivity(context, sliceUri))
        return settingRow
    }

    private fun getBroadcastIntent(action: String): PendingIntent {
        val intent = Intent(action)
        intent.setClass(context, SliceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0 /* requestCode */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun getMainSettingsActivity(context: Context?, sliceUri: Uri): SliceAction {
        val intent = Intent(context, MainSettingsActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, sliceUri.hashCode(),
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        return SliceAction(
            pendingIntent,
            IconCompat.createWithResource(context, R.mipmap.ic_bthelper).toIcon(),
            null /* actionTitle */
        )
    }
}