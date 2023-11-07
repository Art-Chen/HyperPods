/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.Pod

class AirPodsMax(color: String?, singlePod: Pod) : SinglePods(color, singlePod) {
    override val drawable: Int
        get() = when (color) {
            "03" -> R.drawable.AirPods_Max_SkyBlue
            "04" -> R.drawable.AirPods_Max_Pink
            "06" -> R.drawable.AirPods_Max_Silver
            "09" -> R.drawable.AirPods_Max_SpaceGray
            "10" -> R.drawable.AirPods_Max_Green // Guess
            else -> R.drawable.AirPods_Max_Silver
        }
    override val model: String?
        get() = Constants.MODEL_AIRPODS_MAX
    override val manufacturer: String?
        get() = Constants.MANUFACTURER_APPLE
}