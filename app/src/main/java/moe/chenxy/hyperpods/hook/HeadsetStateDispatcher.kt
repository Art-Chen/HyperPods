package moe.chenxy.hyperpods.hook

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Intent
import android.hardware.BatteryState
import android.os.ParcelUuid
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import de.robv.android.xposed.XposedHelpers
import moe.chenxy.hyperpods.pods.PodsScanner
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt

object HeadsetStateDispatcher : YukiBaseHooker() {

    private val airPodsUUIDs = hashSetOf(
        ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
        ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
    )

    @SuppressLint("StaticFieldLeak")
    override fun onHook() {
        var podsScanner : PodsScanner? = null
        val moduleResources = this.moduleAppResources
        "com.android.bluetooth.hfp.HeadsetA2dpSync".toClass().apply {
            var mHeadsetService : Service
            method {
                name = "updateA2DPConnectionState"
                param(IntentClass)
            }.hook {
                after {
                    val intent = this.args[0] as Intent
                    val currState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                    val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                    device?.let {
                        Log.d("Art_Chen", "A2DP Connection State: $currState, isAirPod ${isPods(it)}")
                        if (!isPods(it)) return@after
                        if (currState == BluetoothHeadset.STATE_CONNECTING) {
                            mHeadsetService = XposedHelpers.getObjectField(
                                this.instance,
                                "mHeadsetService"
                            ) as Service
                            podsScanner?.stopScan()
                            podsScanner = PodsScanner(mHeadsetService, moduleResources)
                            podsScanner!!.startScan(it)
                        } else if (currState == BluetoothHeadset.STATE_CONNECTED) {
                            if (podsScanner == null) {
                                mHeadsetService = XposedHelpers.getObjectField(
                                    this.instance,
                                    "mHeadsetService"
                                ) as Service
                                podsScanner = PodsScanner(mHeadsetService, moduleResources)
                                podsScanner!!.startScan(it)
                            }
                        } else if (currState == BluetoothHeadset.STATE_DISCONNECTING || currState == BluetoothHeadset.STATE_DISCONNECTED) {
                            podsScanner?.stopScan()
                            podsScanner = null
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