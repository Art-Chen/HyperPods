/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.slices

import android.R
import android.app.slice.Slice
import android.app.slice.SliceProvider
import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.slice.builders.ListBuilder
import moe.chenxy.hyperpods.Constants

class BtHelperSliceProvider : SliceProvider() {
    private var mContext: Context? = null
    fun onCreateSliceProvider(): Boolean {
        try {
            mContext = getContext()
            mSharedPrefs = mContext!!.getSharedPreferences(
                Constants.PREFERENCES_BTHELPER,
                Context.MODE_PRIVATE
            )
        } catch (e: NullPointerException) {
        }
        return true
    }

    fun onBindSlice(sliceUri: Uri): Slice? {
        val path = sliceUri.path
        when (path) {
            Constants.SLICE_BTHELPER -> return createBtHelperSlice(sliceUri)
        }
        return null
    }

    private fun createBtHelperSlice(sliceUri: Uri): Slice? {
        try {
            if (mContext == null) return null
        } catch (e: NullPointerException) {
        }
        val ONEPOD_TITLE = mContext!!.getString(R.string.onepod_mode_title)
        val onePodModeEnabled: Boolean = mSharedPrefs!!.getBoolean(Constants.KEY_ONEPOD_MODE, false)
        val AUTO_PLAY_TITLE = mContext!!.getString(R.string.auto_play_title)
        val autoPlayEnabled: Boolean = mSharedPrefs!!.getBoolean(Constants.KEY_AUTO_PLAY, false)
        val AUTO_PAUSE_TITLE = mContext!!.getString(R.string.auto_pause_title)
        val autoPauseEnabled: Boolean = mSharedPrefs!!.getBoolean(Constants.KEY_AUTO_PAUSE, false)
        val LOW_LATENCY_TITLE = mContext!!.getString(R.string.low_latency_audio_title)
        val LOW_LATENCY_SUBTITLE = mContext!!.getString(R.string.low_latency_audio_slice_subtitle)
        val lowLatencyEnabled: Boolean =
            mSharedPrefs!!.getBoolean(Constants.KEY_LOW_LATENCY_AUDIO, false)
        val listBuilder = ListBuilder(mContext, sliceUri, INFINITY)
        listBuilder.addRow(
            SliceCreator(
                ONEPOD_TITLE,
                null,
                onePodModeEnabled,
                Constants.ACTION_ONEPOD_CHANGED,
                mContext
            ).getSettingRow(sliceUri)
        )
        listBuilder.addRow(
            SliceCreator(
                AUTO_PLAY_TITLE,
                null,
                autoPlayEnabled,
                Constants.ACTION_AUTO_PLAY_CHANGED,
                mContext
            ).getSettingRow(sliceUri)
        )
        listBuilder.addRow(
            SliceCreator(
                AUTO_PAUSE_TITLE,
                null,
                autoPauseEnabled,
                Constants.ACTION_AUTO_PAUSE_CHANGED,
                mContext
            ).getSettingRow(sliceUri)
        )
        listBuilder.addRow(
            SliceCreator(
                LOW_LATENCY_TITLE,
                LOW_LATENCY_SUBTITLE,
                lowLatencyEnabled,
                Constants.ACTION_LOW_LATENCY_AUDIO_CHANGED,
                mContext
            ).getSettingRow(sliceUri)
        )
        listBuilder.setAccentColor(getColorAccentDefaultColor(mContext))
        return listBuilder.build()
    }

    companion object {
        private var mSharedPrefs: SharedPreferences? = null

        /**
         * Constant representing infinity.
         */
        private const val INFINITY: Long = -1
        @ColorInt
        private fun getColorAccentDefaultColor(context: Context?): Int {
            return getColorAttrDefaultColor(context, R.attr.colorAccent, 0)
        }

        private fun getColorAttrDefaultColor(
            context: Context?,
            attr: Int,
            @ColorInt defValue: Int
        ): Int {
            val ta: TypedArray = context!!.obtainStyledAttributes(intArrayOf(attr))
            @ColorInt val colorAccent: Int = ta.getColor(0, defValue)
            ta.recycle()
            return colorAccent
        }
    }
}