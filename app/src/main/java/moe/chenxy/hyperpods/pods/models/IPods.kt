/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */
package moe.chenxy.hyperpods.pods.models

interface IPods {
    val model: String?
    val isSingle: Boolean
    val isDisconnected: Boolean
    val lowBattThreshold: Int
    val manufacturer: String?
}