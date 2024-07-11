/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods.models;

import moe.chenxy.hyperpods.Constants.Icons;
import moe.chenxy.hyperpods.pods.Pod;

public class AirPods3 extends RegularPods {

    public AirPods3(String color, Pod leftPod, Pod rightPod, Pod casePod) {
        super(color, leftPod, rightPod, casePod);
    }

    @Override
    public int getDrawable() {
        return Icons.AirPods_Gen3;
    }
    ;

    @Override
    public int getLeftDrawable() {
        return Icons.AirPods_Gen3_Left;
    }

    @Override
    public int getRightDrawable() {
        return Icons.AirPods_Gen3_Right;
    }

    @Override
    public int getCaseDrawable() {
        return Icons.AirPods_Gen3_Case;
    }

    @Override
    public String getModel() {
        return Constants.MODEL_AIRPODS_GEN3;
    }

    @Override
    public String getMenufacturer() {
        return Constants.MANUFACTURER_APPLE;
    }
}
