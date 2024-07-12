/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods

import moe.chenxy.hyperpods.utils.SystemApisUtils

class Pod(private val status: Int, val isCharging: Boolean, val isInEar: Boolean) {
    fun parseStatus(arg: Boolean): Int {
        if (arg) {
            return if (status == MAX_CONNECTED_STATUS || (status in 1..<MAX_CONNECTED_STATUS)) status - 1
            else SystemApisUtils.BATTERY_LEVEL_UNKNOWN
        }

        return if (status == MAX_CONNECTED_STATUS) 100
            else if (status < MAX_CONNECTED_STATUS) (status * 10)
            else SystemApisUtils.BATTERY_LEVEL_UNKNOWN
    }

    val isConnected: Boolean
        get() = status <= MAX_CONNECTED_STATUS

    val isDisconnected: Boolean
        get() = status == DISCONNECTED_STATUS

    val isLowBattery: Boolean
        get() = status <= LOW_BATTERY_STATUS

    companion object {
        const val DISCONNECTED_STATUS: Int = 15
        const val MAX_CONNECTED_STATUS: Int = 10
        const val LOW_BATTERY_STATUS: Int = 1
    }
}
