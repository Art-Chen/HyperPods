/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods;

import moe.chenxy.hyperpods.pods.models.AirPods1;
import moe.chenxy.hyperpods.pods.models.AirPods2;
import moe.chenxy.hyperpods.pods.models.AirPods3;
import moe.chenxy.hyperpods.pods.models.AirPodsMax;
import moe.chenxy.hyperpods.pods.models.AirPodsPro;
import moe.chenxy.hyperpods.pods.models.AirPodsPro2;
import moe.chenxy.hyperpods.pods.models.AirPodsPro2UsbC;
import moe.chenxy.hyperpods.pods.models.IPods;

/**
 * Decoding the beacon:
 * This was done through reverse engineering. Hopefully it's correct.
 * - The beacon coming from a pair of AirPods/Beats contains a manufacturer specific data field n°76 of 27 bytes
 * - We convert this data to a hexadecimal string
 * - The 12th and 13th characters in the string represent the charge of the left and right pods.
 * Under unknown circumstances[1], they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 14th character in the string represents the "in charge" status.
 * Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
 * - The 11th character in the string represents the in-ear detection status. Bit 1 is the left pod; Bit 3 is the right pod.
 * - The 7th character in the string represents the model
 *
 * Notes:
 * 1) - isFlipped set by bit 1 of 10th character in the string; seems to be related to in-ear detection;
 */
public class PodsStatus {

    public static final PodsStatus DISCONNECTED = new PodsStatus();

    private IPods pods;

    public PodsStatus() {}

    public PodsStatus(String status) {
        if (status == null) return;

        String color = status.substring(18, 20);

        boolean flip = isFlipped(status);

        int leftStatus =
                Integer.parseInt(
                        "" + status.charAt(flip ? 12 : 13),
                        16); // Left airpod (0-10 batt; 15=disconnected)
        int rightStatus =
                Integer.parseInt(
                        "" + status.charAt(flip ? 13 : 12),
                        16); // Right airpod (0-10 batt; 15=disconnected)
        int caseStatus =
                Integer.parseInt("" + status.charAt(15), 16); // Case (0-10 batt; 15=disconnected)
        int singleStatus =
                Integer.parseInt("" + status.charAt(13), 16); // Single (0-10 batt; 15=disconnected)

        int chargeStatus =
                Integer.parseInt(
                        "" + status.charAt(14),
                        16); // Charge status (bit 0=left; bit 1=right; bit 2=case)

        boolean chargeL = (chargeStatus & (flip ? 0b00000010 : 0b00000001)) != 0;
        boolean chargeR = (chargeStatus & (flip ? 0b00000001 : 0b00000010)) != 0;
        boolean chargeCase = (chargeStatus & 0b00000100) != 0;
        boolean chargeSingle = (chargeStatus & 0b00000001) != 0;

        int inEarStatus =
                Integer.parseInt(
                        "" + status.charAt(11), 16); // InEar status (bit 1=left; bit 3=right)

        boolean inEarL = (inEarStatus & (flip ? 0b00001000 : 0b00000010)) != 0;
        boolean inEarR = (inEarStatus & (flip ? 0b00000010 : 0b00001000)) != 0;

        Pod leftPod = new Pod(leftStatus, chargeL, inEarL);
        Pod rightPod = new Pod(rightStatus, chargeR, inEarR);
        Pod casePod = new Pod(caseStatus, chargeCase, false);
        Pod singlePod = new Pod(singleStatus, chargeSingle, false);

        char idSingle = status.charAt(7); // We don't know the full ID for all devices
        String idFull = status.substring(6, 10);

        // Detect which model
        if ("0220".equals(idFull)) {
            pods = new AirPods1(color, leftPod, rightPod, casePod); // Airpods 1st gen
        } else if ("0F20".equals(idFull)) {
            pods = new AirPods2(color, leftPod, rightPod, casePod); // Airpods 2nd gen
        } else if ("1320".equals(idFull)) {
            pods = new AirPods3(color, leftPod, rightPod, casePod); // Airpods 3rd gen
        } else if ("0E20".equals(idFull)) {
            pods = new AirPodsPro(color, leftPod, rightPod, casePod); // Airpods Pro
        } else if ("1420".equals(idFull)) {
            pods = new AirPodsPro2(color, leftPod, rightPod, casePod); // Airpods Pro 2
        } else if ("2420".equals(idFull)) {
            pods =
                    new AirPodsPro2UsbC(
                            color, leftPod, rightPod, casePod); // Airpods Pro 2 with USB‐C
        } else if ("0A20".equals(idFull)) {
            pods = new AirPodsMax(color, singlePod); // Airpods Max
        }
    }

    public static boolean isFlipped(String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    public IPods getAirpods() {
        return pods;
    }

    public boolean isAllDisconnected() {
        if (this == DISCONNECTED) return true;

        return pods.isDisconnected();
    }
}
