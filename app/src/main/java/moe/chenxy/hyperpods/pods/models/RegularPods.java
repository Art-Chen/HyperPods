/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods.models;

import moe.chenxy.hyperpods.pods.Pod;

public class RegularPods implements IPods {

    public static final int LEFT = 0, RIGHT = 1, CASE = 2;

    private final Pod[] pods;
    private final String color;

    public RegularPods(String color, Pod leftPod, Pod rightPod, Pod casePod) {
        this.pods = new Pod[] {leftPod, rightPod, casePod};
        this.color = color;
    }

    public Pod getPod(int pos) {
        return pods[pos];
    }

    public int getParsedStatus(boolean arg, int pos) {
        return pods[pos].parseStatus(arg);
    }

    public String getColor() {
        return color;
    }

    public int getDrawable() {
        return -1;
    }

    public int getLeftDrawable() {
        return -1;
    }

    public int getRightDrawable() {
        return -1;
    }

    public int getCaseDrawable() {
        return -1;
    }

    @Override
    public String getModel() {
        return Constants.UNKNOWN;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public boolean isDisconnected() {
        return pods[LEFT].isDisconnected()
                && pods[RIGHT].isDisconnected()
                && pods[CASE].isDisconnected();
    }

    public int getLowBattThreshold() {
        // Most AirPods have same Low Battery Threshold to 20
        return 20;
    }

    public String getMenufacturer() {
        return Constants.UNKNOWN;
    }

    public boolean isInEar(int pos) {
        return pods[pos].isInEar();
    }

    public boolean isCharging(int pos) {
        return pods[pos].isCharging();
    }
}
