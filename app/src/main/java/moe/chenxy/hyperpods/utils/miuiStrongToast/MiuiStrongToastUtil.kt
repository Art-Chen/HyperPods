package moe.chenxy.hyperpods.utils.miuiStrongToast

import StringToastBundle
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
import kotlinx.serialization.json.Json
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.SystemApisUtils.isHyperOS
import moe.chenxy.hyperpods.utils.miuiStrongToast.data.IconParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.data.Left
import moe.chenxy.hyperpods.utils.miuiStrongToast.data.Right
import moe.chenxy.hyperpods.utils.miuiStrongToast.data.StringToastBean
import moe.chenxy.hyperpods.utils.miuiStrongToast.data.TextParams

@SuppressLint("WrongConstant")
object MiuiStrongToastUtil {

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
            val stringToastBean = StringToastBean(left, right)
            val jsonStr = Json.encodeToString(StringToastBean.serializer(), stringToastBean)
            val bundle = StringToastBundle.Builder()
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

    fun showCaseBatteryToast(context: Context, case: Int, caseCharing: Boolean, caseMp4Uri: Uri, lowBatteryThreshold: Int) {
        if (!isHyperOS) {
            return
        }
        try {
            val caseText = TextParams("$case %", if (caseCharing) Color.GREEN else if (case <= lowBatteryThreshold) Color.RED else Color.WHITE)
            val caseVideo = IconParams(Category.RAW, FileType.MP4, caseMp4Uri.toString(), 1)
            val left = Left(iconParams = caseVideo)
            val right = Right(textParams = caseText)
            val stringToastBean = StringToastBean(left, right)
            val jsonStr = Json.encodeToString(StringToastBean.serializer(), stringToastBean)
            val bundle = StringToastBundle.Builder()
                .setPackageName("com.xiaomi.bluetooth")
                .setStrongToastCategory(StrongToastCategory.VIDEO_TEXT)
                .setTarget(null)
                .setParam(jsonStr)
                .onCreate()
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
        left: Int,
        leftCharging: Boolean,
        leftVideoUri: Uri,
        right: Int,
        rightCharging: Boolean,
        rightVideoUri: Uri,
        case: Int,
        caseCharing: Boolean,
        caseMp4Uri: Uri,
        lowBatteryThreshold: Int
    ) {
        if (!isHyperOS) {
            return
        }
        try {
            val leftText =
                TextParams(if (left != -1) "$left %" else "", if (leftCharging) Color.GREEN else if (left <= lowBatteryThreshold) Color.RED else Color.WHITE)
            val leftVideo = IconParams(Category.RAW, FileType.MP4, leftVideoUri.toString(), 1)
            val rightText =
                TextParams(if (right != -1) "$right %" else "", if (rightCharging) Color.GREEN else if (right <= lowBatteryThreshold) Color.RED else Color.WHITE)
            val rightVideo = IconParams(Category.RAW, FileType.MP4, rightVideoUri.toString(), 1)
            val l = Left(textParams = leftText, iconParams = leftVideo)
            val r = Right(textParams = rightText, iconParams = rightVideo)
            val stringToastBean = StringToastBean(l, r)
            val jsonStr = Json.encodeToString(StringToastBean.serializer(), stringToastBean)
            val bundle = StringToastBundle.Builder()
                .setPackageName("com.xiaomi.bluetooth")
                .setStrongToastCategory(StrongToastCategory.VIDEO_TEXT_TEXT_VIDEO)
                .setTarget(null)
                .setParam(jsonStr)
                .onCreate()
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus", Int::class.javaPrimitiveType, String::class.java, Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
            if (case != -1) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showCaseBatteryToast(context, case, caseCharing, caseMp4Uri, lowBatteryThreshold)
                }, 3500)
            }
        } catch (e: Exception) {
            Log.e("Art_Chen", "Failed to show Pods Battery Toast")
        }
    }

    fun showPodsBatteryToastByMiuiBt(
        context: Context,
        left: Int,
        leftCharging: Boolean,
        right: Int,
        rightCharging: Boolean,
        case: Int,
        caseCharing: Boolean,
        lowBatteryThreshold: Int
    ) {
        val intent = Intent("chen.action.hyperpods.sendstrongtoast")

        intent.putExtra("left", left)
        intent.putExtra("leftCharging", leftCharging)
        intent.putExtra("right", right)
        intent.putExtra("rightCharging", rightCharging)
        intent.putExtra("case", case)
        intent.putExtra("caseCharging", caseCharing)
        intent.putExtra("lowBatt", lowBatteryThreshold)

        intent.`package` = "com.xiaomi.bluetooth"

        context.sendBroadcast(intent)
    }

    fun showPodsNotificationByMiuiBt(
        context: Context,
        left: Int,
        right: Int,
        case: Int,
        device: BluetoothDevice,
    ) {
        val intent = Intent("chen.action.hyperpods.updatepodsnotification")

        intent.putExtra("left", left)
        intent.putExtra("right", right)
        intent.putExtra("case", case)
        intent.putExtra("device", device)

        intent.`package` = "com.xiaomi.bluetooth"

        context.sendBroadcast(intent)
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