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
import android.os.Process
import android.util.Log
import androidx.core.content.FileProvider
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.constructor
import com.hyperfocus.api.FocusApi
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.SystemApisUtils
import moe.chenxy.hyperpods.utils.SystemApisUtils.cancelAsUser
import moe.chenxy.hyperpods.utils.SystemApisUtils.isHyperOS3
import moe.chenxy.hyperpods.utils.SystemApisUtils.notifyAsUser
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showCaseBatteryToast
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showPodsBatteryToast
import org.json.JSONObject
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min


@SuppressLint("MissingPermission")
object MiBluetoothToastHook : YukiBaseHooker(){
    private const val CASE_MP4_BASE64 = "AAAAGGZ0eXBtcDQyAAAAAG1wNDJtcDQxAAADI21vb3YAAABsbXZoZAAAAADiuBTm4rgU5gABX5AAAAXcAAEAAAEAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAJ1dHJhawAAAFx0a2hkAAAAB+K4FObiuBTmAAAAAQAAAAAAAAXcAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAQAAAAABuAAAAbgAAAAAAJGVkdHMAAAAcZWxzdAAAAAAAAAABAAAF3AAAAAAAAQAAAAAB7W1kaWEAAAAgbWRoZAAAAADiuBTm4rgU5gAA6mAAAAPoFccAAAAAAEBoZGxyAAAAAAAAAAB2aWRlAAAAAAAAAAAAAAAAH01haW5jb25jZXB0IFZpZGVvIE1lZGlhIEhhbmRsZXIAAAGFbWluZgAAABR2bWhkAAAAAQAAAAAAAAAAAAAAM2hkbHIAAAAAAAAAAGFsaXMAAAAAAAAAAAAAAABBbGlhcyBEYXRhIEhhbmRsZXIAAAAAJGRpbmYAAAAcZHJlZgAAAAAAAAABAAAADHVybCAAAAABAAABEnN0YmwAAAChc3RzZAAAAAAAAAABAAAAkWF2YzEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAbgBuAEgAAABIAAAAAAAAAAEKQVZDIENvZGluZwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAY//8AAAA7YXZjQwFNACn/4QAkJ00AKZWxz+qwEQAAAwABAAADAHjoQADk4AAAq6GL3vg7Q4ZcAQAEKO48gAAAABhzdHRzAAAAAAAAAAEAAAABAAAD6AAAABxzdHNjAAAAAAAAAAEAAAABAAAAAQAAAAEAAAAUc3RzegAAAAAAAAm8AAAAAQAAABRzdGNvAAAAAAAAAAEAACsZAAAADXNkdHAAAAAAAAAAADp1ZHRhAAAAF6lUSU0ACwAAMDA6MDA6MDA6MDAAAAAOqVRTQwACAAA2MAAAAA2pVFNaAAEAADEAABAPdXVpZL56z8uXqULonHGZlJHjr6w8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pgo8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJBZG9iZSBYTVAgQ29yZSA3LjEtYzAwMCA3OS5iMGY4YmU5LCAyMDIxLzEyLzA4LTE5OjExOjIyICAgICAgICAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIgogICAgeG1sbnM6eG1wRE09Imh0dHA6Ly9ucy5hZG9iZS5jb20veG1wLzEuMC9EeW5hbWljTWVkaWEvIgogICAgeG1sbnM6c3REaW09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9EaW1lbnNpb25zIyIKICAgIHhtbG5zOnRpZmY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vdGlmZi8xLjAvIgogICAgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iCiAgICB4bWxuczpzdEV2dD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL3NUeXBlL1Jlc291cmNlRXZlbnQjIgogICAgeG1sbnM6ZGM9Imh0dHA6Ly9wdXJsLm9yZy9kYy9lbGVtZW50cy8xLjEvIgogICB4bXA6Q3JlYXRlRGF0ZT0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgeG1wOk1vZGlmeURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgIHhtcDpNZXRhZGF0YURhdGU9IjIwMjQtMDctMTNUMTk6MjY6MzArMDg6MDAiCiAgIHhtcERNOnZpZGVvRnJhbWVSYXRlPSI2MC4wMDAwMDAiCiAgIHhtcERNOnZpZGVvRmllbGRPcmRlcj0iUHJvZ3Jlc3NpdmUiCiAgIHhtcERNOnZpZGVvUGl4ZWxBc3BlY3RSYXRpbz0iMS8xIgogICB4bXBETTpzdGFydFRpbWVTY2FsZT0iNjAiCiAgIHhtcERNOnN0YXJ0VGltZVNhbXBsZVNpemU9IjEiCiAgIHRpZmY6T3JpZW50YXRpb249IjEiCiAgIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6Nzk5OWJkMDEtMDEwNC1hMTQzLWFkOGEtMzUzYmU4YTFjZDUzIgogICB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOmNhNjMzZWNlLTFlYzctYzU0My1iNjUwLTM2OTBiOWE0MjZkMCIKICAgZGM6Zm9ybWF0PSJILjI2NCI+CiAgIDx4bXBETTpkdXJhdGlvbgogICAgeG1wRE06dmFsdWU9IjE1MDAiCiAgICB4bXBETTpzY2FsZT0iMS85MDAwMCIvPgogICA8eG1wRE06dmlkZW9GcmFtZVNpemUKICAgIHN0RGltOnc9IjExMCIKICAgIHN0RGltOmg9IjExMCIKICAgIHN0RGltOnVuaXQ9InBpeGVsIi8+CiAgIDx4bXBETTpzdGFydFRpbWVjb2RlCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIgogICAgeG1wRE06dGltZVZhbHVlPSIwMDowMDowMDowMCIvPgogICA8eG1wRE06YWx0VGltZWNvZGUKICAgIHhtcERNOnRpbWVWYWx1ZT0iMDA6MDA6MDA6MDAiCiAgICB4bXBETTp0aW1lRm9ybWF0PSI2MFRpbWVjb2RlIi8+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InNhdmVkIgogICAgICBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOjc5OTliZDAxLTAxMDQtYTE0My1hZDhhLTM1M2JlOGExY2Q1MyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyNC0wNy0xM1QxOToyNjozMCswODowMCIKICAgICAgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wIDIzLjIgKDIwMjIwMTE4Lm9yaWcuNTIxIDkzMGFhNDgpICAoV2luZG93cykiCiAgICAgIHN0RXZ0OmNoYW5nZWQ9Ii8iLz4KICAgIDwvcmRmOlNlcT4KICAgPC94bXBNTTpIaXN0b3J5PgogIDwvcmRmOkRlc2NyaXB0aW9uPgogPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4KICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgIAo8P3hwYWNrZXQgZW5kPSJ3Ij8+AAAXv2ZyZWUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA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"

    var miheadset_notification_Box: Int = -1
    var miheadset_notification_LeftEar: Int = -1
    var miheadset_notification_RightEar: Int = -1
    var miheadset_notification_Disconnect: Int = -1
    var earphone_drawable: Int = -1
    var system_notification_accent_color: Int = -1
    var ic_headset_notification: Int = -1

    lateinit var mThiz: Any
    val focusApi = FocusApi

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

        fun initResources(context: Context) {
            if (miheadset_notification_Box == -1) {
                miheadset_notification_Box = context.resources.getIdentifier("miheadset_notification_Box", "string", "com.xiaomi.bluetooth")
                miheadset_notification_LeftEar = context.resources.getIdentifier("miheadset_notification_LeftEar", "string", "com.xiaomi.bluetooth")
                miheadset_notification_RightEar = context.resources.getIdentifier("miheadset_notification_RightEar", "string", "com.xiaomi.bluetooth")
                miheadset_notification_Disconnect = context.resources.getIdentifier("miheadset_notification_Disconnect", "string", "com.xiaomi.bluetooth")
                system_notification_accent_color = context.resources.getIdentifier("system_notification_accent_color", "color", "android")
                ic_headset_notification = context.resources.getIdentifier("ic_headset_notification", "drawable", "com.xiaomi.bluetooth")
                earphone_drawable = context.resources.getIdentifier("earphone", "drawable", "com.xiaomi.bluetooth")
            }
        }

        fun buildNotification(bluetoothDevice: BluetoothDevice, context: Context, batteryParams: BatteryParams): Notification.Builder {
            initResources(context)

            val address: String = bluetoothDevice.address
            var alias: String? = bluetoothDevice.alias
            if (alias?.isEmpty() == true) {
                alias = bluetoothDevice.name
            }

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
            intent.identifier = "BTHeadset$address"
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
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent("chen.action.hyperpods.show_airpods_ui"),
                PendingIntent.FLAG_IMMUTABLE
            )

            val caseBattStr = if (batteryParams.case!!.isConnected)
                "${context.resources.getString(miheadset_notification_Box)}：${batteryParams.case!!.battery} %" +
                        "${if (batteryParams.case!!.isCharging) " ⚡" else ""}\n"
            else ""
            val leftEar = if (batteryParams.left!!.isConnected) "${context.resources.getString(miheadset_notification_LeftEar)}：${batteryParams.left!!.battery} %" +
                    (if (batteryParams.left!!.isCharging) " ⚡" else "") else ""
            val leftToRight = if (batteryParams.left!!.isConnected && batteryParams.right!!.isConnected) " | " else ""
            val rightEar = if (batteryParams.right!!.isConnected) "$leftToRight${context.resources.getString(miheadset_notification_RightEar)}：${batteryParams.right!!.battery} %" +
                    (if (batteryParams.right!!.isCharging) " ⚡" else "") else ""

            val content: String = caseBattStr + leftEar + rightEar
            return Notification.Builder(context, "BTHeadset$address").setSmallIcon(
                android.R.drawable.stat_sys_data_bluetooth
            ).setWhen(0L).setTicker(alias).setDefaults(-1).setContentTitle(alias)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deleteIntent(context, bluetoothDevice)).setColor(
                    context.getColor(
                        system_notification_accent_color
                    )
                ).setExtras(bundle2).addAction(action).setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        fun showConnectedToast(bluetoothDevice: BluetoothDevice, context: Context) {
            initResources(context)
            val notificationManager = context.getSystemService("notification") as NotificationManager
            val connectedStrID = context.resources.getIdentifier("headset_autoswitch_connected", "string", "com.xiaomi.bluetooth")

            val baseInfo = focusApi.baseinfo(title = bluetoothDevice.name,
                basetype = 0, content = context.getString(connectedStrID))
            val api = focusApi.sendFocus(
                title = "BTHeadset${bluetoothDevice.address}-connect",
                cancel = false,
                isShowNotification = false,
                showSmallIcon = false,
                baseInfo = baseInfo,
                enableFloat = true,
                reopen = "true", // allow notify again after notification cleaned
                picInfo = Icon.createWithResource(context, earphone_drawable),
                picInfotype = 2,
                timeout = 1000,
                ticker = context.getString(connectedStrID),
                picticker = Icon.createWithResource(context, earphone_drawable)
            )
            val a = Bundle()
            a.putString("miui.effect.src","true")
            a.putAll(api)

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "HEADSET_CONNECT_NOTIFICATION",
                    "HEADSET_CONNECT_NOTIFICATION",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            val notificationBuild: Notification =
                Notification.Builder(context, "HEADSET_CONNECT_NOTIFICATION")
                    .setSmallIcon(earphone_drawable).addExtras(a).build()

            notificationManager.notifyAsUser(
                "HEADSET_CONNECT_NOTIFICATION",
                1001,
                notificationBuild,
                Process.myUserHandle()
            )
        }

        fun createPodsNotificationOS3(bluetoothDevice: BluetoothDevice, context: Context, batteryParams: BatteryParams) {
            val sendNotification = buildNotification(bluetoothDevice, context, batteryParams)

            val picProfiles = focusApi.addpics("icon",Icon.createWithResource(context, earphone_drawable))
            val pics = Bundle()
            pics.putAll(picProfiles)
//            val actions = focusApi.actionInfo(actionIntent = actionIntent, actionIntentType = "2", actionTitle = context.resources
//                .getString(miheadset_notification_Disconnect))

            val disconnectBundle = Bundle()
            disconnectBundle.putParcelable("Device", bluetoothDevice)
            val intent = Intent("com.android.bluetooth.headset.notification")
            intent.putExtra("btData", disconnectBundle)
            intent.putExtra("disconnect", "1")
            intent.identifier = "BTHeadset${bluetoothDevice.address}"
            val action = Notification.Action.Builder(
                null,
                context.resources
                    .getString(miheadset_notification_Disconnect),
                PendingIntent.getBroadcast(context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            ).build()

            val bundle = Bundle()
            bundle.putParcelable("miui.focus.action_disconnect", action)

            var minBatteryLevel =
                if (batteryParams.left?.isConnected == true && batteryParams.right?.isConnected == true)
                    min(batteryParams.left?.battery!!, batteryParams.right?.battery!!)
                else if (batteryParams.left?.isConnected == true) {
                    batteryParams.left?.battery
                } else if (batteryParams.right?.isConnected == true) {
                    batteryParams.right?.battery
                }
                else 0

            if (minBatteryLevel == -1) minBatteryLevel = 0

            val caseBattStr =
                "${context.resources.getString(miheadset_notification_Box)}：" + if (batteryParams.case!!.isConnected) "${batteryParams.case!!.battery} %" else "- %" +
                        "${if (batteryParams.case!!.isCharging) " ⚡" else ""}\n"
            val leftEar = if (batteryParams.left!!.isConnected) "${context.resources.getString(miheadset_notification_LeftEar)}：${batteryParams.left!!.battery} %" +
                    (if (batteryParams.left!!.isCharging) " ⚡" else "") else ""
            val leftToRight = if (batteryParams.left!!.isConnected && batteryParams.right!!.isConnected) " | " else ""
            val rightEar = if (batteryParams.right!!.isConnected) "$leftToRight${context.resources.getString(miheadset_notification_RightEar)}：${batteryParams.right!!.battery} %" +
                    (if (batteryParams.right!!.isCharging) " ⚡" else "") else ""
            val actionObject = JSONObject()
            actionObject.put("action", "miui.focus.action_disconnect")

            val baseInfo = focusApi.baseinfo(title = "$leftEar$rightEar",
                basetype = 1, content = caseBattStr)
            val hintInfo = focusApi.hintInfo(type = 2,
                titleLineCount = 1,
                title = bluetoothDevice.name,
                content = "HyperPods",
                actionInfo = actionObject)
            val api = focusApi.sendFocus(
                title = "BTHeadset${bluetoothDevice.address}",
                cancel = false,
                baseInfo = baseInfo,
                hintInfo = hintInfo,
                enableFloat = false,
                reopen = "true", // allow notify again after notification cleaned
                picInfo = Icon.createWithResource(context, earphone_drawable),
                picInfotype = 2,
                timeout = 1000,
                ticker = "$minBatteryLevel %",
                picticker = Icon.createWithResource(context, earphone_drawable)
            )
            val a = Bundle()
            a.putString("miui.effect.src","true")
            a.putBundle("miui.focus.actions", bundle)
            a.putCharSequence("miui.targetPkg", BuildConfig.APPLICATION_ID)
            a.putAll(api)
            sendNotification.addExtras(a)
            sendNotification.setOngoing(true)
            val notificationManager = context.getSystemService("notification") as NotificationManager
            notificationManager.cancelAsUser("HEADSET_CONNECT_NOTIFICATION", 1001, Process.myUserHandle())
            notificationManager.notifyAsUser(
                "BTHeadset${bluetoothDevice.address}",
                10003,
                sendNotification.build(),
                Process.myUserHandle()
            )
        }

        @SuppressLint("WrongConstant")
        fun createPodsNotification(bluetoothDevice: BluetoothDevice?, context: Context, batteryParams: BatteryParams) {
            if (bluetoothDevice == null) {
                Log.e("Art_Chen", "createPodsNotification: btDevice null");
                return
            }
            if (isHyperOS3) return createPodsNotificationOS3(bluetoothDevice, context, batteryParams)

            try {
                val address: String = bluetoothDevice.address
                var alias: String? = bluetoothDevice.alias
                if (alias?.isEmpty() == true) {
                    alias = bluetoothDevice.name
                }

                val notificationManager = context.getSystemService("notification") as NotificationManager
                notificationManager.notifyAsUser(
                    "BTHeadset$address",
                    10003,
                    buildNotification(bluetoothDevice, context, batteryParams).build(),
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
                    notificationManager.cancelAsUser("BTHeadset$address", 10003, Process.myUserHandle())
                    notificationManager.cancelAsUser("HEADSET_CONNECT_NOTIFICATION", 1001, Process.myUserHandle())
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
                    mThiz = this.instance
                    // update case video first
                    val context = XposedHelpers.getObjectField(this.instance, "mContext") as Context
                    val file = File(context.filesDir, "my_internal_files")
                    if (file.exists()) {
                        val file2 = File(file, "airpods_gen3_case.mp4")
                        if (file2.exists()) {
                            if (Base64.encode(file2.readBytes(), 0) != CASE_MP4_BASE64)
                                file2.delete()
                        }
                    }

                    val broadcastReceiver = object : BroadcastReceiver() {
                        override fun onReceive(p0: Context?, p1: Intent?) {
                            if (p1?.action == "chen.action.hyperpods.sendstrongtoast") {
                                val batteryParams = p1.getParcelableExtra("batteryParams", BatteryParams::class.java)!!
                                val caseBatt = batteryParams.case!!.battery
                                val caseCharging = batteryParams.case!!.isCharging
                                val lowBatt = 20

                                val caseUri = getCaseMp4Uri(context)
                                if (!batteryParams.left!!.isConnected && !batteryParams.right!!.isConnected && batteryParams.case!!.isConnected && caseUri != null) {
                                    batteryParams.case?.let {
                                        showCaseBatteryToast(context, caseBatt, caseCharging, caseUri, lowBatt)
                                    }
                                    return
                                }
                                val leftUri =
                                    if (batteryParams.left!!.isConnected)
                                        getResourcesUrl(context, "earphone_left_inear")
                                    else
                                        getResourcesUrl(context, "earphone_left_no_inear")
                                val rightUri =
                                    if (batteryParams.right!!.isConnected)
                                        getResourcesUrl(context, "earphone_right_inear")
                                    else
                                        getResourcesUrl(context, "earphone_right_no_inear")


                                if (leftUri != null && rightUri != null && caseUri != null) {
                                    showPodsBatteryToast(
                                        context,
                                        leftUri,
                                        rightUri,
                                        caseUri,
                                        lowBatt,
                                        batteryParams
                                    )

                                }
                            } else if (p1?.action == "chen.action.hyperpods.updatepodsnotification") {
                                val batteryParams = p1.getParcelableExtra<BatteryParams>("batteryParams", BatteryParams::class.java)
                                val device = p1.getParcelableExtra("device", BluetoothDevice::class.java)
                                createPodsNotification(device, context, batteryParams!!)
                            } else if (p1?.action == "chen.action.hyperpods.cancelpodsnotification") {
                                val device = p1.getParcelableExtra("device", BluetoothDevice::class.java) as BluetoothDevice
                                cancelNotification(device, context)
                            } else if (p1?.action == "chen.action.hyperpods.podconnecting") {
                                val device = p1.getParcelableExtra("device", BluetoothDevice::class.java) as BluetoothDevice
//                                XposedHelpers.callMethod(mThiz, "showConnectingToast", device.name, true, "00000000")
                                showConnectedToast(device, context)
                            }
                        }

                    }

                    val intentFilter = IntentFilter("chen.action.hyperpods.sendstrongtoast")
                    intentFilter.addAction("chen.action.hyperpods.updatepodsnotification")
                    intentFilter.addAction("chen.action.hyperpods.cancelpodsnotification")
                    intentFilter.addAction("chen.action.hyperpods.podconnecting")
                    context.registerReceiver(broadcastReceiver, intentFilter,
                        Context.RECEIVER_NOT_EXPORTED)
                }
            }
        }
    }

}