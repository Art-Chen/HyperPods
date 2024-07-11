/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.UserHandle;

import moe.chenxy.hyperpods.pods.PodsService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StartupReceiver extends BroadcastReceiver {

    public static final Set<ParcelUuid> PodsUUIDS = new HashSet<>();

    static {
        PodsUUIDS.add(ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"));
        PodsUUIDS.add(ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74"));
    }

    private static final String ACTION_AVRCP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED";

    public static final Set<String> btActions = new HashSet<>();

    static {
        btActions.add(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        btActions.add(ACTION_AVRCP_CONNECTION_STATE_CHANGED);
        btActions.add(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        btActions.add(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        btActions.add(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        btActions.add(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        btActions.add(BluetoothAdapter.ACTION_STATE_CHANGED);
        btActions.add(BluetoothDevice.ACTION_ACL_CONNECTED);
        btActions.add(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btActions.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btActions.add(BluetoothDevice.ACTION_NAME_CHANGED);
        btActions.add(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        btActions.add(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) return;
        if (btActions.contains(Objects.requireNonNull(intent.getAction()))) {
            try {
                final int state =
                        intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) return;
                btProfileChanges(context, state, device);
            } catch (NullPointerException e) {
                return;
            }
        }
    }

    public static boolean isPods(BluetoothDevice device) {
        for (ParcelUuid uuid : device.getUuids()) {
            if (PodsUUIDS.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    private void startPodsService(Context context, BluetoothDevice device) {
        if (!isPods(device)) {
            return;
        }
        final Intent intent = new Intent(context, PodsService.class);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        context.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    private void stopPodsService(Context context) {
        context.stopServiceAsUser(new Intent(context, PodsService.class), UserHandle.CURRENT);
    }

    private void btProfileChanges(Context context, int state, BluetoothDevice device) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                startPodsService(context, device);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
            case BluetoothProfile.STATE_DISCONNECTED:
                stopPodsService(context);
                break;
        }
    }
}
