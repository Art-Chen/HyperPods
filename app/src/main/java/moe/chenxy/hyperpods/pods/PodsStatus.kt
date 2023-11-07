/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods

import com.android.bluetooth.bthelper.pods.models.AirPods1

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
class PodsStatus {
    private var pods: IPods? = null

    constructor()
    constructor(status: String?) {
        if (status == null) return
        val color = status.substring(18, 20)
        val flip = isFlipped(status)
        val leftStatus =
            ("" + status[if (flip) 12 else 13]).toInt(16) // Left airpod (0-10 batt; 15=disconnected)
        val rightStatus =
            ("" + status[if (flip) 13 else 12]).toInt(16) // Right airpod (0-10 batt; 15=disconnected)
        val caseStatus = ("" + status[15]).toInt(16) // Case (0-10 batt; 15=disconnected)
        val singleStatus = ("" + status[13]).toInt(16) // Single (0-10 batt; 15=disconnected)
        val chargeStatus =
            ("" + status[14]).toInt(16) // Charge status (bit 0=left; bit 1=right; bit 2=case)
        val chargeL = chargeStatus and if (flip) 2 else 1 != 0
        val chargeR = chargeStatus and if (flip) 1 else 2 != 0
        val chargeCase = chargeStatus and 4 != 0
        val chargeSingle = chargeStatus and 1 != 0
        val inEarStatus = ("" + status[11]).toInt(16) // InEar status (bit 1=left; bit 3=right)
        val inEarL = inEarStatus and if (flip) 8 else 2 != 0
        val inEarR = inEarStatus and if (flip) 2 else 8 != 0
        val leftPod = Pod(leftStatus, chargeL, inEarL)
        val rightPod = Pod(rightStatus, chargeR, inEarR)
        val casePod = Pod(caseStatus, chargeCase, false)
        val singlePod = Pod(singleStatus, chargeSingle, false)
        val idSingle = status[7] // We don't know the full ID for all devices
        val idFull = status.substring(6, 10)

        // Detect which model
        if ("0220" == idFull) {
            pods = AirPods1(color, leftPod, rightPod, casePod) // Airpods 1st gen
        } else if ("0F20" == idFull) {
            pods = AirPods2(color, leftPod, rightPod, casePod) // Airpods 2nd gen
        } else if ("1320" == idFull) {
            pods = AirPods3(color, leftPod, rightPod, casePod) // Airpods 3rd gen
        } else if ("0E20" == idFull) {
            pods = AirPodsPro(color, leftPod, rightPod, casePod) // Airpods Pro
        } else if ("1420" == idFull) {
            pods = AirPodsPro2(color, leftPod, rightPod, casePod) // Airpods Pro 2
        } else if ("0A20" == idFull) {
            pods = AirPodsMax(color, singlePod) // Airpods Max
        }
    }

    val airpods: IPods?
        get() = pods
    val isAllDisconnected: Boolean
        get() = if (this === DISCONNECTED) true else pods.isDisconnected()

    companion object {
        val DISCONNECTED = PodsStatus()
        fun isFlipped(str: String): Boolean {
            return ("" + str[10]).toInt(16) and 0x02 == 0
        }
    }
}