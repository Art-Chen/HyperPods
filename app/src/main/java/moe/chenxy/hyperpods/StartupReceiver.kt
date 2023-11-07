/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods

import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.Objects

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent == null || context == null) return
        if (btActions.contains(Objects.requireNonNull<String>(intent.getAction()))) {
            try {
                val state: Int =
                    intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR)
                val device: BluetoothDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                btProfileChanges(context, state, device)
            } catch (e: NullPointerException) {
                return
            }
        }
    }

    private fun startPodsService(context: Context, device: BluetoothDevice) {
        if (!isPods(device)) {
            return
        }
        val intent = Intent(context, PodsService::class.java)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        context.startServiceAsUser(intent, UserHandle.CURRENT)
    }

    private fun stopPodsService(context: Context) {
        context.stopServiceAsUser(
            Intent(context, PodsService::class.java),
            UserHandle.CURRENT
        )
    }

    private fun btProfileChanges(context: Context, state: Int, device: BluetoothDevice) {
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> startPodsService(context, device)
            BluetoothDevice.BOND_NONE -> {
                PodsService.shouldResetDevice(true)
                stopPodsService(context)
            }

            BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED -> stopPodsService(
                context
            )
        }
    }

    companion object {
        val PodsUUIDS: MutableSet<ParcelUuid> = HashSet<ParcelUuid>()

        init {
            PodsUUIDS.add(ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"))
            PodsUUIDS.add(ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74"))
        }

        private const val ACTION_AVRCP_CONNECTION_STATE_CHANGED =
            "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED"
        val btActions: MutableSet<String> = HashSet()

        init {
            btActions.add(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED)
            btActions.add(ACTION_AVRCP_CONNECTION_STATE_CHANGED)
            btActions.add(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED)
            btActions.add(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            btActions.add(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            btActions.add(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            btActions.add(BluetoothAdapter.ACTION_STATE_CHANGED)
            btActions.add(BluetoothDevice.ACTION_ACL_CONNECTED)
            btActions.add(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            btActions.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            btActions.add(BluetoothDevice.ACTION_NAME_CHANGED)
            btActions.add(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            btActions.add(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
        }

        fun isPods(device: BluetoothDevice): Boolean {
            for (uuid in device.getUuids()) {
                if (PodsUUIDS.contains(uuid)) {
                    return true
                }
            }
            return false
        }
    }
}