/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2021-2023 Matthias Urhahn
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.os.IBinder
import android.os.UserHandle
import moe.chenxy.hyperpods.Constants
import moe.chenxy.hyperpods.pods.models.IPods
import moe.chenxy.hyperpods.pods.models.RegularPods
import moe.chenxy.hyperpods.pods.models.SinglePods
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.DEVICE_TYPE_UNTETHERED_HEADSET
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_COMPANION_APP
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_DEVICE_TYPE
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_ENHANCED_SETTINGS_UI_URI
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_MAIN_CHARGING
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_MAIN_ICON
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_MAIN_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_MANUFACTURER_NAME
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_MODEL_NAME
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_SOFTWARE_VERSION
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_CASE_BATTERY
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_CASE_CHARGING
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_CASE_ICON
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_LEFT_BATTERY
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_LEFT_CHARGING
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_LEFT_ICON
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_RIGHT_BATTERY
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_RIGHT_CHARGING
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_RIGHT_ICON
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.getMetadata
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.setLowLatencyAudioAllowed
import moe.chenxy.hyperpods.utils.BluetoothDeviceWrapper.setMetadata
import moe.chenxy.hyperpods.utils.MediaControl
import java.util.Locale

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 */
class PodsService : Service() {
    private var btScanner: BluetoothLeScanner? = null
    private var status: PodsStatus = PodsStatus.Companion.DISCONNECTED
    private val btReceiver: BroadcastReceiver? = null
    private var scanCallback: PodsStatusScanCallback? = null
    private var isMetaDataSet = false
    private var isSliceSet = false
    private var isModelDataSet = false
    private var statusChanged = false
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            val device: BluetoothDevice? =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                mCurrentDevice = device
                setLowLatencyAudio()
                startAirPodsScanner()
            }
        } catch (e: NullPointerException) {
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAirPodsScanner()
    }

    // Set Low Latency Audio mode to current device
    fun setLowLatencyAudio() {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, MODE_PRIVATE)
        mCurrentDevice.setLowLatencyAudioAllowed(
            mSharedPrefs.getBoolean(
                Constants.KEY_LOW_LATENCY_AUDIO,
                false
            )
        )
    }

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * After decoding a beacon, the status is written to PodsStatus.
     */
    private fun startAirPodsScanner() {
        try {
            val btManager: BluetoothManager =
                getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter: BluetoothAdapter = btManager.getAdapter() ?: return
            if (btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback)
                scanCallback = null
            }
            if (!btAdapter.isEnabled()) {
                return
            }
            btScanner = btAdapter.getBluetoothLeScanner()
            val scanSettings: ScanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1) // DON'T USE 0
                .build()
            scanCallback = object : PodsStatusScanCallback() {
                override fun onStatus(newStatus: PodsStatus) {
                    setStatusChanged(status, newStatus)
                    status = newStatus
                    handlePlayPause(status, applicationContext)
                    updatePodsStatus(status, mCurrentDevice)
                }
            }
            btScanner.startScan(getScanFilters(), scanSettings, scanCallback)
        } catch (t: Throwable) {
        }
    }

    private fun stopAirPodsScanner() {
        try {
            if (btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback)
                scanCallback = null
            }
            status = PodsStatus.Companion.DISCONNECTED
        } catch (t: Throwable) {
        }
    }

    // Set boolean value to true if device's status has changed
    private fun setStatusChanged(status: PodsStatus, newStatus: PodsStatus) {
        if (status != newStatus) {
            statusChanged = true
        }
    }

    // Handle Play/Pause media control event based on device wear status
    private fun handlePlayPause(status: PodsStatus, context: Context) {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, MODE_PRIVATE)
        val onePodMode: Boolean = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false)
        val autoPlay: Boolean = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false)
        val autoPause: Boolean = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false)
        val autoPlayPause = autoPlay && autoPause
        try {
            mediaControl = MediaControl.getInstance(context)
        } catch (e: Exception) {
        }
        if (mediaControl == null) return
        val airpods: IPods? = status.airpods
        val single: Boolean = airpods?.isSingle ?: false
        var currentWorn = false
        currentWorn = if (!single) {
            val regularPods: RegularPods = airpods as RegularPods
            if (onePodMode) {
                regularPods.isInEar(RegularPods.LEFT) || regularPods.isInEar(RegularPods.RIGHT)
            } else {
                regularPods.isInEar(RegularPods.LEFT) && regularPods.isInEar(RegularPods.RIGHT)
            }
        } else {
            val singlePods: SinglePods = airpods as SinglePods
            singlePods.isInEar
        }
        if (!previousWorn && currentWorn && !mediaControl!!.isPlaying) {
            if (autoPlayPause || autoPlay) {
                mediaControl!!.sendPlay()
            }
        } else if (previousWorn && !currentWorn && mediaControl!!.isPlaying) {
            if (autoPlayPause || autoPause) {
                mediaControl!!.sendPause()
            }
        }
        previousWorn = currentWorn
    }

    // Convert internal resource address to URI
    private fun resToUri(resId: Int): Uri? {
        return try {
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(Constants.AUTHORITY_BTHELPER)
                .appendPath(applicationContext.resources.getResourceTypeName(resId))
                .appendPath(applicationContext.resources.getResourceEntryName(resId))
                .build()
        } catch (e: Resources.NotFoundException) {
            null
        }
    }

    private fun setMetadata(device: BluetoothDevice?, key: Int, value: ByteArray): Boolean {
        return if (device.getMetadata(key) == null)
            device.setMetadata(key, value)
        else true
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private fun updatePodsStatus(status: PodsStatus, device: BluetoothDevice?) {
        val airpods: IPods? = status.airpods
        val single: Boolean = airpods.isSingle
        isSingleDevice = single
        var batteryUnified = 0
        var chargingMain = false
        if (!isMetaDataSet) {
            isSliceSet = setMetadata(
                device,
                METADATA_COMPANION_APP,
                Constants.AUTHORITY_BTHELPER.toByteArray()
            )
            isSliceSet = setMetadata(
                device,
                METADATA_SOFTWARE_VERSION,
                COMPANION_TYPE_NONE.toByteArray()
            )
            isSliceSet = setMetadata(
                device,
                METADATA_ENHANCED_SETTINGS_UI_URI,
                getUri(Constants.PATH_BTHELPER).toString().toByteArray()
            )
        }
        if (!single) {
            val regularPods: RegularPods? = airpods as RegularPods?
            if (!isMetaDataSet) {
                isModelDataSet = (setMetadata(
                    device,
                    METADATA_MANUFACTURER_NAME,
                    regularPods!!.manufacturer!!.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MODEL_NAME,
                    regularPods.model!!.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_DEVICE_TYPE,
                    DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                    (regularPods.lowBattThreshold.toString()).toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                    (regularPods.lowBattThreshold.toString()).toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                    (regularPods.lowBattThreshold.toString()).toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                    (regularPods.lowBattThreshold.toString()).toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MAIN_ICON,
                    resToUri(regularPods.drawable).toString().toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_LEFT_ICON,
                    resToUri(regularPods.leftDrawable).toString().toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_RIGHT_ICON,
                    resToUri(regularPods.rightDrawable).toString().toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_UNTETHERED_CASE_ICON,
                    resToUri(regularPods.caseDrawable).toString().toByteArray()
                ))
            }
            if (statusChanged) {
                val leftCharging: Boolean = regularPods!!.isCharging(RegularPods.LEFT)
                val rightCharging: Boolean = regularPods.isCharging(RegularPods.RIGHT)
                val caseCharging: Boolean = regularPods.isCharging(RegularPods.CASE)
                val leftBattery: Int = regularPods.getParsedStatus(RegularPods.LEFT)
                val rightBattery: Int = regularPods.getParsedStatus(RegularPods.RIGHT)
                val caseBattery: Int = regularPods.getParsedStatus(RegularPods.CASE)
                device.setMetadata(
                    METADATA_UNTETHERED_LEFT_CHARGING,
                    (leftCharging.toString() + "").uppercase(
                        Locale.getDefault()
                    ).toByteArray()
                )
                device.setMetadata(
                    METADATA_UNTETHERED_RIGHT_CHARGING,
                    (rightCharging.toString() + "").uppercase(
                        Locale.getDefault()
                    ).toByteArray()
                )
                device.setMetadata(
                    METADATA_UNTETHERED_CASE_CHARGING,
                    (caseCharging.toString() + "").uppercase(
                        Locale.getDefault()
                    ).toByteArray()
                )
                device.setMetadata(
                    METADATA_UNTETHERED_LEFT_BATTERY,
                    (leftBattery.toString() + "").toByteArray()
                )
                device.setMetadata(
                    METADATA_UNTETHERED_RIGHT_BATTERY,
                    (rightBattery.toString() + "").toByteArray()
                )
                device.setMetadata(
                    METADATA_UNTETHERED_CASE_BATTERY,
                    (caseBattery.toString() + "").toByteArray()
                )
                chargingMain = leftCharging && rightCharging
                batteryUnified = leftBattery.coerceAtMost(rightBattery)
            }
        } else {
            val singlePods: SinglePods? = airpods as SinglePods?
            if (!isMetaDataSet) {
                isModelDataSet = (setMetadata(
                    device,
                    METADATA_MANUFACTURER_NAME,
                    singlePods!!.manufacturer!!.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_DEVICE_TYPE,
                    DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MODEL_NAME,
                    singlePods.model!!.toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                    (singlePods.lowBattThreshold.toString()).toByteArray()
                )
                        && setMetadata(
                    device,
                    METADATA_MAIN_ICON,
                    resToUri(singlePods.drawable).toString().toByteArray()
                ))
            }
            chargingMain = singlePods!!.isCharging
            batteryUnified = singlePods.parsedStatus
        }
        if (statusChanged) {
            device.setMetadata(
                METADATA_MAIN_CHARGING, (chargingMain.toString() + "").uppercase(
                    Locale.getDefault()
                ).toByteArray()
            )
        }
        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet
        }
        if (statusChanged) {
            broadcastHfIndicatorEventIntent(batteryUnified, device)
            statusChanged = false
        }
    }

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    @SuppressLint("MissingPermission")
    private fun broadcastHfIndicatorEventIntent(battery: Int, device: BluetoothDevice?) {
        // Update battery status for this device
        val intent = Intent(ACTION_HF_INDICATORS_VALUE_CHANGED)
        intent.putExtra(EXTRA_HF_INDICATORS_IND_ID, HF_INDICATOR_BATTERY_LEVEL_STATUS)
        intent.putExtra(EXTRA_HF_INDICATORS_IND_VALUE, battery)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        sendBroadcastAsUserMultiplePermissions(intent, UserHandle.ALL, btPermissions)
        if (statusChanged) {
            // Update Android Settings Intelligence's battery widget
            val statusIntent: Intent =
                Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).setPackage(PACKAGE_ASI)
            statusIntent.putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent)
            sendBroadcastAsUser(statusIntent, UserHandle.ALL)
        }
    }

    companion object {
        /**
         * Intent used to broadcast the headset's indicator status
         *
         *
         * This intent will have 3 extras:
         *
         *  *  [.EXTRA_HF_INDICATORS_IND_ID] - The Assigned number of headset Indicator which
         * is supported by the headset ( as indicated by AT+BIND command in the SLC
         * sequence) or whose value is changed (indicated by AT+BIEV command)
         *  *  [.EXTRA_HF_INDICATORS_IND_VALUE] - Updated value of headset indicator.
         *  *  [BluetoothDevice.EXTRA_DEVICE] - Remote device.
         *
         *
         * [.EXTRA_HF_INDICATORS_IND_ID] is defined by Bluetooth SIG and each of the indicators
         * are given an assigned number. Below shows the assigned number of Indicator added so far
         * - Enhanced Safety - 1, Valid Values: 0 - Disabled, 1 - Enabled
         * - Battery Level - 2, Valid Values: 0~100 - Remaining level of Battery
         */
        private const val ACTION_HF_INDICATORS_VALUE_CHANGED =
            "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED"

        /**
         * A int extra field in [.ACTION_HF_INDICATORS_VALUE_CHANGED]
         * intents that contains the assigned number of the headset indicator as defined by
         * Bluetooth SIG that is being sent. Value range is 0-65535 as defined in HFP 1.7
         */
        private const val EXTRA_HF_INDICATORS_IND_ID =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID"

        /**
         * A int extra field in [.ACTION_HF_INDICATORS_VALUE_CHANGED]
         * intents that contains the value of the Headset indicator that is being sent.
         */
        private const val EXTRA_HF_INDICATORS_IND_VALUE =
            "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE"

        // Match up with bthf_hf_ind_type_t of bt_hf.h
        private const val HF_INDICATOR_BATTERY_LEVEL_STATUS = 2

        /**
         * Broadcast Action: Indicates the battery level of a remote device has
         * been retrieved for the first time, or changed since the last retrieval
         *
         * Always contains the extra fields [BluetoothDevice.EXTRA_DEVICE]
         * and [BluetoothDevice.EXTRA_BATTERY_LEVEL].
         */
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

        // Target Android Settings Intelligence package that have battery widget for data update
        private const val PACKAGE_ASI = "com.google.android.settings.intelligence"

        /**
         * Intent used to broadcast bluetooth data update
         * for the Settings Intelligence package's battery widget
         */
        private const val ACTION_ASI_UPDATE_BLUETOOTH_DATA =
            "batterywidget.impl.action.update_bluetooth_data"

        // Enhanced Settings UI Slice for BtHelper
        private const val SLICE_BTHELPER = "bthelper"
        private const val COMPANION_TYPE_NONE = "COMPANION_NONE"
        private var mCurrentDevice: BluetoothDevice? = null

        // Check whether current device is single model (e.g. AirPods Max)
        var isSingleDevice = false
            private set
        private var mSharedPrefs: SharedPreferences? = null
        private var mediaControl: MediaControl? = null
        private var previousWorn = false

        // Reset currently set device
        fun shouldResetDevice(reset: Boolean) {
            if (reset) mCurrentDevice = null
        }

        // Convert internal content address combined with received path value to URI
        fun getUri(path: String?): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(Constants.AUTHORITY_BTHELPER)
                .appendPath(path)
                .build()
        }

        private val btPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
        )
    }
}
