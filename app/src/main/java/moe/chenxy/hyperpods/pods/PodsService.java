/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2019 The MoKee Open Source Project
 *               2021-2023 Matthias Urhahn
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods;

import static moe.chenxy.hyperpods.pods.PodsStatusScanCallback.getScanFilters;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;

import moe.chenxy.hyperpods.Constants;
import moe.chenxy.hyperpods.pods.models.IPods;
import moe.chenxy.hyperpods.pods.models.RegularPods;
import moe.chenxy.hyperpods.pods.models.SinglePods;
import moe.chenxy.hyperpods.utils.MediaControl;

import java.util.Objects;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 */
public class PodsService extends Service {

    /**
     * A vendor-specific AT command
     *
     */
    private static final String VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV";

    /**
     * Battery level indicator associated with
     * {@link #VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV}
     *
     */
    private static final int VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1;

    /*
     * Apple, Inc.
     */
    private static final int APPLE = 0x004C;

    /**
     * Broadcast Action: Indicates the battery level of a remote device has
     * been retrieved for the first time, or changed since the last retrieval
     * <p>Always contains the extra fields {@link BluetoothDevice#EXTRA_DEVICE}
     * and {@link BluetoothDevice#EXTRA_BATTERY_LEVEL}.
     */
    public static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";

    /**
     * Used as an Integer extra field in {@link #ACTION_BATTERY_LEVEL_CHANGED} intent. It contains
     * the most recently retrieved battery level information ranging from 0% to 100% for a remote
     * device, {@link #BATTERY_LEVEL_UNKNOWN} when the valid is unknown or there is an error, {@link
     * #BATTERY_LEVEL_BLUETOOTH_OFF} when the bluetooth is off
     */
    public static final String EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL";

    // Target Android Settings Intelligence package that have battery widget for data update
    private static final String PACKAGE_ASI = "com.google.android.settings.intelligence";

    /**
     * Intent used to broadcast bluetooth data update
     * for the Settings Intelligence package's battery widget
     */
    private static final String ACTION_ASI_UPDATE_BLUETOOTH_DATA =
            "batterywidget.impl.action.update_bluetooth_data";

    private static final String COMPANION_TYPE_NONE = "COMPANION_NONE";

    /** A vendor-specific command for unsolicited result code. */
    public static final String VENDOR_RESULT_CODE_COMMAND_ANDROID = "+ANDROID";

    private BluetoothLeScanner btScanner;
    private PodsStatus status = PodsStatus.DISCONNECTED;

    private BroadcastReceiver btReceiver = null;
    private PodsStatusScanCallback scanCallback = null;

    private static BluetoothDevice mCurrentDevice;

    private static boolean isSinglePods = false;
    private boolean isMetaDataSet = false;
    private boolean isSliceSet = false;
    private boolean isModelDataSet = false;

    private boolean statusChanged = false;

    private static SharedPreferences mSharedPrefs;
    private static MediaControl mediaControl;
    private static boolean previousWorn = false;

    public PodsService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                mCurrentDevice = device;
                startAirPodsScanner();
            }
        } catch (NullPointerException e) {
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCurrentDevice = null;
        stopAirPodsScanner();
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
    private void startAirPodsScanner() {
        try {
            BluetoothManager btManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (btAdapter == null) {
                return;
            }

            if (btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }

            if (!btAdapter.isEnabled()) {
                return;
            }

            btScanner = btAdapter.getBluetoothLeScanner();

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setReportDelay(1) // DON'T USE 0
                            .build();

            scanCallback =
                    new PodsStatusScanCallback() {
                        @Override
                        public void onStatus(PodsStatus newStatus) {
                            setStatusChanged(status, newStatus);
                            status = newStatus;
                            handlePlayPause(status, getApplicationContext());
                            updatePodsStatus(status, mCurrentDevice);
                        }
                    };

            btScanner.startScan(getScanFilters(), scanSettings, scanCallback);
        } catch (Throwable t) {
        }
    }

    private void stopAirPodsScanner() {
        try {
            if (btScanner != null && scanCallback != null) {

                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            status = PodsStatus.DISCONNECTED;
        } catch (Throwable t) {
        }
    }

    // Set boolean value to true if device's status has changed
    private void setStatusChanged(PodsStatus status, PodsStatus newStatus) {
        if (!Objects.equals(status, newStatus)) {
            statusChanged = true;
        }
    }

    // Check whether current device is single model (e.g. AirPods Max)
    public static boolean isSingleDevice() {
        return isSinglePods;
    }

    // Handle Play/Pause media control event based on device wear status
    private void handlePlayPause(PodsStatus status, Context context) {
        mSharedPrefs = getSharedPreferences(Constants.PREFERENCES_BTHELPER, Context.MODE_PRIVATE);

        final boolean onePodMode = mSharedPrefs.getBoolean(Constants.KEY_ONEPOD_MODE, false);
        final boolean autoPlay = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PLAY, false);
        final boolean autoPause = mSharedPrefs.getBoolean(Constants.KEY_AUTO_PAUSE, false);
        final boolean autoPlayPause = autoPlay && autoPause;

        try {
            mediaControl = MediaControl.getInstance(context);
        } catch (Exception e) {
        }

        if (mediaControl == null) return;

        final IPods airpods = status.getAirpods();
        final boolean single = airpods.isSingle();
        boolean currentWorn = false;

        if (!single) {
            final RegularPods regularPods = (RegularPods) airpods;
            if (onePodMode) {
                currentWorn =
                        regularPods.isInEar(RegularPods.LEFT)
                                || regularPods.isInEar(RegularPods.RIGHT);
            } else {
                currentWorn =
                        regularPods.isInEar(RegularPods.LEFT)
                                && regularPods.isInEar(RegularPods.RIGHT);
            }
        } else {
            final SinglePods singlePods = (SinglePods) airpods;
            currentWorn = singlePods.isInEar();
        }

        if (!previousWorn && currentWorn && !mediaControl.isPlaying()) {
            if (autoPlayPause || autoPlay) {
                mediaControl.sendPlay();
            }
        } else if (previousWorn && !currentWorn && mediaControl.isPlaying()) {
            if (autoPlayPause || autoPause) {
                mediaControl.sendPause();
            }
        }

        previousWorn = currentWorn;
    }

    // Convert internal content address combined with recieved path value to URI
    public static Uri getUri(String path) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(Constants.AUTHORITY_BTHELPER)
                .appendPath(path)
                .build();
    }

    // Convert internal resource address to URI
    private Uri resToUri(int resId) {
        try {
            Uri uri =
                    (new Uri.Builder())
                            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                            .authority(Constants.AUTHORITY_BTHELPER)
                            .appendPath(
                                    getApplicationContext()
                                            .getResources()
                                            .getResourceTypeName(resId))
                            .appendPath(
                                    getApplicationContext()
                                            .getResources()
                                            .getResourceEntryName(resId))
                            .build();
            return uri;
        } catch (NotFoundException e) {
            return null;
        }
    }

    private boolean setMetadata(BluetoothDevice device, int key, byte[] value) {
        if (device.getMetadata(key) == null) {
            return device.setMetadata(key, value);
        }
        return true;
    }

    // Set metadata (icon, battery, charging status, etc.) for current device
    // and send broadcast that device status has changed
    private void updatePodsStatus(PodsStatus status, BluetoothDevice device) {
        final IPods airpods = status.getAirpods();
        final boolean single = airpods.isSingle();
        isSinglePods = single;
        int batteryUnified = 0;
        int batteryUnifiedArg = 0;
        boolean chargingMain = false;

        if (!isMetaDataSet) {
            isSliceSet =
                    setMetadata(
                            device,
                            device.METADATA_COMPANION_APP,
                            Constants.AUTHORITY_BTHELPER.getBytes());
            isSliceSet =
                    setMetadata(
                            device,
                            device.METADATA_SOFTWARE_VERSION,
                            COMPANION_TYPE_NONE.getBytes());
            isSliceSet =
                    setMetadata(
                            device,
                            device.METADATA_ENHANCED_SETTINGS_UI_URI,
                            getUri(Constants.PATH_BTHELPER).toString().getBytes());
        }

        if (!single) {
            final RegularPods regularPods = (RegularPods) airpods;
            if (!isMetaDataSet) {
                isModelDataSet =
                        setMetadata(
                                device,
                                device.METADATA_MANUFACTURER_NAME,
                                regularPods.getMenufacturer().getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MODEL_NAME,
                                regularPods.getModel().getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_DEVICE_TYPE,
                                device.DEVICE_TYPE_UNTETHERED_HEADSET.getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_IS_UNTETHERED_HEADSET,
                                Boolean.toString(true).getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                                (regularPods.getLowBattThreshold() + "").getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
                                (regularPods.getLowBattThreshold() + "").getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
                                (regularPods.getLowBattThreshold() + "").getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
                                (regularPods.getLowBattThreshold() + "").getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MAIN_ICON,
                                resToUri(regularPods.getDrawable()).toString().getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_LEFT_ICON,
                                resToUri(regularPods.getLeftDrawable())
                                        .toString()
                                        .getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_RIGHT_ICON,
                                resToUri(regularPods.getRightDrawable())
                                        .toString()
                                        .getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_UNTETHERED_CASE_ICON,
                                resToUri(regularPods.getCaseDrawable())
                                        .toString()
                                        .getBytes());
            }

            if (statusChanged) {
                final boolean leftCharging = regularPods.isCharging(RegularPods.LEFT);
                final boolean rightCharging = regularPods.isCharging(RegularPods.RIGHT);
                final boolean caseCharging = regularPods.isCharging(RegularPods.CASE);
                final int leftBattery = regularPods.getParsedStatus(false, RegularPods.LEFT);
                final int rightBattery = regularPods.getParsedStatus(false, RegularPods.RIGHT);
                final int leftBatteryArg = regularPods.getParsedStatus(true, RegularPods.LEFT);
                final int rightBatteryArg = regularPods.getParsedStatus(true, RegularPods.RIGHT);
                final int caseBattery = regularPods.getParsedStatus(false, RegularPods.CASE);

                device.setMetadata(
                        device.METADATA_UNTETHERED_LEFT_CHARGING,
                        (leftCharging + "").toUpperCase().getBytes());
                device.setMetadata(
                        device.METADATA_UNTETHERED_RIGHT_CHARGING,
                        (rightCharging + "").toUpperCase().getBytes());
                device.setMetadata(
                        device.METADATA_UNTETHERED_CASE_CHARGING,
                        (caseCharging + "").toUpperCase().getBytes());
                device.setMetadata(
                        device.METADATA_UNTETHERED_LEFT_BATTERY, (leftBattery + "").getBytes());
                device.setMetadata(
                        device.METADATA_UNTETHERED_RIGHT_BATTERY, (rightBattery + "").getBytes());
                device.setMetadata(
                        device.METADATA_UNTETHERED_CASE_BATTERY, (caseBattery + "").getBytes());

                chargingMain = leftCharging && rightCharging;
                batteryUnified = Math.min(leftBattery, rightBattery);
                batteryUnifiedArg = Math.min(leftBatteryArg, rightBatteryArg);
            }
        } else {
            final SinglePods singlePods = (SinglePods) airpods;
            if (!isMetaDataSet) {
                isModelDataSet =
                        setMetadata(
                                device,
                                device.METADATA_MANUFACTURER_NAME,
                                singlePods.getMenufacturer().getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_DEVICE_TYPE,
                                device.DEVICE_TYPE_UNTETHERED_HEADSET.getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_IS_UNTETHERED_HEADSET,
                                Boolean.toString(true).getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MODEL_NAME,
                                singlePods.getModel().getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MAIN_LOW_BATTERY_THRESHOLD,
                                (singlePods.getLowBattThreshold() + "").getBytes())
                        && setMetadata(
                                device,
                                device.METADATA_MAIN_ICON,
                                resToUri(singlePods.getDrawable()).toString().getBytes());
            }
            chargingMain = singlePods.isCharging();
            batteryUnified = singlePods.getParsedStatus(true);
            batteryUnifiedArg = singlePods.getParsedStatus(false);
        }

        if (!isMetaDataSet) {
            isMetaDataSet = isSliceSet && isModelDataSet;
        }

        if (statusChanged) {
            device.setMetadata(
                    device.METADATA_MAIN_CHARGING, (chargingMain + "").toUpperCase().getBytes());
            device.setMetadata(
                    device.METADATA_MAIN_BATTERY, (batteryUnified + "").getBytes());

            broadcastVendorSpecificEventIntent(
                    VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                    APPLE,
                    BluetoothHeadset.AT_CMD_TYPE_SET,
                    batteryUnified,
                    batteryUnifiedArg,
                    device);

            statusChanged = false;
        }
    }

    // Send broadcasts to Android Settings Intelligence, Bluetooth app, System Settings
    // to reflect current device status changes
    private void broadcastVendorSpecificEventIntent(
            String command,
            int companyId,
            int commandType,
            int batteryUnified,
            int batteryUnifiedArg,
            BluetoothDevice device) {

        final Object[] arguments =
                new Object[] {
                    1, // Number of key(IndicatorType)/value pairs
                    VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL, // IndicatorType:
                                                                                    // Battery Level
                    batteryUnifiedArg, // Battery Level
                };

        // Update battery status for this device
        final Intent intent = new Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, commandType);
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, device.getName());
        intent.addCategory(
                BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY
                        + "."
                        + Integer.toString(companyId));
        sendBroadcastAsUser(intent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT);

        // Broadcast battery level changes
        final Intent batteryIntent =
                new Intent(ACTION_BATTERY_LEVEL_CHANGED);
        batteryIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        batteryIntent.putExtra(EXTRA_BATTERY_LEVEL, batteryUnified);
        sendBroadcastAsUser(batteryIntent, UserHandle.ALL, Manifest.permission.BLUETOOTH_CONNECT);

        // Update Android Settings Intelligence's battery widget
        final Intent statusIntent =
                new Intent(ACTION_ASI_UPDATE_BLUETOOTH_DATA).setPackage(PACKAGE_ASI);
        statusIntent.putExtra(ACTION_BATTERY_LEVEL_CHANGED, intent);
        sendBroadcastAsUser(statusIntent, UserHandle.ALL);
    }
}
