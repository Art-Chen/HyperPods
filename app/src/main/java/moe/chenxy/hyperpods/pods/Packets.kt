package moe.chenxy.hyperpods.pods;

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize

enum class Enums(val value: ByteArray) {
    NOISE_CANCELLATION(Capabilities.NOISE_CANCELLATION),
    CONVERSATION_AWARENESS(Capabilities.CONVERSATION_AWARENESS),
    CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY(Capabilities.CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY),
    PREFIX(byteArrayOf(0x04, 0x00, 0x04, 0x00)),
    SETTINGS(byteArrayOf(0x09, 0x00)),
    SUFFIX(byteArrayOf(0x00, 0x00, 0x00)),
    NOTIFICATION_FILTER(byteArrayOf(0x0f)),
    HANDSHAKE(byteArrayOf(0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
    SPECIFIC_FEATURES(byteArrayOf(0x4d)),
    SET_SPECIFIC_FEATURES(PREFIX.value + SPECIFIC_FEATURES.value + byteArrayOf(0x00,
        0xD7.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
    REQUEST_NOTIFICATIONS(PREFIX.value + NOTIFICATION_FILTER.value + byteArrayOf(0x00, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte())),
    NOISE_CANCELLATION_PREFIX(PREFIX.value + SETTINGS.value + NOISE_CANCELLATION.value),
    NOISE_CANCELLATION_OFF(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.OFF.value + SUFFIX.value),
    NOISE_CANCELLATION_ON(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.ON.value + SUFFIX.value),
    NOISE_CANCELLATION_TRANSPARENCY(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.TRANSPARENCY.value + SUFFIX.value),
    NOISE_CANCELLATION_ADAPTIVE(NOISE_CANCELLATION_PREFIX.value + Capabilities.NoiseCancellation.ADAPTIVE.value + SUFFIX.value),
    SET_CONVERSATION_AWARENESS_OFF(PREFIX.value + SETTINGS.value + CONVERSATION_AWARENESS.value + Capabilities.ConversationAwareness.OFF.value + SUFFIX.value),
    SET_CONVERSATION_AWARENESS_ON(PREFIX.value + SETTINGS.value + CONVERSATION_AWARENESS.value + Capabilities.ConversationAwareness.ON.value + SUFFIX.value),
    CONVERSATION_AWARENESS_RECEIVE_PREFIX(PREFIX.value + byteArrayOf(0x4b, 0x00, 0x02, 0x00));
}

object BatteryComponent {
    const val LEFT = 4
    const val RIGHT = 2
    const val CASE = 8
}

object BatteryStatus {
    const val CHARGING = 1
    const val NOT_CHARGING = 2
    const val NEED_AGAIN = 3
    const val DISCONNECTED = 4
}

@Parcelize
data class Battery(val component: Int, val level: Int, val status: Int) : Parcelable {
    fun getComponentName(): String? {
        return when (component) {
            BatteryComponent.LEFT -> "LEFT"
            BatteryComponent.RIGHT -> "RIGHT"
            BatteryComponent.CASE -> "CASE"
            else -> null
        }
    }

    fun getStatusName(): String? {
        return when (status) {
            BatteryStatus.CHARGING -> "CHARGING"
            BatteryStatus.NOT_CHARGING -> "NOT_CHARGING"
            BatteryStatus.DISCONNECTED -> "DISCONNECTED"
            else -> null
        }
    }
}

object LongPressEnableMode {
    const val ANC_SWITCH = 0x1
    const val VOICE_ASSIST = 0x2
    const val OFF = 0x4
    const val NOISE_CANCELLATION = 0x8
    const val TRANSPARENCY = 0x16
    const val ADAPTIVE = 0x32
}

enum class NoiseControlMode {
    OFF,  NOISE_CANCELLATION, TRANSPARENCY, ADAPTIVE
}

object EarDetectionStatus {
    const val IN_EAR = 0x0.toByte()
    const val OUT_OF_EAR = 0x1.toByte()
    const val IN_CASE = 0x2.toByte()
}

class AirPodsNotifications {
    object EarDetection {
        private val notificationBit = Capabilities.EAR_DETECTION
        private val notificationPrefix = Enums.PREFIX.value + notificationBit

        var status: List<Byte> = listOf(0x01, 0x01)

        fun setStatus(data: ByteArray) {
            status = listOf(data[6], data[7])
        }

        fun isEarDetectionData(data: ByteArray): Boolean {
            if (data.size != 8) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }
    }

    object ANC {
        private val notificationPrefix = Enums.NOISE_CANCELLATION_PREFIX.value

        var status: Int = 1
            private set

        fun isANCData(data: ByteArray): Boolean {
            if (data.size != 11) {
                return false
            }
            val prefixHex = notificationPrefix.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setStatus(data: ByteArray) {
            when (data.size) {
                // if the whole packet is given
                11 -> {
                    status = data[7].toInt()
                }
                // if only the data is given
                1 -> {
                    status = data[0].toInt()
                }
                // if the value of control command is given
                4 -> {
                    status = data[0].toInt()
                }
                else -> {
                    Log.d("ANC", "Invalid ANC data size: ${data.size}")
                }
            }
        }

        val name: String =
            when (status) {
                1 -> "OFF"
                2 -> "ON"
                3 -> "TRANSPARENCY"
                4 -> "ADAPTIVE"
                else -> "UNKNOWN"
            }

    }

    object BatteryNotification {
        private var first: Battery = Battery(BatteryComponent.LEFT, 0, BatteryStatus.DISCONNECTED)
        private var second: Battery = Battery(BatteryComponent.RIGHT, 0, BatteryStatus.DISCONNECTED)
        private var case: Battery = Battery(BatteryComponent.CASE, 0, BatteryStatus.DISCONNECTED)

        fun isBatteryData(data: ByteArray): Boolean {
            if (data.size != 22) {
                return false
            }

            return data.joinToString("") { "%02x".format(it) }.startsWith("040004000400")
        }

        fun setBattery(data: ByteArray) {
            first = if (data[10].toInt() == BatteryStatus.DISCONNECTED || data[10].toInt() == BatteryStatus.NEED_AGAIN) {
                Battery(first.component, first.level, data[10].toInt())
            } else {
                Battery(data[7].toInt(), data[9].toInt(), data[10].toInt())
            }
            second = if (data[15].toInt() == BatteryStatus.DISCONNECTED || data[15].toInt() == BatteryStatus.NEED_AGAIN) {
                Battery(second.component, second.level, data[15].toInt())
            } else {
                Battery(data[12].toInt(), data[14].toInt(), data[15].toInt())
            }
            case = if ((data[20].toInt() == BatteryStatus.DISCONNECTED || data[20].toInt() == BatteryStatus.NEED_AGAIN)
                        && case.status != BatteryStatus.DISCONNECTED && case.status != BatteryStatus.NEED_AGAIN) {
                Battery(case.component, case.level, data[20].toInt())
            } else {
                Battery(data[17].toInt(), data[19].toInt(), data[20].toInt())
            }
        }

        fun getBattery(): List<Battery> {
            val left = if (first.component == BatteryComponent.LEFT) first else second
            val right = if (first.component == BatteryComponent.LEFT) second else first
            return listOf(left, right, case)
        }
    }

    object ConversationalAwarenessNotification {
        private val NOTIFICATION_PREFIX = Enums.CONVERSATION_AWARENESS_RECEIVE_PREFIX.value

        var status: Byte = 0
            private set

        fun isConversationalAwarenessData(data: ByteArray): Boolean {
            if (data.size != 10) {
                return false
            }
            val prefixHex = NOTIFICATION_PREFIX.joinToString("") { "%02x".format(it) }
            val dataHex = data.joinToString("") { "%02x".format(it) }
            return dataHex.startsWith(prefixHex)
        }

        fun setData(data: ByteArray) {
            status = data[9]
        }
    }
}

class Capabilities {
    companion object {
        val NOISE_CANCELLATION = byteArrayOf(0x0d)
        val CONVERSATION_AWARENESS = byteArrayOf(0x28)
        val CUSTOMIZABLE_ADAPTIVE_TRANSPARENCY = byteArrayOf(0x01, 0x02)
        val EAR_DETECTION = byteArrayOf(0x06)
    }

    enum class NoiseCancellation(val value: ByteArray) {
        OFF(byteArrayOf(0x01)),
        ON(byteArrayOf(0x02)),
        TRANSPARENCY(byteArrayOf(0x03)),
        ADAPTIVE(byteArrayOf(0x04));
    }

    enum class ConversationAwareness(val value: ByteArray) {
        OFF(byteArrayOf(0x02)),
        ON(byteArrayOf(0x01));
    }
}

enum class LongPressMode {
    OFF, TRANSPARENCY, ADAPTIVE, ANC
}

data class LongPressPacket(val modes: Set<LongPressMode>) {
    val value: ByteArray
        get() {
            val baseArray = byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x1A)
            val modeByte = calculateModeByte()
            return baseArray + byteArrayOf(modeByte, 0x00, 0x00, 0x00)
        }

    private fun calculateModeByte(): Byte {
        var modeByte: Byte = 0x00
        modes.forEach { mode ->
            modeByte = when (mode) {
                LongPressMode.OFF -> (modeByte + 0x01).toByte()
                LongPressMode.TRANSPARENCY -> (modeByte + 0x02).toByte()
                LongPressMode.ADAPTIVE -> (modeByte + 0x04).toByte()
                LongPressMode.ANC -> (modeByte + 0x08).toByte()
            }
        }
        return modeByte
    }
}

fun determinePacket(changedIndex: Int, newEnabled: Boolean, oldModes: Set<LongPressMode>, newModes: Set<LongPressMode>): ByteArray? {
    return if (newEnabled) {
        LongPressPacket(oldModes + newModes.elementAt(changedIndex)).value
    } else {
        LongPressPacket(oldModes - newModes.elementAt(changedIndex)).value
    }
}