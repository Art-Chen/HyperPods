package moe.chenxy.hyperpods.pods

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaRoute2Info
import android.media.MediaRouter2
import android.media.MediaRouter2.ScanToken
import android.media.RouteDiscoveryPreference
import android.os.ParcelUuid
import android.util.Log
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import de.robv.android.xposed.XposedHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.AirPodsInstance
import moe.chenxy.hyperpods.utils.MediaControl
import moe.chenxy.hyperpods.utils.SystemApisUtils
import moe.chenxy.hyperpods.utils.SystemApisUtils.setIconVisibility
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil
import moe.chenxy.hyperpods.utils.miuiStrongToast.MiuiStrongToastUtil.cancelPodsNotificationByMiuiBt
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import moe.chenxy.hyperpods.utils.data.HyperPodsAction
import moe.chenxy.hyperpods.utils.data.HyperPodsPrefsKey
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import java.util.concurrent.Executor
import kotlin.collections.get
import kotlin.experimental.or
import kotlin.reflect.full.memberProperties

@SuppressLint("MissingPermission", "StaticFieldLeak")
object L2CAPController {
    private const val TAG = "HyperPods-L2CAPController"

    // Basic Object
    lateinit var socket: BluetoothSocket
    private var mContext: Context? = null
    lateinit var mDevice: BluetoothDevice
    private val audioManager: AudioManager? by lazy {
        mContext?.getSystemService(AudioManager::class.java)
    }
    private lateinit var mPrefsBridge: YukiHookPrefsBridge

    private var aacpManager: AACPManager? = null

//    private lateinit var mAirPodsInstance: AirPodsInstance

    private var scanToken: ScanToken? = null
    var routes: List<MediaRoute2Info> = listOf()

    private lateinit var mediaRouter: MediaRouter2

    // Status
    private var mShowedConnectedToast = false
    private var lastCaseConnected = false
    private var disconnectedAudio = true /* default to true to connect audio first time always */
    private var pausedAudio = false
    private var lastTempBatt = 0
    lateinit var currentEarDetectionParams: EarDetectionParams
    lateinit var currentBatteryParams: BatteryParams
    private var currentAnc: Int = 1
    lateinit var currentPodsInfo: AACPManager.Companion.AirPodsInformation

    // Function toggle
    private var earDetection = true
    private var disconnectAudio = true

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1?.action == HyperPodsAction.ACTION_GET_PODS_MAC) {
                Intent(HyperPodsAction.ACTION_PODS_MAC_RECEIVED).apply {
                    Log.i(TAG, "${p1.action} ,mac ${mDevice.address}")
                    this.`package` = "com.android.systemui"
                    this.putExtra("mac", mDevice.address)
                    this.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    p0?.sendBroadcast(this)
                    return
                }
            }
            handleUIEvent(p1!!)
        }
    }

    private fun changeUIAncStatus(status: Int) {
        if (status !in 1..4) {
            // ignore invalid param
            return
        }
        Intent(HyperPodsAction.ACTION_PODS_ANC_CHANGED).apply {
            this.putExtra("status", status)
            this.`package` = BuildConfig.APPLICATION_ID
            this.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            mContext?.sendBroadcast(this)
        }
    }

    private fun changeUIInEarStatus(status: EarDetectionParams) {
        Intent(HyperPodsAction.ACTION_EAR_DETECTION_STATUS_CHANGED).apply {
            this.putExtra("status", status)
            this.`package` = BuildConfig.APPLICATION_ID
            this.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            mContext?.sendBroadcast(this)
        }
    }

    private fun changeUIBatteryStatus(status: BatteryParams) {
        Intent(HyperPodsAction.ACTION_PODS_BATTERY_CHANGED).apply {
            this.putExtra("status", status)
            this.`package` = BuildConfig.APPLICATION_ID
            this.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            mContext?.sendBroadcast(this)
        }
    }

    fun handleUIEvent(intent: Intent) {
        when (intent.action) {
            HyperPodsAction.ACTION_PODS_UI_INIT -> {
                Log.i(TAG, "UI Init")

                if (::currentEarDetectionParams.isInitialized)
                    changeUIInEarStatus(currentEarDetectionParams)

                if (::currentBatteryParams.isInitialized)
                    changeUIBatteryStatus(currentBatteryParams)

                changeUIAncStatus(currentAnc)
                Intent(HyperPodsAction.ACTION_PODS_CONNECTED).apply {
                    if (this@L2CAPController::currentPodsInfo.isInitialized)
                        this.putExtra("device_info", currentPodsInfo)
                    this.putExtra("device_name", mDevice.name)
                    mContext?.sendBroadcast(this)
                }
            }
            HyperPodsAction.ACTION_ANC_SELECT -> {
                val status = intent.getIntExtra("status", 0)
                setANCMode(status)
            }

            HyperPodsAction.ACTION_EAR_DETECTION_SWITCH_CHANGED -> {
                earDetection = intent.getBooleanExtra("ear_detection", true)
                disconnectAudio = intent.getBooleanExtra("disconnect_audio", true)
            }
            HyperPodsAction.ACTION_PODS_SETTINGS_CHANGED -> {
                intent.getStringExtra("key")?.let { handleUISettingsChanged(it) }
            }
        }
    }

    private fun handleInEarStatusChanged(status: List<Byte>) {
        if (::currentEarDetectionParams.isInitialized) {
            if (currentEarDetectionParams.left == status[0] && currentEarDetectionParams.right == status[1]) {
                Log.d(TAG, "receive same in ear status, ignored")
                return
            }
        }
        currentEarDetectionParams = EarDetectionParams(status[0], status[1])
        changeUIInEarStatus(currentEarDetectionParams)

        if (!earDetection) return

        val leftInEar = status[0] == EarDetectionStatus.IN_EAR
        val rightInEar = status[1] == EarDetectionStatus.IN_EAR
        val inEar = if (status.find { it == EarDetectionStatus.IN_CASE || it == 0x3.toByte() } != null) {
            // one is in case
            leftInEar || rightInEar
        } else {
            leftInEar && rightInEar
        }

        Log.d(TAG, "handleInEarStatusChanged left $leftInEar right $rightInEar res $inEar")

        // Check if need disconnect Audio and switch to speaker
        if (disconnectAudio) {
            if (!leftInEar && !rightInEar && !disconnectedAudio) {
                disconnectedAudio = true
                disconnectAudio(mContext!!, mDevice)
            } else if ((leftInEar || rightInEar) && disconnectedAudio) {
                connectAudio(mContext!!, mDevice)
                disconnectedAudio = false
            }
        } else if (disconnectedAudio){
            connectAudio(mContext!!, mDevice)
            disconnectedAudio = false
        }

        val audioIsPlaying = audioManager?.isMusicActive == true

        if (inEar) {
            if (pausedAudio && !audioIsPlaying) {
                MediaControl.sendPlay()
                pausedAudio = false
            }
        } else if (!disconnectedAudio && audioIsPlaying && !pausedAudio){
            MediaControl.sendPause()
            pausedAudio = true
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun handleBatteryChanged(packet: ByteArray) {
        val batteries = AirPodsNotifications.BatteryNotification.getBattery()
        val left = PodBatteryParams(
            batteries[0].level,
            batteries[0].status == BatteryStatus.CHARGING,
            batteries[0].status != BatteryStatus.DISCONNECTED,
            batteries[0].status
        )
        val right = PodBatteryParams(
            batteries[1].level,
            batteries[1].status == BatteryStatus.CHARGING,
            batteries[1].status != BatteryStatus.DISCONNECTED,
            batteries[1].status
        )
        val case = PodBatteryParams(
            batteries[2].level,
            batteries[2].status == BatteryStatus.CHARGING,
            batteries[2].status != BatteryStatus.DISCONNECTED,
            batteries[2].status
        )
        if (BuildConfig.DEBUG) {
            Log.v(
                TAG,
                "batt left ${left.battery} right ${right.battery} case ${case.battery} packet ${
                    packet.toHexString(
                        HexFormat.UpperCase
                    )
                }"
            )
        }

        val shouldShowToast = !mShowedConnectedToast || (lastCaseConnected != case.isConnected && !lastCaseConnected)
        if (shouldShowToast && (left.battery <= 0 || right.battery <= 0 || (case.isConnected && case.battery <= 0))) {
            // only show connected toast when battery info all correct
            return
        }

        val batteryParams = BatteryParams(left, right, case)
        currentBatteryParams = batteryParams

        // allow show toast again when case status from disconnected to active, it means pods put in the case again
        if (shouldShowToast) {
            MiuiStrongToastUtil.showPodsBatteryToastByMiuiBt(mContext, batteryParams)
            mShowedConnectedToast = true
        }
        lastCaseConnected = case.isConnected
        MiuiStrongToastUtil.showPodsNotificationByMiuiBt(mContext, batteryParams, mDevice)
        changeUIBatteryStatus(batteryParams)

        lastTempBatt = if (left.isConnected && right.isConnected)
                minOf(left.battery, right.battery)
            else if (left.isConnected)
                left.battery
            else if (right.isConnected)
                right.battery
            else SystemApisUtils.BATTERY_LEVEL_UNKNOWN

        setRegularBatteryLevel(lastTempBatt)
    }

    fun initAllCustomSettings() {
        val values = HyperPodsPrefsKey::class.memberProperties
            .filter { it.returnType.classifier == String::class }
            .map { it.getter.call(HyperPodsPrefsKey) as String }
        for (key in values) {
            handleUISettingsChanged(key)
        }
    }

    fun getIdentifierValue(identifier: AACPManager.Companion.ControlCommandIdentifiers): Byte? {
        return aacpManager?.controlCommandStatusList?.find {
            it.identifier == identifier
        }?.value?.takeIf { it.isNotEmpty() }?.get(0)
    }

    fun handleUISettingsChanged(key: String) {
        when (key) {
            HyperPodsPrefsKey.PERSONLIZED_VOLUME -> {
                val checked = mPrefsBridge.getBoolean(key, true)
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG.value, value = checked)
            }
//            HyperPodsPrefsKey.CASE_CHARGING_SOUND -> {
////                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.CH.value, value = checked)
//                setCaseChargingSounds(mPrefsBridge.getBoolean(key, true))
//            }
            HyperPodsPrefsKey.ADAPTIVE_AUDIO_LEVEL -> {
                val value = mPrefsBridge.getFloat(key, 0.5f)
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.AUTO_ANC_STRENGTH.value, value = (100 - value * 100).toInt())
            }
            HyperPodsPrefsKey.CONVERSATION_AWARENESS -> {
                val checked = mPrefsBridge.getBoolean(key, true)
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG.value, value = checked)
            }
            HyperPodsPrefsKey.LOUD_SOUND_REDUCTION -> {
                val checked = mPrefsBridge.getBoolean(key, true)
                // TODO: porting ATTManager from LibrePod
                setLoudSoundReduction(checked)
            }
            HyperPodsPrefsKey.ADJUST_VOLUME_BY_SWIPER -> {
                val checked = mPrefsBridge.getBoolean(key, true)
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_MODE.value, value = checked)
            }
            HyperPodsPrefsKey.LISTENING_MODE_BYTE -> {
                val value = mPrefsBridge.getInt(key, AACPManager.Companion.ListeningMode.NC.value.toInt() or AACPManager.Companion.ListeningMode.TRANSPARENCY.value.toInt())
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE_CONFIGS.value, value = value.toByte())
            }
            HyperPodsPrefsKey.LONG_PRESS_MODE_LEFT -> {
                // TODO: Implement launch Xiaoai & enable custom hold action
            }
            HyperPodsPrefsKey.LONG_PRESS_MODE_RIGHT -> {
                // TODO: Implement launch Xiaoai & enable custom hold action
            }

            HyperPodsPrefsKey.EAR_DETECTION -> {
                earDetection = mPrefsBridge.getBoolean(key, true)
            }

            HyperPodsPrefsKey.EAR_DETECTION_SWITCH_SPEAKER -> {
                disconnectAudio = mPrefsBridge.getBoolean(key, true)
            }

            HyperPodsPrefsKey.SINGLE_POD_ANC -> {
                val checked = mPrefsBridge.getBoolean(key, true)
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.ONE_BUD_ANC_MODE.value, value = checked)
            }

            HyperPodsPrefsKey.MICROPHONE_MODE -> {
                val value = mPrefsBridge.getInt(key, 0)
                val byteValue = when (value) {
                    0 -> 0x00
                    2 -> 0x01
                    1 -> 0x02
                    else -> 0x00
                }
                aacpManager?.sendControlCommand(identifier = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE.value, value = byteValue)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun handleAirPodsPacket(packet: ByteArray) {
        if (AirPodsNotifications.EarDetection.isEarDetectionData(packet)) {
            AirPodsNotifications.EarDetection.setStatus(packet)
            handleInEarStatusChanged(AirPodsNotifications.EarDetection.status)
        } else if (AirPodsNotifications.ANC.isANCData(packet)) {
            AirPodsNotifications.ANC.setStatus(packet)
            currentAnc = AirPodsNotifications.ANC.status
            changeUIAncStatus(currentAnc)
        } else if (AirPodsNotifications.BatteryNotification.isBatteryData(packet)) {
            AirPodsNotifications.BatteryNotification.setBattery(packet)
            handleBatteryChanged(packet)
        } else if (AirPodsNotifications.ConversationalAwarenessNotification.isConversationalAwarenessData(packet)) {
            AirPodsNotifications.ConversationalAwarenessNotification.setData(packet)
        } else {
            if (BuildConfig.DEBUG) {
                Log.v(
                    TAG,
                    "Unknown AirPods Packet Received: ${packet.toHexString(HexFormat.UpperCase)}"
                )
            }
        }

    }

    private fun updateFeatureToggle() {
        earDetection = mPrefsBridge.getBoolean(HyperPodsPrefsKey.EAR_DETECTION, true)
        disconnectAudio = mPrefsBridge.getBoolean(HyperPodsPrefsKey.EAR_DETECTION_SWITCH_SPEAKER, true)
    }

    private val routeCallback = object : MediaRouter2.RouteCallback() {
        override fun onRoutesUpdated(routes: List<MediaRoute2Info>) {
            Log.v(TAG, "routes updated: $routes")
            this@L2CAPController.routes = routes
        }
    }
    private fun startRoutesScan() {
        val executor = Executor { p0 ->
            CoroutineScope(Dispatchers.IO).launch {
                p0?.run()
            }
        }

        val preferredFeature = listOf(MediaRoute2Info.FEATURE_LIVE_AUDIO, MediaRoute2Info.FEATURE_LIVE_VIDEO)
        mediaRouter.registerRouteCallback(executor, routeCallback, RouteDiscoveryPreference.Builder(preferredFeature, true).build())
        scanToken = mediaRouter.requestScan(MediaRouter2.ScanRequest.Builder().build())
    }

    private fun stopRoutesScan() {
        scanToken?.let { mediaRouter.cancelScanRequest(it) }
        mediaRouter.unregisterRouteCallback(routeCallback)
        scanToken = null
    }

    private val packetCallback = object : AACPManager.PacketCallback {
        override fun onBatteryInfoReceived(batteryInfo: ByteArray) {
            AirPodsNotifications.BatteryNotification.setBattery(batteryInfo)
            handleBatteryChanged(batteryInfo)
        }

        override fun onEarDetectionReceived(earDetection: ByteArray) {
            AirPodsNotifications.EarDetection.setStatus(earDetection)
            handleInEarStatusChanged(AirPodsNotifications.EarDetection.status)
        }

        override fun onConversationAwarenessReceived(conversationAwareness: ByteArray) {
            AirPodsNotifications.ConversationalAwarenessNotification.setData(conversationAwareness)
            Log.i(TAG, "Conversation Awareness: ${AirPodsNotifications.ConversationalAwarenessNotification.status}")
        }

        override fun onControlCommandReceived(controlCommand: ByteArray) {
            val command = AACPManager.ControlCommand.fromByteArray(controlCommand)
            if (command.identifier == AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value) {
                AirPodsNotifications.ANC.setStatus(byteArrayOf(command.value.takeIf { it.isNotEmpty() }?.get(0) ?: 0x00.toByte()))
                currentAnc = AirPodsNotifications.ANC.status
                changeUIAncStatus(currentAnc)
            }
        }

        override fun onDeviceInformationReceived(deviceInformation: AACPManager.Companion.AirPodsInformation) {
            Log.i(TAG, "Device Information: $deviceInformation")
            currentPodsInfo = deviceInformation
            Intent(HyperPodsAction.ACTION_PODS_CONNECTED).apply {
                this.putExtra("device_info", deviceInformation)
                this.putExtra("device_name", mDevice.name)
                mContext?.sendBroadcast(this)
            }
        }

        override fun onHeadTrackingReceived(headTracking: ByteArray) {
//            TODO("Not yet implemented")
        }

        override fun onUnknownPacketReceived(packet: ByteArray) {

        }

        override fun onProximityKeysReceived(proximityKeys: ByteArray) {
//            TODO("Not yet implemented")
        }

        override fun onStemPressReceived(stemPress: ByteArray) {

        }

        override fun onAudioSourceReceived(audioSource: ByteArray) {
//            TODO("Not yet implemented")
        }

        override fun onOwnershipChangeReceived(owns: Boolean) {
//            TODO("Not yet implemented")
        }

        override fun onConnectedDevicesReceived(connectedDevices: List<AACPManager.Companion.ConnectedDevice>) {
//            TODO("Not yet implemented")
        }

        override fun onOwnershipToFalseRequest(
            sender: String,
            reasonReverseTapped: Boolean
        ) {
//            TODO("Not yet implemented")
        }

        override fun onShowNearbyUI(sender: String) {

        }
    }

    fun connectPod(context: Context, device: BluetoothDevice, prefsBridge: YukiHookPrefsBridge) {
        mContext = context
        mDevice = device
        mPrefsBridge = prefsBridge

        updateFeatureToggle()

        context.registerReceiver(broadcastReceiver, IntentFilter().apply {
            this.addAction(HyperPodsAction.ACTION_ANC_SELECT)
            this.addAction(HyperPodsAction.ACTION_PODS_UI_INIT)
            this.addAction(HyperPodsAction.ACTION_EAR_DETECTION_SWITCH_CHANGED)
            this.addAction(HyperPodsAction.ACTION_GET_PODS_MAC)
            this.addAction(HyperPodsAction.ACTION_PODS_SETTINGS_CHANGED)
        }, Context.RECEIVER_EXPORTED)

        MediaControl.mContext = mContext
        mediaRouter = MediaRouter2.getInstance(mContext!!)
        startRoutesScan()

        val uuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        fun getBtSocket(): BluetoothSocket {
            try {
                return BluetoothSocket::class.java.getDeclaredConstructor(IntType, BooleanType, BooleanType,
                    BluetoothDevice::class.java, IntType,
                    ParcelUuid::class.java).newInstance(3, true, true, device, 0x1001, uuid) as BluetoothSocket
            } catch (_: NoSuchMethodException) {
                Log.i(TAG, "get bt socket method failed, try android Baklava method")
                return BluetoothSocket::class.java.getDeclaredConstructor(BluetoothDevice::class.java, IntType, BooleanType, BooleanType,
                    IntType, ParcelUuid::class.java).newInstance(device, 3, true, true, 0x1001, uuid) as BluetoothSocket
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            socket = getBtSocket()

            Log.d(TAG, "connecting AirPods!")
            try {
                socket.connect()
            } catch (e: Exception) {
                Log.e(TAG, "failed to connect to socket, retry!", e)
                connectPod(context, mDevice, mPrefsBridge)

                return@launch
            }


            Log.d(TAG, "connected!")
            aacpManager = AACPManager(socket)
            aacpManager!!.setPacketCallback(packetCallback)
            aacpManager!!.sendDataPacket(aacpManager!!.createHandshakePacket())
            aacpManager!!.sendPacket(aacpManager!!.createHandshakePacket())
            delay(200)
            aacpManager!!.sendSetFeatureFlagsPacket()
            delay(200)
            aacpManager!!.sendNotificationRequest()
            delay(200)
            while (socket.isConnected) {
                val buffer = ByteArray(1024)
                val bytesRead = socket.inputStream.read(buffer)
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "bytesRead $bytesRead!")
                }
                if (bytesRead > 0) {
                    aacpManager!!.receivePacket(buffer.copyOfRange(0, bytesRead))
//                    handleAirPodsPacket(buffer.copyOfRange(0, bytesRead))
                } else if (bytesRead == -1) {
                    // disconnected
                    socket.close()
                }
            }
        }
    }

    fun disconnectedPod(context: Context, device: BluetoothDevice) {
        if (::socket.isInitialized) {
            socket.close()
        }

        mContext?.let {
            stopRoutesScan()
            cancelPodsNotificationByMiuiBt(context, device)
            Intent(HyperPodsAction.ACTION_PODS_DISCONNECTED).apply {
                context.sendBroadcast(this)
            }
            it.unregisterReceiver(broadcastReceiver)
        }

        mShowedConnectedToast = false
        pausedAudio = false
//        disconnectedAudio = false
        mContext = null
        MediaControl.mContext = null
        aacpManager = null
    }

    fun sendPacket(packet: String) {
        val fromHex = packet.split(" ").map { it.toInt(16).toByte() }
        socket.outputStream?.write(fromHex.toByteArray())
        socket.outputStream?.flush()
    }

    fun sendPacket(packet: ByteArray) {
        if (this::socket.isInitialized && socket.isConnected && socket.outputStream != null) {
            socket.outputStream?.write(packet)
            socket.outputStream?.flush()
        }
    }

    fun setANCMode(mode: Int) {
        Log.d(TAG, "setANCMode: $mode")

        if (mode in 1..4) {
            aacpManager?.sendControlCommand(
                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE.value,
                mode
            )
        }
    }

    fun disconnectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        MediaControl.sendPause()

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val method = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) { }
        }, BluetoothProfile.HEADSET)

        CoroutineScope(Dispatchers.Default).launch {
            // Wait pause done
            delay(500)
            for (route in routes) {
                // try switch to speaker
                if (route.type == MediaRoute2Info.TYPE_BUILTIN_SPEAKER) {
                    Log.d(TAG, "found speaker route $route")
                    mediaRouter.transferTo(route)
                }
            }
        }

        setRegularBatteryLevel(lastTempBatt)
    }

    fun connectAudio(context: Context, device: BluetoothDevice?) {
        val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter

        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    try {
                        val method = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        method.invoke(proxy, device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) { }
        }, BluetoothProfile.HEADSET)


        for (route in routes) {
            // try switch back
            if (route.type == MediaRoute2Info.TYPE_BLUETOOTH_A2DP && route.name == device!!.name) {
                Log.d(TAG, "found bt route $route")
                mediaRouter.transferTo(route)
            }
        }

        // Restore icon
        val statusBarManager =
            context.getSystemService("statusbar") as StatusBarManager
        statusBarManager.setIconVisibility("wireless_headset", true)
        setRegularBatteryLevel(lastTempBatt)
    }

    fun setLoudSoundReduction(enabled: Boolean) {
        val hex = "52 1B 00 0${if (enabled) "1" else "0"}"
        val bytes = hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()
        sendPacket(bytes)
    }

    fun setRegularBatteryLevel(level: Int) {
        val service = XposedHelpers.getObjectField(mContext, "mAdapterService")
        XposedHelpers.callMethod(service, "setBatteryLevel", mDevice, level, false)
    }
}
