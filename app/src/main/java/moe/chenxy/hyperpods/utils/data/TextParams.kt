package moe.chenxy.hyperpods.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class TextParams(
    var text: String? = null,
    var textColor: Int = 0,
    var turnAnim: Boolean = false
)
