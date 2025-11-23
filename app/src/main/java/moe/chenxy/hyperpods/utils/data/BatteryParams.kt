package moe.chenxy.hyperpods.utils.data
import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import moe.chenxy.hyperpods.pods.EarDetectionStatus

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class PodBatteryParams (
    var battery: Int = 0,
    var isCharging: Boolean = false,
    var isConnected: Boolean = false,
    var rawStatus: Int = 0
) : Parcelable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class BatteryParams(
    var left: PodBatteryParams? = null,
    var right: PodBatteryParams? = null,
    var case: PodBatteryParams? = null
) : Parcelable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class EarDetectionParams(
    var left: Byte = EarDetectionStatus.IN_CASE,
    var right: Byte = EarDetectionStatus.IN_CASE,
) : Parcelable