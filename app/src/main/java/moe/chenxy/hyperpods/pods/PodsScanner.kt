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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import com.highcapable.yukihookapi.hook.xposed.bridge.resources.YukiModuleResources
import moe.chenxy.hyperpods.Constants
import moe.chenxy.hyperpods.pods.models.RegularPods
import moe.chenxy.hyperpods.pods.models.SinglePods
import moe.chenxy.hyperpods.utils.MediaControl
import moe.chenxy.hyperpods.utils.SystemApisUtils.DEVICE_TYPE_UNTETHERED_HEADSET
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_DEVICE_TYPE
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_IS_UNTETHERED_HEADSET
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MAIN_BATTERY
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MAIN_CHARGING
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MAIN_ICON
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MAIN_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MANUFACTURER_NAME
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_MODEL_NAME
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_BATTERY
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_CHARGING
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_ICON
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_BATTERY
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_CHARGING
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_ICON
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_BATTERY
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_CHARGING
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_ICON
import moe.chenxy.hyperpods.utils.SystemApisUtils.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD
import java.util.Locale
import kotlin.math.min
import moe.chenxy.hyperpods.utils.SystemApisUtils.getMetadata
import moe.chenxy.hyperpods.utils.SystemApisUtils.getUserAllUserHandle
import moe.chenxy.hyperpods.utils.SystemApisUtils.setMetadata
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showPodsNotificationByMiuiBt

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 */
@SuppressLint("MissingPermission")
class PodsScanner(private val context: Context, private val moduleResources: YukiModuleResources) {
    private lateinit var btScanner: BluetoothLeScanner
    private var status: PodsStatus = PodsStatus.DISCONNECTED

    private var scanCallback: PodsStatusScanCallback? = null

    private var isMetaDataSet = false
    private var isSliceSet = false
    private var isModelDataSet = false

    private var statusChanged = false
    private var isAlreadyShowConnectedToast = false
    private var isAlreadyShowLeftLowBatt = false
    private var isAlreadyShowRightLowBatt = false
    private var isAlreadyShowCaseLowBatt = false
    private var lastCaseBatt = -1

    fun startScan(device: BluetoothDevice) {
        mCurrentDevice = device
        startAirPodsScanner()
    }

    fun stopScan() {
        mCurrentDevice?.let { cancelPodsNotificationByMiuiBt(context, it) }
        mCurrentDevice = null
        stopAirPodsScanner()
        isAlreadyShowConnectedToast = false
        isAlreadyShowLeftLowBatt = false
        isAlreadyShowRightLowBatt = false
        isAlreadyShowCaseLowBatt = false
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
            val btManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter ?: return

            if (this::btScanner.isInitialized && scanCallback != null) {
                btScanner.stopScan(scanCallback)
                scanCallback = null
            }

            if (!btAdapter.isEnabled) {
                return
            }

            btScanner = btAdapter.bluetoothLeScanner

            val scanSettings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1) // DON'T USE 0
                    .build()

            scanCallback =
                object : PodsStatusScanCallback() {
                    override fun onStatus(newStatus: PodsStatus) {
                        setStatusChanged(status, newStatus)
                        status = newStatus
                        mCurrentDevice?.let { updatePodsStatus(status, it) }
                    }
                }

            btScanner.startScan(PodsStatusScanCallback.scanFilters, scanSettings, scanCallback)
        } catch (_: Throwable) {
        }
    }

    private fun stopAirPodsScanner() {
        try {
            if (this::btScanner.isInitialized && scanCallback != null) {
                btScanner.stopScan(scanCallback)
                scanCallback = null
            }
            status = PodsStatus.DISCONNECTED
        } catch (_: Throwable) {
        }
    }

    // Set boolean value to true if device's status has changed
    private fun setStatusChanged(status: PodsStatus, newStatus: PodsStatus) {
        if (status != newStatus) {
            statusChanged = true
        }
    }
//
//    // Handle Play/Pause media control event based on device wear status
//    private fun handlePlayPause(status: PodsStatus, context: Context) {
//        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, MODE_PRIVATE)
//
//        val onePodMode = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false)
//        val autoPlay = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false)
//        val autoPause = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false)
//        val autoPlayPause = autoPlay && autoPause
//
//        try {
//            mediaControl = getInstance(context)
//        } catch (e: Exception) {
//        }
//
//        if (mediaControl == null) return
//
//        val airpods = status.airpods
//        val single = airpods.isSingle
//        var currentWorn = false
//
//        if (!single) {
//            val regularPods = airpods as RegularPods
//            currentWorn = if (onePodMode) {
//                    regularPods.isInEar(RegularPods.LEFT) || regularPods.isInEar(RegularPods.RIGHT)
//                } else {
//                    regularPods.isInEar(RegularPods.LEFT) && regularPods.isInEar(RegularPods.RIGHT))
//                }
//        } else {
//            val singlePods = airpods as SinglePods
//            currentWorn = singlePods.isInEar
//        }
//
//        if (!previousWorn && currentWorn && !isPlaying) {
//            if (autoPlayPause || autoPlay) {
//                sendPlay()
//            }
//        } else if (previousWorn && !currentWorn && isPlaying) {
//            if (autoPlayPause || autoPause) {
//                sendPause()
//            }
//        }
//
//        previousWorn = currentWorn
//    }

    // Convert internal resource address to URI
    private fun resToUri(resId: Int): Uri? {
        try {
            val uri =
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(Constants.PKG_NAME_HYPERPODS)
                    .appendPath(
                        moduleResources.getResourceTypeName(resId)
                    )
                    .appendPath(
                        moduleResources
                            .getResourceEntryName(resId)
                    )
                    .build()
            return uri
        } catch (e: Resources.NotFoundException) {
            return null
        }
    }

    private fun setMetadata(device: BluetoothDevice?, key: Int, value: ByteArray): Boolean {
        if (device?.getMetadata(key) == null) {
            return device!!.setMetadata(key, value)
        }
        return true
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private fun updatePodsStatus(status: PodsStatus, device: BluetoothDevice) {
        val airpods = status.airpods ?: return
        val single = airpods.isSingle
        isSingleDevice = single
        var batteryUnified = 0
        var batteryUnifiedArg = 0
        var chargingMain = false

        if (!single) {
            val regularPods = airpods as RegularPods
            if (!isMetaDataSet) {
                isModelDataSet =
                    (setMetadata(
                        device,
                        device.METADATA_MANUFACTURER_NAME,
                        regularPods.menufacturer.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_MODEL_NAME,
                        regularPods.model.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_DEVICE_TYPE,
                        device.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_IS_UNTETHERED_HEADSET,
                        true.toString().toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                        (regularPods.lowBattThreshold.toString() + "").toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                        (regularPods.lowBattThreshold.toString() + "").toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                        (regularPods.lowBattThreshold.toString() + "").toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                        (regularPods.lowBattThreshold.toString() + "").toByteArray()
                    )
                    // TODO: Support showing pods icons
//                            && setMetadata(
//                        device,
//                        device.METADATA_MAIN_ICON,
//                        resToUri(regularPods.drawable).toString().toByteArray()
//                    )
//                            && setMetadata(
//                        device,
//                        device.METADATA_UNTETHERED_LEFT_ICON,
//                        resToUri(regularPods.leftDrawable)
//                            .toString()
//                            .toByteArray()
//                    )
//                            && setMetadata(
//                        device,
//                        device.METADATA_UNTETHERED_RIGHT_ICON,
//                        resToUri(regularPods.rightDrawable)
//                            .toString()
//                            .toByteArray()
//                    )
//                            && setMetadata(
//                        device,
//                        device.METADATA_UNTETHERED_CASE_ICON,
//                        resToUri(regularPods.caseDrawable)
//                            .toString()
//                            .toByteArray()
//                    )
                )
            }

            if (statusChanged) {
                val leftCharging = regularPods.isCharging(RegularPods.LEFT)
                val rightCharging = regularPods.isCharging(RegularPods.RIGHT)
                val caseCharging = regularPods.isCharging(RegularPods.CASE)
                val leftBattery = regularPods.getParsedStatus(false, RegularPods.LEFT)
                val rightBattery = regularPods.getParsedStatus(false, RegularPods.RIGHT)
                val leftBatteryArg = regularPods.getParsedStatus(true, RegularPods.LEFT)
                val rightBatteryArg = regularPods.getParsedStatus(true, RegularPods.RIGHT)
                val caseBattery = regularPods.getParsedStatus(false, RegularPods.CASE)

                device.setMetadata(
                    device.METADATA_UNTETHERED_LEFT_CHARGING,
                    (leftCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray()
                )
                device.setMetadata(
                    device.METADATA_UNTETHERED_RIGHT_CHARGING,
                    (rightCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray()
                )
                device.setMetadata(
                    device.METADATA_UNTETHERED_CASE_CHARGING,
                    (caseCharging.toString() + "").uppercase(Locale.getDefault()).toByteArray()
                )
                device.setMetadata(
                    device.METADATA_UNTETHERED_LEFT_BATTERY,
                    (leftBattery.toString() + "").toByteArray()
                )
                device.setMetadata(device.METADATA_UNTETHERED_RIGHT_BATTERY, (rightBattery.toString() + "").toByteArray())
                device.setMetadata(
                    device.METADATA_UNTETHERED_CASE_BATTERY,
                    (caseBattery.toString() + "").toByteArray()
                )

                Log.v("Art_Chen", "statusChanged battery left $leftBattery right $rightBattery case $caseBattery")
                if (!isAlreadyShowConnectedToast || (lastCaseBatt == -1 && caseBattery != -1)) {
                    MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(
                        context,
                        leftBattery,
                        leftCharging,
                        rightBattery,
                        rightCharging,
                        caseBattery,
                        caseCharging,
                        regularPods.lowBattThreshold
                    )

                    // Prevent double low batt toast
                    if (leftBattery <= regularPods.lowBattThreshold) isAlreadyShowLeftLowBatt = true
                    if (rightBattery <= regularPods.lowBattThreshold) isAlreadyShowRightLowBatt = true
                    if (caseBattery <= regularPods.lowBattThreshold) isAlreadyShowCaseLowBatt = true
                    isAlreadyShowConnectedToast = true
                } else {
                    // Low Batt
                    if (!isAlreadyShowLeftLowBatt && !leftCharging && leftBattery <= regularPods.lowBattThreshold) {
                        MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(
                            context,
                            leftBattery,
                            leftCharging,
                            rightBattery,
                            rightCharging,
                            -1,
                            false,
                            regularPods.lowBattThreshold
                        )
                        isAlreadyShowLeftLowBatt = true
                    } else if (!isAlreadyShowRightLowBatt && !rightCharging && rightBattery <= regularPods.lowBattThreshold) {
                        MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(
                            context,
                            leftBattery,
                            leftCharging,
                            rightBattery,
                            rightCharging,
                            -1,
                            false,
                            regularPods.lowBattThreshold
                        )
                        isAlreadyShowRightLowBatt = true
                    } else if (!isAlreadyShowCaseLowBatt && !caseCharging && caseBattery <= regularPods.lowBattThreshold) {
                        MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(
                            context,
                            -1,
                            false,
                            -1,
                            false,
                            caseBattery,
                            caseCharging,
                            regularPods.lowBattThreshold
                        )
                        isAlreadyShowCaseLowBatt = true
                    }
                }

                showPodsNotificationByMiuiBt(context, leftBattery, rightBattery, caseBattery, device)

                chargingMain = leftCharging && rightCharging
                batteryUnified = min(leftBattery.toDouble(), rightBattery.toDouble()).toInt()
                batteryUnifiedArg = min(leftBatteryArg.toDouble(), rightBatteryArg.toDouble())
                    .toInt()
                lastCaseBatt = caseBattery
            }
        } else {
            val singlePods = airpods as SinglePods
            if (!isMetaDataSet) {
                isModelDataSet =
                    (setMetadata(
                        device,
                        device.METADATA_MANUFACTURER_NAME,
                        singlePods.menufacturer.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_DEVICE_TYPE,
                        device.DEVICE_TYPE_UNTETHERED_HEADSET.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_IS_UNTETHERED_HEADSET,
                        true.toString().toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_MODEL_NAME,
                        singlePods.model.toByteArray()
                    )
                            && setMetadata(
                        device,
                        device.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                        (singlePods.lowBattThreshold.toString() + "").toByteArray()
                    )
//                            && setMetadata(
//                        device,
//                        device.METADATA_MAIN_ICON,
//                        resToUri(singlePods.drawable).toString().toByteArray()
//                    )
                    )
            }
            chargingMain = singlePods.isCharging
            batteryUnified = singlePods.getParsedStatus(true)
            batteryUnifiedArg = singlePods.getParsedStatus(false)
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isModelDataSet
        }

        if (statusChanged) {
            device.setMetadata(
                device.METADATA_MAIN_CHARGING,
                (chargingMain.toString() + "").uppercase(Locale.getDefault()).toByteArray()
            )
            device.setMetadata(
                device.METADATA_MAIN_BATTERY, (batteryUnified.toString() + "").toByteArray()
            )

            broadcastVendorSpecificEventIntent(
                VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                APPLE,
                BluetoothHeadset.AT_CMD_TYPE_SET,
                batteryUnified,
                batteryUnifiedArg,
                device
            )

            statusChanged = false
        }
    }

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    private fun broadcastVendorSpecificEventIntent(
        command: String,
        companyId: Int,
        commandType: Int,
        batteryUnified: Int,
        batteryUnifiedArg: Int,
        device: BluetoothDevice?
    ) {
        val arguments =
            arrayOf<Any>(
                1,  // Number of key(IndicatorType)/value pairs
                VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL,  // IndicatorType:
                // Battery Level
                batteryUnifiedArg,  // Battery Level
            )

        // Update battery status for this device
        val intent = Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command)
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, commandType)
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        intent.putExtra(BluetoothDevice.EXTRA_NAME, device!!.name)
        intent.addCategory(
            BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
                    + "."
                    + companyId.toString()
        )
        context.sendBroadcastAsUser(intent, getUserAllUserHandle(), Manifest.permission.BLUETOOTH_CONNECT)

        // Broadcast battery level changes
        val batteryIntent =
            Intent(ACTION_BATTERY_LEVEL_CHANGED)
        batteryIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        batteryIntent.putExtra(EXTRA_BATTERY_LEVEL, batteryUnified)
        context.sendBroadcastAsUser(batteryIntent, getUserAllUserHandle(), Manifest.permission.BLUETOOTH_CONNECT)

        // Update Android Settings Intelligence's battery widget
        val statusIntent =
            Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).setPackage(PACKAGE_ASI)
        statusIntent.putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent)
        context.sendBroadcastAsUser(statusIntent, getUserAllUserHandle())
    }

    companion object {
        /**
         * A vendor-specific AT command
         *
         */
        private const val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV"

        /**
         * Battery level indicator associated with
         * [.VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV]
         *
         */
        private const val VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1

        /*
         * Apple, Inc.
         */
        private const val APPLE = 0x004C

        /**
         * Broadcast Action: Indicates the battery level of a remote device has
         * been retrieved for the first time, or changed since the last retrieval
         *
         * Always contains the extra fields [BluetoothDevice.EXTRA_DEVICE]
         * and [BluetoothDevice.EXTRA_BATTERY_LEVEL].
         */
        const val ACTION_BATTERY_LEVEL_CHANGED: String =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

        /**
         * Used as an Integer extra field in [.ACTION_BATTERY_LEVEL_CHANGED] intent. It contains
         * the most recently retrieved battery level information ranging from 0% to 100% for a remote
         * device, [.BATTERY_LEVEL_UNKNOWN] when the valid is unknown or there is an error, [ ][.BATTERY_LEVEL_BLUETOOTH_OFF] when the bluetooth is off
         */
        const val EXTRA_BATTERY_LEVEL: String = "android.bluetooth.device.extra.BATTERY_LEVEL"

        // Target Android Settings Intelligence package that have battery widget for data update
        private const val PACKAGE_ASI = "com.google.android.settings.intelligence"

        /**
         * Intent used to broadcast bluetooth data update
         * for the Settings Intelligence package's battery widget
         */
        private const val ACTION_ASI_UPDATE_BLUETOOTH_DATA =
            "batterywidget.impl.action.update_bluetooth_data"


        /** A vendor-specific command for unsolicited result code.  */
        const val VENDOR_RESULT_CODE_COMMAND_ANDROID: String = "+ANDROID"

        private var mCurrentDevice: BluetoothDevice? = null

        // Check whether current device is single model (e.g. AirPods Max)
        var isSingleDevice: Boolean = false
            private set
        private var mediaControl: MediaControl? = null

        // Convert internal content address combined with recieved path value to URI
        fun getUri(path: String?): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(Constants.PKG_NAME_HYPERPODS)
                .appendPath(path)
                .build()
        }
    }
}
