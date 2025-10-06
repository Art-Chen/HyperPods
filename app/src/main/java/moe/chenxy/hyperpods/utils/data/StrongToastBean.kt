package moe.chenxy.hyperpods.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class StrongToastBean(
    var left: Left? = null,
    var right: Right? = null
)
