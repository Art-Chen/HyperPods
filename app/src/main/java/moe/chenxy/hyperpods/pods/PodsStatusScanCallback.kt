/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods

import android.bluetooth.BluetoothAssignedNumbers
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.os.SystemClock

/**
 * This method aims to address the unavailability of Bluetooth addresses from incoming BLE beacons due to privacy restrictions
 * imposed by certain operating systems like Google's. Typically, the Bluetooth address of a device would be used to filter out
 * beacons from other devices. However, due to privacy concerns, access to the Bluetooth address of incoming BLE beacons is
 * restricted, making it challenging to reliably identify if a beacon originates from specific devices like AirPods.
 *
 * To work around this issue, the following steps are implemented:
 * - Upon receiving a beacon resembling a pair of AirPods, the method analyzes other beacons received within the last 10 seconds
 * and identifies the one with the strongest signal.
 * - It then compares the fake address of the strongest beacon with the current beacon's fake address. If they match, the current
 * beacon is used; otherwise, the strongest beacon is chosen.
 * - Additionally, signals stronger than -60dB are filtered for further processing.
 * - The method proceeds to decode the beacon data for further analysis.
 */
abstract class PodsStatusScanCallback : ScanCallback() {
    private val recentBeacons: MutableList<ScanResult> = ArrayList()

    abstract fun onStatus(status: PodsStatus)

    override fun onBatchScanResults(scanResults: List<ScanResult>) {
        for (result in scanResults) onScanResult(-1, result)

        super.onBatchScanResults(scanResults)
    }

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val res: ScanResult?
        try {
            if (!isAirpodsResult(result)) return

            res = getBestResult(result)
            if (res == null || result.rssi < MIN_RSSI) return

            val status = PodsStatus(decodeResult(res))
            onStatus(status)
        } catch (_: Throwable) {
        }
    }

    private fun getBestResult(result: ScanResult): ScanResult? {
        recentBeacons.add(result)
        var strongestBeacon: ScanResult? = null

        var i = 0
        while (i < recentBeacons.size) {
            if (SystemClock.elapsedRealtimeNanos() - recentBeacons[i].timestampNanos
                > RECENT_BEACONS_MAX_T_NS
            ) {
                recentBeacons.removeAt(i--)
                i++
                continue
            }

            if (strongestBeacon == null
                || strongestBeacon.rssi < recentBeacons[i].rssi
            ) strongestBeacon = recentBeacons[i]
            i++
        }

        if (strongestBeacon != null && strongestBeacon.device.address == result.device.address) strongestBeacon =
            result

        return strongestBeacon
    }

    companion object {
        const val RECENT_BEACONS_MAX_T_NS: Long = 10000000000L // 10s

        const val AIRPODS_DATA_LENGTH: Int = 27
        const val MIN_RSSI: Int = -60

        val scanFilters: List<ScanFilter>
            get() {
                val manufacturerData = ByteArray(AIRPODS_DATA_LENGTH)
                val manufacturerDataMask = ByteArray(AIRPODS_DATA_LENGTH)

                manufacturerData[0] = 7
                manufacturerData[1] = 25

                manufacturerDataMask[0] = -1
                manufacturerDataMask[1] = -1

                val builder = ScanFilter.Builder()
                builder.setManufacturerData(
                    BluetoothAssignedNumbers.APPLE, manufacturerData, manufacturerDataMask
                )

                return listOf(builder.build())
            }

        private fun isAirpodsResult(result: ScanResult?): Boolean {
            return result != null && result.scanRecord != null && isDataValid(
                result.scanRecord!!
                    .getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE)
            )
        }

        private fun isDataValid(data: ByteArray?): Boolean {
            return data != null && data.size == AIRPODS_DATA_LENGTH
        }

        private fun decodeResult(result: ScanResult?): String? {
            if (result != null && result.scanRecord != null) {
                val data =
                    result.scanRecord!!
                        .getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE)
                if (isDataValid(data)) return decodeHex(data)
            }
            return null
        }

        fun decodeHex(bArr: ByteArray?): String {
            val ret = StringBuilder()

            for (b in bArr!!) ret.append(String.format("%02X", b))

            return ret.toString()
        }
    }
}
