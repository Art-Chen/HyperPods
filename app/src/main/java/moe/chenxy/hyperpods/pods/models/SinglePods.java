/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods.models;

import moe.chenxy.hyperpods.pods.Pod;

public abstract class SinglePods implements IPods {

    public abstract int getDrawable();

    private final Pod pod;
    private final String color;

    public SinglePods(String color, Pod pod) {
        this.pod = pod;
        this.color = color;
    }

    public Pod getPod() {
        return pod;
    }

    public int getParsedStatus(boolean arg) {
        return pod.parseStatus(arg);
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public boolean isDisconnected() {
        return pod.isDisconnected();
    }

    public int getLowBattThreshold() {
        // Most AirPods have same Low Battery Threshold to 20
        return 20;
    }

    public String getMenufacturer() {
        return Constants.UNKNOWN;
    }

    public boolean isInEar() {
        return pod.isInEar();
    }

    public boolean isCharging() {
        return pod.isCharging();
    }
}
