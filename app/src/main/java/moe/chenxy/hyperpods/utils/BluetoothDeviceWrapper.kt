package moe.chenxy.hyperpods.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresPermission
import de.robv.android.xposed.XposedHelpers
import java.lang.annotation.RetentionPolicy
import java.util.UUID


object BluetoothDeviceWrapper {

    fun BluetoothDevice?.setLowLatencyAudioAllowed(boolean: Any) {
        XposedHelpers.callMethod(this, "setLowLatencyAudioAllowed", boolean)
    }

    fun BluetoothDevice?.getMetadata(@MetadataKey key: Int): ByteArray? {
        return XposedHelpers.callMethod(this, "getMetadata", key) as ByteArray?
    }

    fun BluetoothDevice?.setMetadata(@MetadataKey key: Int, value: ByteArray): Boolean {
        return XposedHelpers.callMethod(this, "setMetadata", key, value) as Boolean
    }


// Copied from framework

    /**
     * Maximum length of a metadata entry, this is to avoid exploding Bluetooth
     * disk usage
     * @hide
     */

    val METADATA_MAX_LENGTH = 2048

    /**
     * Manufacturer name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_MANUFACTURER_NAME = 0

    /**
     * Model name of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_MODEL_NAME = 1

    /**
     * Software version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_SOFTWARE_VERSION = 2

    /**
     * Hardware version of this Bluetooth device
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_HARDWARE_VERSION = 3

    /**
     * Package name of the companion app, if any
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_COMPANION_APP = 4

    /**
     * URI to the main icon shown on the settings UI
     * Data type should be [Byte] array.
     * @hide
     */

    const val METADATA_MAIN_ICON = 5

    /**
     * Whether this device is an untethered headset with left, right and case
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_IS_UNTETHERED_HEADSET = 6

    /**
     * URI to icon of the left headset
     * Data type should be [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_LEFT_ICON = 7

    /**
     * URI to icon of the right headset
     * Data type should be [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_RIGHT_ICON = 8

    /**
     * URI to icon of the headset charging case
     * Data type should be [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_CASE_ICON = 9

    /**
     * Battery level of left headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */

    const val METADATA_UNTETHERED_LEFT_BATTERY = 10

    /**
     * Battery level of rigth headset
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */

    const val METADATA_UNTETHERED_RIGHT_BATTERY = 11

    /**
     * Battery level of the headset charging case
     * Data type should be {@String} 0-100 as [Byte] array, otherwise
     * as invalid.
     * @hide
     */

    const val METADATA_UNTETHERED_CASE_BATTERY = 12

    /**
     * Whether the left headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_LEFT_CHARGING = 13

    /**
     * Whether the right headset is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_RIGHT_CHARGING = 14

    /**
     * Whether the headset charging case is charging
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_CASE_CHARGING = 15

    /**
     * URI to the enhanced settings UI slice
     * Data type should be {@String} as [Byte] array, null means
     * the UI does not exist.
     * @hide
     */

    const val METADATA_ENHANCED_SETTINGS_UI_URI = 16

    /**
     * @hide
     */
    const val COMPANION_TYPE_PRIMARY = "COMPANION_PRIMARY"

    /**
     * @hide
     */
    const val COMPANION_TYPE_SECONDARY = "COMPANION_SECONDARY"

    /**
     * @hide
     */
    const val COMPANION_TYPE_NONE = "COMPANION_NONE"

    /**
     * Type of the Bluetooth device, must be within the list of
     * BluetoothDevice.DEVICE_TYPE_*
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_DEVICE_TYPE = 17

    /**
     * Battery level of the Bluetooth device, use when the Bluetooth device
     * does not support HFP battery indicator.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_MAIN_BATTERY = 18

    /**
     * Whether the device is charging.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_MAIN_CHARGING = 19

    /**
     * The battery threshold of the Bluetooth device to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_MAIN_LOW_BATTERY_THRESHOLD = 20

    /**
     * The battery threshold of the left headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD = 21

    /**
     * The battery threshold of the right headset to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD = 22

    /**
     * The battery threshold of the case to show low battery icon.
     * Data type should be {@String} as [Byte] array.
     * @hide
     */

    const val METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD = 23


    /**
     * The metadata of the audio spatial data.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_SPATIAL_AUDIO = 24

    /**
     * The metadata of the Fast Pair for any custmized feature.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_FAST_PAIR_CUSTOMIZED_FIELDS = 25

    /**
     * The metadata of the Fast Pair for LE Audio capable devices.
     * Data type should be [Byte] array.
     * @hide
     */

    const val METADATA_LE_AUDIO = 26

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Media Control services.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_GMCS_CCCD = 27

    /**
     * The UUIDs (16-bit) of registered to CCC characteristics from Telephony Bearer service.
     * Data type should be [Byte] array.
     * @hide
     */
    const val METADATA_GTBS_CCCD = 28

    private const val METADATA_MAX_KEY = METADATA_GTBS_CCCD

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a standard Bluetooth accessory or
     * not listed in METADATA_DEVICE_TYPE_*.
     * @hide
     */

    val DEVICE_TYPE_DEFAULT = "Default"

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a watch.
     * @hide
     */

    val DEVICE_TYPE_WATCH = "Watch"

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is an untethered headset.
     * @hide
     */

    val DEVICE_TYPE_UNTETHERED_HEADSET = "Untethered Headset"

    /**
     * Device type which is used in METADATA_DEVICE_TYPE
     * Indicates this Bluetooth device is a stylus.
     * @hide
     */

    val DEVICE_TYPE_STYLUS = "Stylus"

    /**
     * Broadcast Action: This intent is used to broadcast the [UUID]
     * wrapped as a [android.os.ParcelUuid] of the remote device after it
     * has been fetched. This intent is sent only when the UUIDs of the remote
     * device are requested to be fetched using Service Discovery Protocol
     *
     *  Always contains the extra field [.EXTRA_DEVICE]
     *
     *  Always contains the extra field [.EXTRA_UUID]
     */
    @RequiresPermission(
        Manifest.permission.BLUETOOTH_CONNECT
    )
    val ACTION_UUID = "android.bluetooth.device.action.UUID"

    /** @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    val ACTION_MAS_INSTANCE = "android.bluetooth.device.action.MAS_INSTANCE"

    /**
     * Broadcast Action: Indicates a failure to retrieve the name of a remote
     * device.
     *
     * Always contains the extra field [.EXTRA_DEVICE].
     *
     * @hide
     */
//TODO: is this actually useful?
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    val ACTION_NAME_FAILED = "android.bluetooth.device.action.NAME_FAILED"

    /**
     * Broadcast Action: This intent is used to broadcast PAIRING REQUEST
     */
    @RequiresPermission(
        Manifest.permission.BLUETOOTH_CONNECT
    )
    val ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST"

    /**
     * Starting from [android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
     * the return value of [BluetoothDevice.toString] has changed
     * to improve privacy.
     */
    private val CHANGE_TO_STRING_REDACTED = 265103382L

    /**
     * Broadcast Action: This intent is used to broadcast PAIRING CANCEL
     *
     * @hide
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ActionValue")
    val ACTION_PAIRING_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL"

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS REQUEST
     *
     * This action will trigger a prompt for the user to accept or deny giving the
     * permission for this device. Permissions can be specified with
     * [.EXTRA_ACCESS_REQUEST_TYPE].
     *
     * The reply will be an [.ACTION_CONNECTION_ACCESS_REPLY] sent to the specified
     * [.EXTRA_PACKAGE_NAME] and [.EXTRA_CLASS_NAME].
     *
     * This action can be cancelled with [.ACTION_CONNECTION_ACCESS_CANCEL].
     *
     * @hide
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ActionValue")
    val ACTION_CONNECTION_ACCESS_REQUEST =
        "android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST"

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS REPLY
     *
     * This action is the reply from [.ACTION_CONNECTION_ACCESS_REQUEST]
     * that is sent to the specified [.EXTRA_PACKAGE_NAME]
     * and [.EXTRA_CLASS_NAME].
     *
     * See the extra fields [.EXTRA_CONNECTION_ACCESS_RESULT] and
     * [.EXTRA_ALWAYS_ALLOWED] for possible results.
     *
     * @hide
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ActionValue")
    val ACTION_CONNECTION_ACCESS_REPLY = "android.bluetooth.device.action.CONNECTION_ACCESS_REPLY"

    /**
     * Broadcast Action: This intent is used to broadcast CONNECTION ACCESS CANCEL
     *
     * @hide
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("ActionValue")
    val ACTION_CONNECTION_ACCESS_CANCEL = "android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL"

    /**
     * Intent to broadcast silence mode changed.
     * Alway contains the extra field [.EXTRA_DEVICE]
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)

    val ACTION_SILENCE_MODE_CHANGED = "android.bluetooth.device.action.SILENCE_MODE_CHANGED"

    /**
     * Used as an extra field in [.ACTION_CONNECTION_ACCESS_REQUEST].
     *
     * Possible values are [.REQUEST_TYPE_PROFILE_CONNECTION],
     * [.REQUEST_TYPE_PHONEBOOK_ACCESS], [.REQUEST_TYPE_MESSAGE_ACCESS]
     * and [.REQUEST_TYPE_SIM_ACCESS]
     *
     * @hide
     */

    @SuppressLint("ActionValue")
    val EXTRA_ACCESS_REQUEST_TYPE = "android.bluetooth.device.extra.ACCESS_REQUEST_TYPE"

    /** @hide
     */

    val REQUEST_TYPE_PROFILE_CONNECTION = 1

    /** @hide
     */

    val REQUEST_TYPE_PHONEBOOK_ACCESS = 2

    /** @hide
     */

    val REQUEST_TYPE_MESSAGE_ACCESS = 3

    /** @hide
     */

    val REQUEST_TYPE_SIM_ACCESS = 4

    /**
     * Used as an extra field in [.ACTION_CONNECTION_ACCESS_REQUEST] intents,
     * Contains package name to return reply intent to.
     *
     * @hide
     */
    const val EXTRA_PACKAGE_NAME = "android.bluetooth.device.extra.PACKAGE_NAME"

    /**
     * Used as an extra field in [.ACTION_CONNECTION_ACCESS_REQUEST] intents,
     * Contains class name to return reply intent to.
     *
     * @hide
     */
    const val EXTRA_CLASS_NAME = "android.bluetooth.device.extra.CLASS_NAME"

    /**
     * Used as an extra field in [.ACTION_CONNECTION_ACCESS_REPLY] intent.
     *
     * Possible values are [.CONNECTION_ACCESS_YES] and [.CONNECTION_ACCESS_NO].
     *
     * @hide
     */

    @SuppressLint("ActionValue")
    val EXTRA_CONNECTION_ACCESS_RESULT = "android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT"

    /** @hide
     */

    val CONNECTION_ACCESS_YES = 1

    /** @hide
     */

    val CONNECTION_ACCESS_NO = 2

    /**
     * Used as an extra field in [.ACTION_CONNECTION_ACCESS_REPLY] intents,
     * Contains boolean to indicate if the allowed response is once-for-all so that
     * next request will be granted without asking user again.
     *
     * @hide
     */

    @SuppressLint("ActionValue")
    val EXTRA_ALWAYS_ALLOWED = "android.bluetooth.device.extra.ALWAYS_ALLOWED"

    /**
     * A bond attempt succeeded
     *
     * @hide
     */
    const val BOND_SUCCESS = 0

    /**
     * A bond attempt failed because pins did not match, or remote device did
     * not respond to pin request in time
     *
     * @hide
     */

    val UNBOND_REASON_AUTH_FAILED = 1

    /**
     * A bond attempt failed because the other side explicitly rejected
     * bonding
     *
     * @hide
     */

    val UNBOND_REASON_AUTH_REJECTED = 2

    /**
     * A bond attempt failed because we canceled the bonding process
     *
     * @hide
     */

    val UNBOND_REASON_AUTH_CANCELED = 3

    /**
     * A bond attempt failed because we could not contact the remote device
     *
     * @hide
     */

    val UNBOND_REASON_REMOTE_DEVICE_DOWN = 4

    /**
     * A bond attempt failed because a discovery is in progress
     *
     * @hide
     */

    val UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5

    /**
     * A bond attempt failed because of authentication timeout
     *
     * @hide
     */

    val UNBOND_REASON_AUTH_TIMEOUT = 6

    /**
     * A bond attempt failed because of repeated attempts
     *
     * @hide
     */

    val UNBOND_REASON_REPEATED_ATTEMPTS = 7

    /**
     * A bond attempt failed because we received an Authentication Cancel
     * by remote end
     *
     * @hide
     */

    val UNBOND_REASON_REMOTE_AUTH_CANCELED = 8

    /**
     * An existing bond was explicitly revoked
     *
     * @hide
     */

    val UNBOND_REASON_REMOVED = 9

    /**
     * The user will be prompted to enter a pin or
     * an app will enter a pin for user.
     */
    const val PAIRING_VARIANT_PIN = 0

    /**
     * The user will be prompted to enter a passkey
     *
     * @hide
     */

    val PAIRING_VARIANT_PASSKEY = 1

    /**
     * The user will be prompted to confirm the passkey displayed on the screen or
     * an app will confirm the passkey for the user.
     */
    const val PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2

    /**
     * The user will be prompted to accept or deny the incoming pairing request
     *
     * @hide
     */

    val PAIRING_VARIANT_CONSENT = 3

    /**
     * The user will be prompted to enter the passkey displayed on remote device
     * This is used for Bluetooth 2.1 pairing.
     *
     * @hide
     */

    val PAIRING_VARIANT_DISPLAY_PASSKEY = 4

    /**
     * The user will be prompted to enter the PIN displayed on remote device.
     * This is used for Bluetooth 2.0 pairing.
     *
     * @hide
     */

    val PAIRING_VARIANT_DISPLAY_PIN = 5

    /**
     * The user will be prompted to accept or deny the OOB pairing request.
     * This is used for Bluetooth 2.1 secure simple pairing.
     *
     * @hide
     */

    val PAIRING_VARIANT_OOB_CONSENT = 6

    /**
     * The user will be prompted to enter a 16 digit pin or
     * an app will enter a 16 digit pin for user.
     *
     * @hide
     */

    val PAIRING_VARIANT_PIN_16_DIGITS = 7

    /**
     * Used as an extra field in [.ACTION_UUID] intents,
     * Contains the [android.os.ParcelUuid]s of the remote device which
     * is a parcelable version of [UUID].
     * A `null` EXTRA_UUID indicates a timeout.
     */
    const val EXTRA_UUID = "android.bluetooth.device.extra.UUID"

    /** @hide
     */
    const val EXTRA_SDP_RECORD = "android.bluetooth.device.extra.SDP_RECORD"

    /** @hide
     */
    val EXTRA_SDP_SEARCH_STATUS = "android.bluetooth.device.extra.SDP_SEARCH_STATUS"

    /**
     * For [.getPhonebookAccessPermission], [.setPhonebookAccessPermission],
     * [.getMessageAccessPermission] and [.setMessageAccessPermission].
     *
     * @hide
     */

    val ACCESS_UNKNOWN = 0

    /**
     * For [.getPhonebookAccessPermission], [.setPhonebookAccessPermission],
     * [.getMessageAccessPermission] and [.setMessageAccessPermission].
     *
     * @hide
     */

    val ACCESS_ALLOWED = 1

    /**
     * For [.getPhonebookAccessPermission], [.setPhonebookAccessPermission],
     * [.getMessageAccessPermission] and [.setMessageAccessPermission].
     *
     * @hide
     */

    val ACCESS_REJECTED = 2

    @IntDef(
        METADATA_MANUFACTURER_NAME,
        METADATA_MODEL_NAME,
        METADATA_SOFTWARE_VERSION,
        METADATA_HARDWARE_VERSION,
        METADATA_COMPANION_APP,
        METADATA_MAIN_ICON,
        METADATA_IS_UNTETHERED_HEADSET,
        METADATA_UNTETHERED_LEFT_ICON,
        METADATA_UNTETHERED_RIGHT_ICON,
        METADATA_UNTETHERED_CASE_ICON,
        METADATA_UNTETHERED_LEFT_BATTERY,
        METADATA_UNTETHERED_RIGHT_BATTERY,
        METADATA_UNTETHERED_CASE_BATTERY,
        METADATA_UNTETHERED_LEFT_CHARGING,
        METADATA_UNTETHERED_RIGHT_CHARGING,
        METADATA_UNTETHERED_CASE_CHARGING,
        METADATA_ENHANCED_SETTINGS_UI_URI,
        METADATA_DEVICE_TYPE,
        METADATA_MAIN_BATTERY,
        METADATA_MAIN_CHARGING,
        METADATA_MAIN_LOW_BATTERY_THRESHOLD,
        METADATA_UNTETHERED_LEFT_LOW_BATTERY_THRESHOLD,
        METADATA_UNTETHERED_RIGHT_LOW_BATTERY_THRESHOLD,
        METADATA_UNTETHERED_CASE_LOW_BATTERY_THRESHOLD,
        METADATA_SPATIAL_AUDIO,
        METADATA_FAST_PAIR_CUSTOMIZED_FIELDS,
        METADATA_LE_AUDIO,
        METADATA_GMCS_CCCD,
        METADATA_GTBS_CCCD
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class MetadataKey
}
