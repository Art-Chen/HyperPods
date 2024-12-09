package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.Service
import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.pods.PodsScanner
import moe.chenxy.hyperpods.utils.SystemApisUtils.setIconVisibility


object HeadsetStateDispatcher : YukiBaseHooker() {

    private val airPodsUUIDs = hashSetOf(
        ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
        ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
    )

    @SuppressLint("StaticFieldLeak")
    override fun onHook() {
        var podsScanner : PodsScanner? = null
        val moduleResources = this.moduleAppResources
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
                        if (currState == BluetoothHeadset.STATE_CONNECTING) {
                            podsScanner?.stopScan()
                            podsScanner = PodsScanner(context, moduleResources)
                            podsScanner!!.startScan(device)
                        } else if (currState == BluetoothHeadset.STATE_CONNECTED) {
                            if (podsScanner == null) {
                                podsScanner = PodsScanner(context, moduleResources)
                                podsScanner!!.startScan(device)
                            }
                            // Show Wireless Pods icon
                            val statusBarManager =
                                context.getSystemService("statusbar") as StatusBarManager
                            statusBarManager.setIconVisibility("wireless_headset", true)

                        } else if (currState == BluetoothHeadset.STATE_DISCONNECTING || currState == BluetoothHeadset.STATE_DISCONNECTED) {
                            podsScanner?.stopScan()
                            podsScanner = null
                            val statusBarManager =
                                context.getSystemService("statusbar") as StatusBarManager
                            statusBarManager.setIconVisibility("wireless_headset", false)
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