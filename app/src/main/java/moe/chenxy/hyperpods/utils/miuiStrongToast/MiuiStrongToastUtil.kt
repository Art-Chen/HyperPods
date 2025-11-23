package moe.chenxy.hyperpods.utils.miuiStrongToast

import DeviceNotificationBundle
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.SystemApisUtils.isHyperOS
import moe.chenxy.hyperpods.utils.SystemApisUtils.isHyperOS3
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.IconParams
import moe.chenxy.hyperpods.utils.data.Left
import moe.chenxy.hyperpods.utils.data.Right
import moe.chenxy.hyperpods.utils.data.StrongToastBean
import moe.chenxy.hyperpods.utils.data.TextParams
import androidx.core.graphics.toColorInt

@SuppressLint("WrongConstant")
object MiuiStrongToastUtil {
    var lastPodsTimestamp = -1L
    val colorGreen = "#FF34C759".toColorInt()
    val colorRed = "#FFFF3B30".toColorInt()

    fun showStringToast(context: Context, text: String?, colorType: Int) {
        if (!isHyperOS) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
            return
        }
        try {
            val textParams = TextParams(text, if (colorType == 1) Color.parseColor("#4CAF50") else Color.parseColor("#E53935"))
            val left = Left(textParams = textParams)
            val iconParams = IconParams(Category.DRAWABLE, FileType.SVG, "ic_launcher", 1)
            val right = Right(iconParams = iconParams)
            val strongToastBean = StrongToastBean(left, right)
            val jsonStr = Json.encodeToString(StrongToastBean.serializer(), strongToastBean)
            val bundle = DeviceNotificationBundle.Builder()
                .setPackageName(BuildConfig.APPLICATION_ID)
                .setStrongToastCategory(StrongToastCategory.TEXT_BITMAP_INTENT)
                .setTarget(null)
                .setParam(jsonStr)
                .onCreate()
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
        } catch (e: Exception) {
            Log.e("Art_Chen", "Failed to show HyperOS String Toast")
        }
    }

    fun showCaseBatteryToast(context: Context, case: Int, caseCharging: Boolean, caseMp4Uri: Uri, lowBatteryThreshold: Int) {
        if (!isHyperOS) {
            return
        }
        val caseText = TextParams("$case %", if (caseCharging) colorGreen else if (case <= lowBatteryThreshold) colorRed else Color.WHITE)
        val caseVideo = IconParams(Category.RAW, FileType.MP4, caseMp4Uri.toString(), 1)
        val left = Left(iconParams = caseVideo)
        val right = Right(textParams = caseText)
        val strongToastBean = StrongToastBean(left, right)
        val jsonStr = Json.encodeToString(StrongToastBean.serializer(), strongToastBean)
        val bundle = DeviceNotificationBundle.Builder()
            .setPackageName("com.xiaomi.bluetooth")
            .setStrongToastCategory(StrongToastCategory.VIDEO_TEXT)
            .setNotifyId("headset_wear_notification")
            .setTarget(null)
            .setParam(jsonStr)
            .onCreate()

        try {
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            Log.d("Art_Chen", "Showing Case Batt Toast")
            service.javaClass.getMethod(
                "setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
        } catch (e: Exception) {
            Log.e("Art_Chen", "Failed to show Case Battery Toast")
        }
    }

    fun showPodsBatteryToast(
        context: Context,
        leftVideoUri: Uri,
        rightVideoUri: Uri,
        caseMp4Uri: Uri,
        lowBatteryThreshold: Int = 20,
        batteryParams: BatteryParams
    ) {
        if (!isHyperOS) {
            return
        }
        val left = batteryParams.left!!.battery
        val leftCharging = batteryParams.left!!.isCharging
        val right = batteryParams.right!!.battery
        val rightCharging = batteryParams.right!!.isCharging
        val case = batteryParams.case!!.battery
        val caseCharging = batteryParams.case!!.isCharging

        val leftText =
            if (batteryParams.left!!.isConnected) TextParams("$left %", if (leftCharging) colorGreen else if (left <= lowBatteryThreshold) colorRed else Color.WHITE, turnAnim = true) else null
        val leftVideo = IconParams(Category.RAW, FileType.MP4, leftVideoUri.toString(), 1)
        val rightText =
            if (batteryParams.right!!.isConnected) TextParams( "$right %", if (rightCharging) colorGreen else if (right <= lowBatteryThreshold) colorRed else Color.WHITE, turnAnim = true) else null
        val rightVideo = IconParams(Category.RAW, FileType.MP4, rightVideoUri.toString(), 1)
        val l = Left(textParams = leftText, iconParams = leftVideo)
        val r = Right(textParams = rightText, iconParams = rightVideo)
        val strongToastBean = StrongToastBean(l, r)
        val jsonStr = Json.encodeToString(StrongToastBean.serializer(), strongToastBean)
        val bundle = DeviceNotificationBundle.Builder()
            .setPackageName("com.xiaomi.bluetooth")
            .setStrongToastCategory(StrongToastCategory.VIDEO_TEXT_TEXT_VIDEO)
            .setDuration(7000)
            .setNotifyId("headset_wear_notification")
            .setTarget(null)
            .setParam(jsonStr)
            .onCreate()
        try {
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
            lastPodsTimestamp = System.currentTimeMillis()
            if (batteryParams.case!!.isConnected) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(4000)
                    showCaseBatteryToast(
                        context,
                        case,
                        caseCharging,
                        caseMp4Uri,
                        lowBatteryThreshold
                    )
                }
            }
        } catch (_: Exception) {
            Log.e("Art_Chen", "Failed to show Pods Battery Toast")
        }
    }

    fun showPodsBatteryToastByMiuiBt(
        context: Context?,
        batteryParams: BatteryParams
    ) {
        val intent = Intent("chen.action.hyperpods.sendstrongtoast")

        intent.putExtra("batteryParams", batteryParams)

        intent.`package` = "com.xiaomi.bluetooth"

        context?.sendBroadcast(intent)
    }

    fun showPodsNotificationByMiuiBt(
        context: Context?,
        batteryParams: BatteryParams,
        device: BluetoothDevice,
    ) {
        val intent = Intent("chen.action.hyperpods.updatepodsnotification")

        intent.putExtra("batteryParams", batteryParams)
        intent.putExtra("device", device)

        intent.`package` = "com.xiaomi.bluetooth"

        context?.sendBroadcast(intent)
    }

    fun cancelPodsNotificationByMiuiBt(
        context: Context,
        device: BluetoothDevice,
    ) {
        val intent = Intent("chen.action.hyperpods.cancelpodsnotification")

        intent.putExtra("device", device)

        intent.`package` = "com.xiaomi.bluetooth"

        context.sendBroadcast(intent)
    }

    fun showPodConnectingByMiuiBt(context: Context, device: BluetoothDevice) {
        val intent = Intent("chen.action.hyperpods.podconnecting")
        intent.putExtra("device", device)
        intent.`package` = "com.xiaomi.bluetooth"
        context.sendBroadcast(intent)
    }

    object Category {
        const val RAW = "raw"
        const val DRAWABLE = "drawable"
        const val FILE = "file"
        const val MIPMAP = "mipmap"
    }

    object FileType {
        const val MP4 = "mp4"
        const val PNG = "png"
        const val SVG = "svg"
    }

    object StrongToastCategory {
        const val VIDEO_TEXT = "video_text"
        const val VIDEO_BITMAP_INTENT = "video_bitmap_intent"
        const val TEXT_BITMAP = "text_bitmap"
        const val TEXT_BITMAP_INTENT = "text_bitmap_intent"
        const val VIDEO_TEXT_TEXT_VIDEO = "video_text_text_video"
    }

}