package moe.chenxy.hyperpods.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.pods.EarDetectionStatus
import moe.chenxy.hyperpods.utils.AirPodsBase
import moe.chenxy.hyperpods.utils.AirPodsPro3
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.EarDetectionParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PodBattery(level: Int, icon: Painter, isCharging: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(5.dp)
    ) {
        CircularProgressIndicator(
            progress = if (level == 0) null else level / 100f,
            colors = ProgressIndicatorDefaults.ProgressIndicatorColors(
                foregroundColor = if (isCharging)
                        Color(0xFF34C759)
                    else if (level < 30)
                        Color(0xFFFF3B30)
                    else MiuixTheme.colorScheme.primary
                ,
                disabledForegroundColor = MiuixTheme.colorScheme.disabledPrimarySlider,
                backgroundColor = MiuixTheme.colorScheme.tertiaryContainerVariant),
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
        )
        Row(
            modifier = Modifier.height(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = "Battery Type Icon", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 5.dp).height(12.dp).offset(y = 1.dp))
            Text("${if (level > 0) level else "-" } %", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Pod(batteryParams: BatteryParams, darkMode: Boolean, modifier: Modifier = Modifier, podimg: Painter) {
    Column(modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = modifier
            .heightIn(100.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Image(
                painter = podimg,
                contentDescription = "Pods",
                modifier = Modifier.weight(1f).padding(end = 10.dp),
                alignment = Alignment.Center
            )
        }

        Row {
            AnimatedVisibility(batteryParams.left != null && batteryParams.left?.isConnected == true) {
                PodBattery(
                    level = batteryParams.left!!.battery,
                    icon = painterResource(R.drawable.circle_l),
                    isCharging = batteryParams.left!!.isCharging
                )
            }

            AnimatedVisibility(batteryParams.right != null && batteryParams.right?.isConnected == true) {
                PodBattery(
                    level = batteryParams.right!!.battery,
                    icon = painterResource(R.drawable.circle_r),
                    isCharging = batteryParams.right!!.isCharging
                )
            }
        }
    }
}

@Composable
fun Case(podBatteryParams: PodBatteryParams?, darkMode: Boolean, modifier: Modifier = Modifier, caseimg: Painter) {
    Column(modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = modifier
            .heightIn(100.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Image(
                painter = caseimg,
                contentDescription = "Case",
                modifier = Modifier.padding(20.dp)
            )
        }
        podBatteryParams?.let {
            PodBattery(level = it.battery, icon = painterResource(R.drawable.airpods_pro_case_notification), isCharging = it.isCharging)
        }
    }
}

@Composable
fun PodStatus(batteryParams: BatteryParams, earDetectionParams: EarDetectionParams, modifier: Modifier = Modifier, model: AirPodsBase = AirPodsPro3()) {
    val currentDarkMode = isSystemInDarkTheme()

    val anyPodsInCase = earDetectionParams.left == EarDetectionStatus.IN_CASE
            || earDetectionParams.right == EarDetectionStatus.IN_CASE
    val shouldShowCase = batteryParams.case?.isConnected == true
    val targetWeight = if (shouldShowCase) 1f else 4f
    val targetCaseWeight = if (shouldShowCase) 1f else 0.01f
    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = spring(stiffness = if (shouldShowCase) Spring.StiffnessMedium else Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "PodWeightAnimation"
    )
    val animatedCaseWeight by animateFloatAsState(
        targetValue = targetCaseWeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "CaseWeightAnimation"
    )
    val targetAlpha = if (shouldShowCase) 1f else 0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(stiffness = if (shouldShowCase) Spring.StiffnessVeryLow else Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "AlphaAnimation"
    )

    Row(modifier = modifier
        .fillMaxWidth()
        .height(220.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround) {
        Pod(batteryParams, currentDarkMode, modifier = Modifier.weight(animatedWeight), podimg = painterResource(model.budsRes))
        Case(
            batteryParams.case,
            currentDarkMode,
            modifier = Modifier.weight(animatedCaseWeight).alpha(animatedAlpha),
            caseimg = painterResource(model.caseRes)
        )
    }
}

@Composable
@Preview
fun PodStatusPreview() {
    PodStatus(BatteryParams(
        left = PodBatteryParams(50, isCharging = true, isConnected = true),
        right = PodBatteryParams(40, isCharging = false, isConnected = true),
        case = PodBatteryParams(0, isCharging = false, isConnected = true)
    ), EarDetectionParams(), modifier = Modifier.padding(12.dp), model = AirPodsPro3())
}