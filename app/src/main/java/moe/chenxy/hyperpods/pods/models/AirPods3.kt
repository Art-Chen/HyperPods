/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.Pod

class AirPods3(color: String?, leftPod: Pod, rightPod: Pod, casePod: Pod) :
    RegularPods(color, leftPod, rightPod, casePod) {
    override val drawable: Int
        get() = R.drawable.AirPods_Gen3
    override val leftDrawable: Int
        get() = R.drawable.AirPods_Gen3_Left
    override val rightDrawable: Int
        get() = R.drawable.AirPods_Gen3_Right
    override val caseDrawable: Int
        get() = R.drawable.AirPods_Gen3_Case
    override val model: String?
        get() = Constants.MODEL_AIRPODS_GEN3
    override val manufacturer: String?
        get() = Constants.MANUFACTURER_APPLE
}