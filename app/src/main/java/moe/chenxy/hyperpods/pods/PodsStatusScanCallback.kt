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
 * TODO: proper javadoc
 * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
 * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit) that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons.
 * As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods and not the guy sitting next to you on the bus.
 * What we did to workaround this issue is this:
 * - When a beacon arrives that looks like a pair of AirPods, look at the other beacons received in the last 10 seconds and get the strongest one
 * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
 * - Filter for signals stronger than -60db
 * - Decode...
 */
abstract class PodsStatusScanCallback : ScanCallback() {
    private val recentBeacons: MutableList<ScanResult> = ArrayList()
    abstract fun onStatus(status: PodsStatus)
    override fun onBatchScanResults(scanResults: List<ScanResult>) {
        for (result in scanResults) onScanResult(-1, result)
        super.onBatchScanResults(scanResults)
    }

    override fun onScanResult(callbackType: Int, result: ScanResult) {
        var result: ScanResult? = result
        try {
            if (!isAirpodsResult(result)) return
            result!!.device.address
            result = getBestResult(result)
            if (result == null || result.rssi < MIN_RSSI) return
            val status = PodsStatus(decodeResult(result))
            onStatus(status)
        } catch (t: Throwable) {
        }
    }

    private fun getBestResult(result: ScanResult): ScanResult? {
        recentBeacons.add(result)
        var strongestBeacon: ScanResult? = null
        var i = 0
        while (i < recentBeacons.size) {
            if (SystemClock.elapsedRealtimeNanos() - recentBeacons[i].timestampNanos > RECENT_BEACONS_MAX_T_NS) {
                recentBeacons.removeAt(i--)
                i++
                continue
            }
            if (strongestBeacon == null || strongestBeacon.rssi < recentBeacons[i].rssi) strongestBeacon =
                recentBeacons[i]
            i++
        }
        if (strongestBeacon != null && strongestBeacon.device.address == result.device.address) strongestBeacon =
            result
        return strongestBeacon
    }

    companion object {
        const val RECENT_BEACONS_MAX_T_NS = 10000000000L //10s
        const val AIRPODS_DATA_LENGTH = 27
        const val MIN_RSSI = -60
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
                    BluetoothAssignedNumbers.APPLE,
                    manufacturerData,
                    manufacturerDataMask
                )
                return listOf(builder.build())
            }

        private fun isAirpodsResult(result: ScanResult?): Boolean {
            return result != null && result.scanRecord != null && isDataValid(
                result.scanRecord!!.getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE)
            )
        }

        private fun isDataValid(data: ByteArray?): Boolean {
            return data != null && data.size == AIRPODS_DATA_LENGTH
        }

        private fun decodeResult(result: ScanResult?): String? {
            if (result != null && result.scanRecord != null) {
                val data =
                    result.scanRecord!!.getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE)
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