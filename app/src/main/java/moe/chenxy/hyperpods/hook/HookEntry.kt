package moe.chenxy.hyperpods.hook

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.YukiHookAPI.configs
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import moe.chenxy.hyperpods.BuildConfig

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onHook()  = YukiHookAPI.encase {
        loadApp("com.android.bluetooth", HeadsetStateDispatcher)
        loadApp("com.xiaomi.bluetooth", MiBluetoothToastHook)
    }

    override fun onInit() = configs {
        isDebug = BuildConfig.DEBUG
    }
}