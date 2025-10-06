package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.pods.L2CAPController
import moe.chenxy.hyperpods.utils.SystemApisUtils.setIconVisibility
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.showPodConnectingByMiuiBt

object HeadsetStateDispatcher : YukiBaseHooker() {
    private var isShowedToast = false
    private val airPodsUUIDs = hashSetOf(
        ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
        ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
    )

    @SuppressLint("PrivateApi")
    private fun getBooleanProp(prop: String, def: Boolean): Boolean {
        return XposedHelpers.callStaticMethod(Class.forName("android.os.SystemProperties"), "getBoolean", prop, def) as Boolean
    }

    external fun nativeGetHookResult(): Boolean

    override fun onHook() {
        // Load Native hook
        System.loadLibrary("hyperpods_hook")

        "com.android.bluetooth.a2dp.A2dpService".toClass().apply {
            method {
                name = "handleConnectionStateChanged"
                paramCount = 3
            }.hook {
                after {
                    val currState = this.args[2] as Int
                    val fromState = this.args[1] as Int
                    val device = this.args[0] as BluetoothDevice?
                    val handler = XposedHelpers.getObjectField(this.instance, "mHandler") as Handler
                    if (device == null || currState == fromState) {
                        return@after
                    }
                    handler.post {
                        Log.d(
                            "Art_Chen",
                            "A2DP Connection State: $currState, isAirPod ${isPods(device)}"
                        )
                        val context = this.instance as ContextWrapper
                        if (!isPods(device)) return@post

                        val statusBarManager =
                            context.getSystemService("statusbar") as StatusBarManager
                        if (currState == BluetoothHeadset.STATE_CONNECTED) {
                            // Show Wireless Pods icon
                            statusBarManager.setIconVisibility("wireless_headset", true)

                            val hookRes = nativeGetHookResult()
                            if (!hookRes) {
                                Toast.makeText(
                                    appContext,
                                    "HyperPods: hook failed, this version of HyperPods will not work!",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@post
                            }
                            showPodConnectingByMiuiBt(context, device)
                            L2CAPController.connectPod(context, device, prefs)
                        } else if (currState == BluetoothHeadset.STATE_DISCONNECTING || currState == BluetoothHeadset.STATE_DISCONNECTED) {
                            statusBarManager.setIconVisibility("wireless_headset", false)
                            L2CAPController.disconnectedPod(context, device)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun isPods(device: BluetoothDevice): Boolean {
        for (uuid in device.uuids) {
            if (airPodsUUIDs.contains(uuid)) {
                return true
            }
        }
        return false
    }

}