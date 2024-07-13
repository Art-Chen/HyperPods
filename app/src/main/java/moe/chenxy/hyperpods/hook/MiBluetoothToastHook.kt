package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.utils.SystemApisUtils
import moe.chenxy.hyperpods.utils.SystemApisUtils.cancelAsUser
import moe.chenxy.hyperpods.utils.SystemApisUtils.notifyAsUser
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showCaseBatteryToast
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showPodsBatteryToast
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@SuppressLint("MissingPermission")
object MiBluetoothToastHook : YukiBaseHooker(){
    private const val CASE_MP4_BASE64 = "AAAAGGZ0eXBtcDQyAAAAAG1wNDJtcDQxAAADI21vb3YAAABsbXZoZAAAAADiuBTm4rgU5gABX5AAAAXcAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAJ1dHJhawAAAFx0a2hkAAAAB+K4FObiuBTmAAAAAQAAAAAAAAXcAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAABuAAAAbgAAAAAAJGVkdHMAAAAcZWxzdAAAAAAAAAABAAAF3AAAAAAAAQAAAAAB7W1kaWEAAAAgbWRoZAAAAADiuBTm4rgU5gAA6mAAAAPoFccAAAAAAEBoZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAH01haW5jb25jZXB0IFZpZGVvIE1lZGlhIEhhbmRsZXIAAAGFbWluZgAAABR2bWhkAAAAAQAAAAAAAAAAAAAAM2hkbHIAAAAAAAAAAGFsaXMAAAAAAAAAAAAAAABBbGlhcyBEYXRhIEhhbmRsZXIAAAAAJGRpbmYAAAAcZHJlZgAAAAAAAAABAAAADHVybCAAAAABAAABEnN0YmwAAAChc3RzZAAAAAAAAAABAAAAkWF2YzEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAbgBuAEgAAABIAAAAAAAAAAEKQVZDIENvZGluZwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAY//8AAAA7YXZjQwFNACn/4QAkJ00AKZWxz+qwEQAAAwABAAADAHjoQADk4AAAq6GL3vg7Q4ZcAQAEKO48gAAAABhzdHRzAAAAAAAAAAEAAAABAAAD6AAAABxzdHNjAAAAAAAAAAEAAAABAAAAAQAAAAEAAAAUc3RzegAAAAAAAAm8AAAAAQAAABRzdGNvAAAAAAAAAAEAACsZAAAADXNkdHAAAAAAAAAAADp1ZHRhAAAAF6lUSU0ACwAAMDA6MDA6MDA6MDAAAAAOqVRTQwACAAA2MAAAAA2pVFNaAAEAADEAABAPdXVpZL56z8uXqULonHGZlJHjr6w8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pgo8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA3LjEtYzAwMCA3OS5iMGY4YmU5LCAyMDIxLzEyLzA4LTE5OjExOjIyICAgICAgICAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgeG1sbnM6eG1wRE09Imh0dHA6Ly9ucy5hZG9iZS5jb20veG1wLzEuMC9EeW5hbWljTWVkaWEvIgogICAgeG1sbnM6c3REaW09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9EaW1lbnNpb25zIyIKICAgIHhtbG5zOnRpZmY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vdGlmZi8xLjAvIgogICAgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iCiAgICB4bWxuczpzdEV2dD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL3NUeXBlL1Jlc291cmNlRXZlbnQjIgogICAgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIgogICB4bXA6Q3JlYXRlRGF0ZT0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcERNOnZpZGVvRnJhbWVSYXRlPSI2MC4wMDAwMDAiCiAgIHhtcERNOnZpZGVvRmllbGRPcmRlcj0iUHJvZ3Jlc3NpdmUiCiAgIHhtcERNOnZpZGVvUGl4ZWxBc3BlY3RSYXRpbz0iMS8xIgogICB4bXBETTpzdGFydFRpbWVTY2FsZT0iNjAiCiAgIHhtcERNOnN0YXJ0VGltZVNhbXBsZVNpemU9IjEiCiAgIHRpZmY6T3JpZW50YXRpb249IjEiCiAgIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6Nzk5OWJkMDEtMDEwNC1hMTQzLWFkOGEtMzUzYmU4YTFjZDUzIgogICB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgZGM6Zm9ybWF0PSJILjI2NCI+CiAgIDx4bXBETTpkdXJhdGlvbgogICAgeG1wRE06dmFsdWU9IjE1MDAiCiAgICB4bXBETTpzY2FsZT0iMS85MDAwMCIvPgogICA8eG1wRE06dmlkZW9GcmFtZVNpemUKICAgIHN0RGltOnc9IjExMCIKICAgIHN0RGltOmg9IjExMCIKICAgIHN0RGltOnVuaXQ9InBpeGVsIi8+CiAgIDx4bXBETTpzdGFydFRpbWVjb2RlCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIgogICAgeG1wRE06dGltZVZhbHVlPSIwMDowMDowMDowMCIvPgogICA8eG1wRE06YWx0VGltZWNvZGUKICAgIHhtcERNOnRpbWVWYWx1ZT0iMDA6MDA6MDA6MDAiCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIi8+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InNhdmVkIgogICAgICBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjc5OTliZDAxLTAxMDQtYTE0My1hZDhhLTM1M2JlOGExY2Q1MyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgICAgIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4KICAgIDwvcmRmOlNlcT4KICAgPC94bXBNTTpIaXN0b3J5PgogIDwvcmRmOkRlc2NyaXB0aW9uPgogPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4KICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgIAo8P3hwYWNrZXQgZW5kPSJ3Ij8+AAAXv2ZyZWUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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"
    @OptIn(ExperimentalEncodingApi::class)
    override fun onHook() {
        fun getCaseMp4Uri(context: Context): Uri? {
            val file = File(context.filesDir, "my_internal_files")
            if (file.exists()) {
                val file2 = File(file, "airpods_gen3_case.mp4")
                if (!file2.exists()) {
                    file2.createNewFile()
                    file2.writeBytes(Base64.decode(CASE_MP4_BASE64))
                    file2.setReadable(true)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.xiaomi.bluetooth.fileprovider",
                    file2
                )
                context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                return uri
            }
            return null
        }

        fun getResourcesUrl(context: Context, name: String) : Uri? {
            var uri: Uri? = null
            try {
                val file = File(context.filesDir, "my_internal_files")
                if (file.exists()) {
                    val str3 = "$name.mp4"
                    val file2 = File(file, str3)
                    if (file2.exists()) {
                        uri = FileProvider.getUriForFile(
                            context,
                            "com.xiaomi.bluetooth.fileprovider",
                            file2
                        )
                        context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            } catch (e: Exception) {
                Log.e("Art_Chen", "getResourcesUrl failed! ", e)
            }
            Log.d("Art_Chen", "getResourceUri uri = $uri")
            return uri

        }

        fun deleteIntent(context: Context, bluetoothDevice: BluetoothDevice): PendingIntent? {
            val intent = Intent("com.android.bluetooth.headset.notification.cancle")
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice)
            return PendingIntent.getBroadcast(context, 0, intent, 201326592)
        }


        @SuppressLint("WrongConstant")
        fun createPodsNotification(bluetoothDevice: BluetoothDevice?, context: Context, left: Int, right: Int, case: Int) {
            val miheadset_notification_Box = context.resources.getIdentifier("miheadset_notification_Box", "string", "com.xiaomi.bluetooth")
            val miheadset_notification_LeftEar = context.resources.getIdentifier("miheadset_notification_LeftEar", "string", "com.xiaomi.bluetooth")
            val miheadset_notification_RightEar = context.resources.getIdentifier("miheadset_notification_RightEar", "string", "com.xiaomi.bluetooth")
            val miheadset_notification_Disconnect = context.resources.getIdentifier("miheadset_notification_Disconnect", "string", "com.xiaomi.bluetooth")
            val system_notification_accent_color = context.resources.getIdentifier("system_notification_accent_color", "color", "android")
            val ic_headset_notification = context.resources.getIdentifier("ic_headset_notification", "drawable", "com.xiaomi.bluetooth")

            if (bluetoothDevice == null) {
                Log.e("Art_Chen", "createPodsNotification: btDevice null");
                return
            }
            try {
                val address: String = bluetoothDevice.address
                var alias: String? = bluetoothDevice.alias
                if (alias?.isEmpty() == true) {
                    alias = bluetoothDevice.name
                }

                val caseBattStr = if (case != -1) context.resources.getString(miheadset_notification_Box) + "：" + case + " %\n" else ""
                val leftEar = if (left != -1) context.resources.getString(miheadset_notification_LeftEar) + "：" + left + "%" else ""
                val leftToRight = if (left != -1 && right != -1) " | " else ""
                val rightEar = if (right != -1) leftToRight + context.resources.getString(miheadset_notification_RightEar) + "：" + right + "%" else ""

                val content: String = caseBattStr + leftEar + rightEar
                val notificationManager = context.getSystemService("notification") as NotificationManager
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        "BTHeadset$address",
                        alias,
                        NotificationManager.IMPORTANCE_MIN
                    )
                )
                val bundle = Bundle()
                bundle.putParcelable("Device", bluetoothDevice)
                val intent = Intent("com.android.bluetooth.headset.notification")
                intent.putExtra("btData", bundle)
                intent.putExtra("disconnect", "1")
                intent.setIdentifier("BTHeadset$address")
                val action = Notification.Action(
                    285737079,
                    context.resources
                        .getString(miheadset_notification_Disconnect),
                    PendingIntent.getBroadcast(context, 0, intent, 201326592)
                )
                val bundle2 = Bundle()
                bundle2.putBoolean("miui.showAction", true)
                bundle2.putParcelable(
                    "miui.appIcon",
                    Icon.createWithResource(context, ic_headset_notification)
                )
                notificationManager.notifyAsUser(
                    "BTHeadset$address",
                    10003,
                    Notification.Builder(context, "BTHeadset$address").setSmallIcon(
                        android.R.drawable.stat_sys_data_bluetooth
                    ).setWhen(0L).setTicker(alias).setDefaults(-1).setContentTitle(alias)
                        .setContentText(content)
                        .setDeleteIntent(deleteIntent(context, bluetoothDevice)).setColor(
                            context.getColor(
                                system_notification_accent_color
                            )
                        ).setExtras(bundle2).addAction(action).setVisibility(Notification.VISIBILITY_PUBLIC).build(),
                    SystemApisUtils.getUserAllUserHandle()
                )
            } catch (e: Exception) {
                Log.e("Art_Chen", "Failed to create Pod Notification", e)
            }
        }

        fun cancelNotification(bluetoothDevice: BluetoothDevice, context: Context) {
            try {
                val address = bluetoothDevice.address
                if (address.isNotEmpty()) {
                    val notificationManager = context.getSystemService("notification") as NotificationManager
                    notificationManager.cancelAsUser("BTHeadset$address", 10003, SystemApisUtils.getUserAllUserHandle())
                }
            } catch (e: Exception) {
                Log.e("Art_Chen", "Failed to cancel Pod Notification!", e)
            }
        }


        "com.android.bluetooth.ble.app.MiuiBluetoothNotification".toClass().apply {
            constructor {
                paramCount = 2
            }.hook {
                after {
                    // update case video first
                    val context = XposedHelpers.getObjectField(this.instance, "mContext") as Context
                    val file = File(context.filesDir, "my_internal_files")
                    if (file.exists()) {
                        val file2 = File(file, "airpods_gen3_case.mp4")
                        if (file2.exists()) {
                            file2.delete()
                        }
                    }

                    val broadcastReceiver = object : BroadcastReceiver() {
                        override fun onReceive(p0: Context?, p1: Intent?) {
                            if (p1?.action == "chen.action.hyperpods.sendstrongtoast") {
                                val leftBatt = p1.getIntExtra("left", -1)
                                val leftCharging = p1.getBooleanExtra("leftCharging", false)
                                val rightBatt = p1.getIntExtra("right", -1)
                                val rightCharging = p1.getBooleanExtra("rightCharging", false)
                                val caseBatt = p1.getIntExtra("case", -1)
                                val caseCharging = p1.getBooleanExtra("caseCharging", false)
                                val lowBatt = p1.getIntExtra("lowBatt", -1)

                                Log.i("Art_Chen", "Showing AirPods connected toast")
                                val caseUri = getCaseMp4Uri(context)
                                if (leftBatt == -1 && rightBatt == -1 && caseBatt != -1 && caseUri != null) {
                                    showCaseBatteryToast(context, caseBatt, caseCharging, caseUri, lowBatt)
                                    return
                                }
                                val leftUri =
                                    if (leftBatt != -1)
                                        getResourcesUrl(context, "earphone_left_inear")
                                    else
                                        getResourcesUrl(context, "earphone_left_no_inear")
                                val rightUri =
                                    if (rightBatt != -1)
                                        getResourcesUrl(context, "earphone_right_inear")
                                    else
                                        getResourcesUrl(context, "earphone_right_no_inear")


                                if (leftUri != null && rightUri != null && caseUri != null) {
                                    showPodsBatteryToast(
                                        context,
                                        leftBatt,
                                        leftCharging,
                                        leftUri,
                                        rightBatt,
                                        rightCharging,
                                        rightUri,
                                        caseBatt,
                                        caseCharging,
                                        caseUri,
                                        lowBatt
                                    )
                                }
                            } else if (p1?.action == "chen.action.hyperpods.updatepodsnotification") {
                                val leftBatt = p1.getIntExtra("left", -1)
                                val rightBatt = p1.getIntExtra("right", -1)
                                val caseBatt = p1.getIntExtra("case", -1)
                                val device = p1.getParcelableExtra("device", BluetoothDevice::class.java)
                                createPodsNotification(device, context, leftBatt, rightBatt, caseBatt)
                            } else if (p1?.action == "chen.action.hyperpods.cancelpodsnotification") {
                                val device = p1.getParcelableExtra("device", BluetoothDevice::class.java) as BluetoothDevice
                                cancelNotification(device, context)
                            }
                        }

                    }

                    val intentFilter = IntentFilter("chen.action.hyperpods.sendstrongtoast")
                    intentFilter.addAction("chen.action.hyperpods.updatepodsnotification")
                    intentFilter.addAction("chen.action.hyperpods.cancelpodsnotification")
                    context.registerReceiver(broadcastReceiver, intentFilter,
                        Context.RECEIVER_NOT_EXPORTED)
                }
            }
        }
    }

}