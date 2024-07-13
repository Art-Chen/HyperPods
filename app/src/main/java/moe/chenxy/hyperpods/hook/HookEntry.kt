package moe.chenxy.hyperpods.hook

import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.YukiHookAPI.configs
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onHook()  = YukiHookAPI.encase {
        this.
        loadApp("com.android.bluetooth", HeadsetStateDispatcher)
        loadApp("com.xiaomi.bluetooth", MiBluetoothToastHook)
    }

    override fun onInit() = configs {
        isDebug = true
    }
}