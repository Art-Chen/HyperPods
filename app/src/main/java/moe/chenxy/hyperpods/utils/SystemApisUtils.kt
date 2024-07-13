package moe.chenxy.hyperpods.utils

import android.app.Notification
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object SystemApisUtils {

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is an untethered headset.
     * @hide
     */
    val BluetoothDevice.DEVICE_TYPE_UNTETHERED_HEADSET: String
        get() = "Untethered Headset"

    /**
     * Maximum length of a metadata entry, this is to avoid exploding Bluetooth
     * disk usage
     * @hide
     */
    val BluetoothDevice.METADATA_MAX_LENGTH: Int
        get() = 2048

    /**
     * Manufacturer name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MANUFACTURER_NAME: Int
        get() = 0

    /**
     * Model name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MODEL_NAME: Int
        get() = 1

    /**
     * Software version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_SOFTWARE_VERSION: Int
        get() = 2

    /**
     * Hardware version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_HARDWARE_VERSION: Int
        get() = 3

    /**
     * Package name of the companion app, if any
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_COMPANION_APP: Int
        get() = 4

    /**
     * URI to the main icon shown on the settings UI
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MAIN_ICON: Int
        get() = 5

    /**
     * Whether this device is an untethered headset with left, right and case
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_IS_UNTETHERED_HEADSET: Int
        get() = 6

    /**
     * URI to icon of the left headset
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON: Int
        get() = 7

    /**
     * URI to icon of the right headset
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON: Int
        get() = 8

    /**
     * URI to icon of the headset charging case
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_CASE_ICON: Int
        get() = 9

    /**
     * Battery level of left headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY: Int
        get() = 10

    /**
     * Battery level of rigth headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY: Int
        get() = 11

    /**
     * Battery level of the headset charging case
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY: Int
        get() = 12

    /**
     * Whether the left headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING: Int
        get() = 13

    /**
     * Whether the right headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING: Int
        get() = 14

    /**
     * Whether the headset charging case is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING: Int
        get() = 15

    /**
     * URI to the enhanced settings UI slice
     * Data type should be {@String} as [Byte] array, null means
     * the UI does not exist.
     * @hide
     */
    val BluetoothDevice.METADATA_ENHANCED_SETTINGS_UI_URI: Int
        get() = 16

    /**
     * @hide
     */
    val BluetoothDevice.COMPANION_TYPE_PRIMARY: String
        get() = "COMPANION_PRIMARY"

    /**
     * @hide
     */
    val BluetoothDevice.COMPANION_TYPE_SECONDARY: String
        get() = "COMPANION_SECONDARY"

    /**
     * @hide
     */
    val BluetoothDevice.COMPANION_TYPE_NONE: String
        get() = "COMPANION_NONE"

    /**
     * Type of the Bluetooth device, must be within the list of
     * BluetoothDevice.DEVICE_TYPE_*
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_DEVICE_TYPE: Int
        get() = 17

    /**
     * Battery level of the Bluetooth device, use when the Bluetooth device
     * does not support HFP battery indicator.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MAIN_BATTERY: Int
        get() = 18

    /**
     * Whether the device is charging.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MAIN_CHARGING: Int
        get() = 19

    /**
     * The battery threshold of the Bluetooth device to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_MAIN_LOW_BATTERY_THRESHOLD: Int
        get() = 20

    /**
     * The battery threshold of the left headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD: Int
        get() = 21

    /**
     * The battery threshold of the right headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD: Int
        get() = 22

    /**
     * The battery threshold of the case to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD: Int
        get() = 23


    /**
     * The metadata of the audio spatial data.
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_SPATIAL_AUDIO: Int
        get() = 24

    /**
     * The metadata of the Fast Pair for any custmized feature.
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_FAST_PAIR_CUSTOMIZED_FIELDS: Int
        get() = 25

    /**
     * The metadata of the Fast Pair for LE Audio capable devices.
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_LE_AUDIO: Int
        get() = 26

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Media Control services.
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_GMCS_CCCD: Int
        get() = 27

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Telephony Bearer service.
     * Data type should be [Byte] array.
     * @hide
     */
    val BluetoothDevice.METADATA_GTBS_CCCD: Int
        get() = 28

    val BATTERY_LEVEL_UNKNOWN: Int
        get() = -1

    fun getUserAllUserHandle() : UserHandle {
        return XposedHelpers.getStaticObjectField(UserHandle::class.java, "ALL") as UserHandle
    }

    fun BluetoothDevice.getMetadata(key: Int) : ByteArray? {
        return XposedHelpers.callMethod(this, "getMetadata", key) as ByteArray?
    }

    fun BluetoothDevice.setMetadata(key: Int, value: ByteArray) : Boolean {
        return XposedHelpers.callMethod(this, "setMetadata", key, value) as Boolean
    }

    fun NotificationManager.notifyAsUser(tag: String, id: Int, notification: Notification, userHandle: UserHandle) {
        XposedHelpers.callMethod(this, "notifyAsUser", tag, id, notification, userHandle)
    }
    fun NotificationManager.cancelAsUser(tag: String, id: Int, userHandle: UserHandle) {
        XposedHelpers.callMethod(this, "cancelAsUser", tag, id, userHandle)
    }

    private fun getPropByShell(propName: String): String {
        return try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            BufferedReader(InputStreamReader(p.inputStream), 1024).use { it.readLine() ?: "" }
        } catch (ignore: IOException) {
            ""
        }
    }

    val isHyperOS: Boolean
        get() {
            return getPropByShell("ro.mi.os.version.code").isNotEmpty()
        }
}