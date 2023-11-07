/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

import com.android.bluetooth.bthelper.pods.Pod

abstract class SinglePods(color: String?, pod: Pod) : IPods {
    abstract val drawable: Int
    private val pod: Pod
    val color: String?

    init {
        this.pod = pod
        this.color = color
    }

    fun getPod(): Pod {
        return pod
    }

    val parsedStatus: Int
        get() = pod.parseStatus()
    override val isSingle: Boolean
        get() = true
    override val isDisconnected: Boolean
        get() = pod.isDisconnected()
    override val lowBattThreshold: Int
        get() =// Most AirPods have same Low Battery Threshold to 20
            20
    override val menufacturer: String?
        get() = Constants.UNKNOWN
    val isInEar: Boolean
        get() = pod.isInEar()
    val isCharging: Boolean
        get() = pod.isCharging()
}