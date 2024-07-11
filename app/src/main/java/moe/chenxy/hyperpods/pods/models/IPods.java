/*
 * Copyright (C) 2019-2022 Federico Dossena
 *               2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods.pods.models;

public interface IPods {

    String getModel();

    boolean isSingle();

    boolean isDisconnected();

    int getLowBattThreshold();

    String getMenufacturer();
}
