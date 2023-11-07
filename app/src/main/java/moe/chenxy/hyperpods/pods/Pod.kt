/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods

class Pod(val status: Int, val isCharging: Boolean, val isInEar: Boolean) {

    fun parseStatus(): Int {
        return if (status == MAX_CONNECTED_STATUS) 100 else if (status < MAX_CONNECTED_STATUS) status * 10 + 5 else BluetoothDevice.BATTERY_LEVEL_UNKNOWN
    }

    val isConnected: Boolean
        get() = status <= MAX_CONNECTED_STATUS
    val isDisconnected: Boolean
        get() = status == DISCONNECTED_STATUS
    val isLowBattery: Boolean
        get() = status <= LOW_BATTERY_STATUS

    companion object {
        const val DISCONNECTED_STATUS = 15
        const val MAX_CONNECTED_STATUS = 10
        const val LOW_BATTERY_STATUS = 1
    }
}