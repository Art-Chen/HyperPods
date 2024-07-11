/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods.models;

import moe.chenxy.hyperpods.Constants.Icons;
import moe.chenxy.hyperpods.pods.Pod;

public class AirPodsMax extends SinglePods {

    public AirPodsMax(String color, Pod singlePod) {
        super(color, singlePod);
    }

    @Override
    public int getDrawable() {
        switch (getColor()) {
            case "03":
                return Icons.AirPods_Max_SkyBlue;
            case "04":
                return Icons.AirPods_Max_Pink;
            case "06":
                return Icons.AirPods_Max_Silver;
            case "09":
                return Icons.AirPods_Max_SpaceGray;
            case "10":
                return Icons.AirPods_Max_Green; // Guess
            default:
                return Icons.AirPods_Max_Silver;
        }
    }

    @Override
    public String getModel() {
        return Constants.MODEL_AIRPODS_MAX;
    }

    @Override
    public String getMenufacturer() {
        return Constants.MANUFACTURER_APPLE;
    }
}
