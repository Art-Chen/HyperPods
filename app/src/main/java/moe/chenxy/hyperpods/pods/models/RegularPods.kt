/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

import com.android.bluetooth.bthelper.pods.Pod

open class RegularPods(val color: String?, leftPod: Pod, rightPod: Pod, casePod: Pod) : IPods {
    private val pods: Array<Pod>

    init {
        pods = arrayOf<Pod>(leftPod, rightPod, casePod)
    }

    fun getPod(pos: Int): Pod {
        return pods[pos]
    }

    fun getParsedStatus(pos: Int): Int {
        return pods[pos].parseStatus()
    }

    open val drawable: Int
        get() = -1
    open val leftDrawable: Int
        get() = -1
    open val rightDrawable: Int
        get() = -1
    open val caseDrawable: Int
        get() = -1
    override val model: String?
        get() = Constants.UNKNOWN
    override val isSingle: Boolean
        get() = false
    override val isDisconnected: Boolean
        get() = pods[LEFT].isDisconnected() &&
                pods[RIGHT].isDisconnected() &&
                pods[CASE].isDisconnected()
    override val lowBattThreshold: Int
        get() =// Most AirPods have same Low Battery Threshold to 20
            20
    override val menufacturer: String?
        get() = Constants.UNKNOWN

    fun isInEar(pos: Int): Boolean {
        return pods[pos].isInEar()
    }

    fun isCharging(pos: Int): Boolean {
        return pods[pos].isCharging()
    }

    companion object {
        const val LEFT = 0
        const val RIGHT = 1
        const val CASE = 2
    }
}