/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

import com.android.bluetooth.bthelper.pods.Pod

class AirPodsPro(color: String?, leftPod: Pod, rightPod: Pod, casePod: Pod) :
    RegularPods(color, leftPod, rightPod, casePod) {
    override val drawable: Int
        get() = R.drawable.AirPods_Pro
    override val leftDrawable: Int
        get() = R.drawable.AirPods_Pro_Left
    override val rightDrawable: Int
        get() = R.drawable.AirPods_Pro_Right
    override val caseDrawable: Int
        get() = R.drawable.AirPods_Pro_Case
    override val model: String?
        get() = Constants.MODEL_AIRPODS_PRO
    override val menufacturer: String?
        get() = Constants.MANUFACTURER_APPLE
}