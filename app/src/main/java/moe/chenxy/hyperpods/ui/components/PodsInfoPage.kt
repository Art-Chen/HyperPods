package moe.chenxy.hyperpods.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import moe.chenxy.hyperpods.R
import moe.chenxy.hyperpods.utils.AACPManager
import moe.chenxy.hyperpods.utils.AirPodsModels.getModelByModelNumber
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InfoItem(title: String, summary: String, onClick: (() -> Unit)? = null) {
    BasicComponent(
        title = title,
        titleColor = BasicComponentColors(
            color = MiuixTheme.colorScheme.onSurface,
            disabledColor = MiuixTheme.colorScheme.onSurface
        ),
        rightActions = {
            Text(summary, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = MiuixTheme.textStyles.body2.fontSize)
        },
        onClick = onClick,
    )
}

@Composable
fun PodsInfoPage(titleModifier: Modifier,
                 cardModifier: Modifier,
                 info: AACPManager.Companion.AirPodsInformation
) {
    val podModel = getModelByModelNumber(info.modelNumber)
    var expandSerialNumber by remember { mutableStateOf(false) }
    SmallTitle(stringResource(R.string.about), modifier = titleModifier)
    Card(
        modifier = cardModifier
    ) {
        val infoColor = BasicComponentColors(
            color = MiuixTheme.colorScheme.onSurface,
            disabledColor = MiuixTheme.colorScheme.onSurface
        )

        InfoItem(title = stringResource(R.string.product_manufacturer),
            summary = info.manufacturer)

        podModel?.let {
            InfoItem(title = stringResource(R.string.product_name),
                summary = podModel.name)
        }

        InfoItem(title = stringResource(R.string.model_name),
            summary = info.modelNumber)

        InfoItem(title = stringResource(R.string.serial_num),
            summary = if (expandSerialNumber)
                "${info.serialNumber}\nL: ${info.leftSerialNumber}\nR: ${info.rightSerialNumber}"
            else
                info.serialNumber,
            onClick = {
                expandSerialNumber = !expandSerialNumber
            })

        InfoItem(title = stringResource(R.string.version_num),
            summary = info.version3)
        InfoItem(title = stringResource(R.string.hw_version_num),
            summary = info.hardwareRevision)
    }
}
