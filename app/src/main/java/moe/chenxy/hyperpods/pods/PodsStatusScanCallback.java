/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods;

import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This method aims to address the unavailability of Bluetooth addresses from incoming BLE beacons due to privacy restrictions
 * imposed by certain operating systems like Google's. Typically, the Bluetooth address of a device would be used to filter out
 * beacons from other devices. However, due to privacy concerns, access to the Bluetooth address of incoming BLE beacons is
 * restricted, making it challenging to reliably identify if a beacon originates from specific devices like AirPods.
 *
 * To work around this issue, the following steps are implemented:
 * - Upon receiving a beacon resembling a pair of AirPods, the method analyzes other beacons received within the last 10 seconds
 *   and identifies the one with the strongest signal.
 * - It then compares the fake address of the strongest beacon with the current beacon's fake address. If they match, the current
 *   beacon is used; otherwise, the strongest beacon is chosen.
 * - Additionally, signals stronger than -60dB are filtered for further processing.
 * - The method proceeds to decode the beacon data for further analysis.
 */
public abstract class PodsStatusScanCallback extends ScanCallback {

    public static final long RECENT_BEACONS_MAX_T_NS = 10000000000L; // 10s

    public static final int AIRPODS_DATA_LENGTH = 27;
    public static final int MIN_RSSI = -60;

    private final List<ScanResult> recentBeacons = new ArrayList<>();

    public abstract void onStatus(PodsStatus status);

    public static List<ScanFilter> getScanFilters() {
        byte[] manufacturerData = new byte[AIRPODS_DATA_LENGTH];
        byte[] manufacturerDataMask = new byte[AIRPODS_DATA_LENGTH];

        manufacturerData[0] = 7;
        manufacturerData[1] = 25;

        manufacturerDataMask[0] = -1;
        manufacturerDataMask[1] = -1;

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(
                BluetoothAssignedNumbers.APPLE, manufacturerData, manufacturerDataMask);

        return Collections.singletonList(builder.build());
    }

    @Override
    public void onBatchScanResults(List<ScanResult> scanResults) {
        for (ScanResult result : scanResults) onScanResult(-1, result);

        super.onBatchScanResults(scanResults);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        try {
            if (!isAirpodsResult(result)) return;

            result.getDevice().getAddress();

            result = getBestResult(result);
            if (result == null || result.getRssi() < MIN_RSSI) return;

            PodsStatus status = new PodsStatus(decodeResult(result));
            onStatus(status);
        } catch (Throwable t) {
        }
    }

    private ScanResult getBestResult(ScanResult result) {
        recentBeacons.add(result);
        ScanResult strongestBeacon = null;

        for (int i = 0; i < recentBeacons.size(); i++) {
            if (SystemClock.elapsedRealtimeNanos() - recentBeacons.get(i).getTimestampNanos()
                    > RECENT_BEACONS_MAX_T_NS) {
                recentBeacons.remove(i--);
                continue;
            }

            if (strongestBeacon == null
                    || strongestBeacon.getRssi() < recentBeacons.get(i).getRssi())
                strongestBeacon = recentBeacons.get(i);
        }

        if (strongestBeacon != null
                && Objects.equals(
                        strongestBeacon.getDevice().getAddress(), result.getDevice().getAddress()))
            strongestBeacon = result;

        return strongestBeacon;
    }

    private static boolean isAirpodsResult(ScanResult result) {
        return result != null
                && result.getScanRecord() != null
                && isDataValid(
                        result.getScanRecord()
                                .getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE));
    }

    private static boolean isDataValid(byte[] data) {
        return data != null && data.length == AIRPODS_DATA_LENGTH;
    }

    private static String decodeResult(ScanResult result) {
        if (result != null && result.getScanRecord() != null) {
            byte[] data =
                    result.getScanRecord()
                            .getManufacturerSpecificData(BluetoothAssignedNumbers.APPLE);
            if (isDataValid(data)) return decodeHex(data);
        }
        return null;
    }

    public static String decodeHex(byte[] bArr) {
        StringBuilder ret = new StringBuilder();

        for (byte b : bArr) ret.append(String.format("%02X", b));

        return ret.toString();
    }
}
