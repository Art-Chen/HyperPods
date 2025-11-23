package moe.chenxy.hyperpods.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.utils.data.HyperPodsAction

object DeviceCardHook : YukiBaseHooker() {
    @Volatile
    var mac = ""

    override fun onHook() {
        val handlerThread = HandlerThread("broadcast_handler_thread").also {
            it.start()
        }
        val handler = Handler(handlerThread.looper)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == HyperPodsAction.ACTION_PODS_MAC_RECEIVED){
                    mac = p1.getStringExtra("mac")!!
                    Log.i("Art_Chen", "received mac $mac")
                }
            }
        }
        var isRegistered = false


        var panelController: Any? = null
        "miui.systemui.controlcenter.panel.main.MainPanelController".toClass().method {
            name = "onCreate"
        }.hook {
            after {
                panelController = this.instance
            }
        }

        fun hidePanel() {
            panelController?.let {
                XposedHelpers.callMethod(panelController, "exitOrHide")
            }
        }

        "miui.systemui.devicecenter.devices.DeviceInfoWrapper".toClass().apply {
            method {
                name = "performClicked"
                param(ContextClass)
            }.hook {
                before {
                    val context = this.args[0] as Context
                    val deviceInfo = XposedHelpers.callMethod(this.instance, "getDeviceInfo")
                    val id = XposedHelpers.callMethod(deviceInfo, "getId") as String
                    val deviceType = XposedHelpers.callMethod(deviceInfo, "getDeviceType") as String
                    if (deviceType != "third_headset") {
                        return@before
                    }

                    if (!isRegistered) {
                        context.registerReceiver(
                            receiver,
                            IntentFilter(HyperPodsAction.ACTION_PODS_MAC_RECEIVED),
                            null,
                            handler,
                            Context.RECEIVER_EXPORTED,
                        )
                        isRegistered = true
                    }

                    Intent(HyperPodsAction.ACTION_GET_PODS_MAC).apply {
                        this.`package` = "com.android.bluetooth"
                        this.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        context.sendBroadcast(this)
                    }

                    var waitCnt = 10
                    while (mac.isEmpty() && waitCnt-- > 0) {
                        Thread.sleep(50)
                    }

                    if (mac == id) {
                        Intent("chen.action.hyperpods.show_airpods_ui").apply {
                            this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(this)
                        }
                        mac = ""
                        hidePanel()
                        this.result = null
                    }

                    Log.i("Art_Chen", "performClicked $id mac $mac")
//                    this.result = null
                }
            }
        }
    }
}