package moe.chenxy.hyperpods.hook

import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import de.robv.android.xposed.XposedHelpers


object SystemUIPluginHook : YukiBaseHooker() {
    override fun onHook() {
        var pluginLoaderClassLoader: ClassLoader? = null

        fun loadPluginHooker(hooker: YukiBaseHooker) {
            hooker.appClassLoader = pluginLoaderClassLoader
            loadHooker(hooker)
        }

        fun initPluginHook() {
            loadPluginHooker(DeviceCardHook)
        }

        // Load plugin hooker
        // get Classloader for plugin on Android U
        "com.android.systemui.shared.plugins.PluginInstance".toClass().method {
            name = "loadPlugin"
        }.hook {
            after {
                val pkgName = runCatching {
                    XposedHelpers.callMethod(this.instance, "getPackageName") as String
                }.getOrElse {
                    // HyperOS 3 and older SystemUI plugin framework.
                    XposedHelpers.callMethod(this.instance, "getPackage") as String
                }
                if (pkgName == "miui.systemui.plugin") {
                    val clsLoader = runCatching {
                        // HyperOS 4 stores the loaded plugin context in PluginData.
                        val pluginData = XposedHelpers.getObjectField(this.instance, "pluginData")
                        val pluginContext = XposedHelpers.getObjectField(pluginData, "context")
                        XposedHelpers.callMethod(pluginContext, "getClassLoader") as ClassLoader
                    }.getOrElse {
                        // HyperOS 3 and older SystemUI plugin framework.
                        val factory = XposedHelpers.getObjectField(this.instance, "mPluginFactory")
                        XposedHelpers.callMethod(
                            XposedHelpers.getObjectField(factory, "mClassLoaderFactory"),
                            "get"
                        ) as ClassLoader
                    }
                    if (pluginLoaderClassLoader != clsLoader) {
                        Log.i(
                            "Art_Chen",
                            "[loadPlugin] initPluginHook"
                        )
                        pluginLoaderClassLoader = clsLoader
                        initPluginHook()
                    }
                }
            }
        }
    }
}
