/*
 * Copyright (C) 2023 someone5678
 * SPDX-License-Identifier: GPL-3.0-or-later
 * License-Filename: LICENSE
 */

package moe.chenxy.hyperpods;

public class Constants {
    /* Authority (package name) */
    public static final String AUTHORITY_BTHELPER = "moe.chenxy.hyperpods";

    /* Slices Paths */
    public static final String PATH_BTHELPER = "bthelper";
    public static final String SLICE_BTHELPER = "/" + PATH_BTHELPER;

    /* Slices Type */
    public static final int SLICE_TOGGLE = 101;
    public static final int SLICE_MAIN = 102;

    /* Slices Intent Action */
    public static final String ACTION_PENDING_INTENT =
            "moe.chenxy.hyperpods.ACTION_PENDING_INTENT";

    /* Slices Intent Extra */
    public static final int EXTRA_NONE = 0;
    public static final int EXTRA_ONEPOD_CHANGED = 10001;
    public static final int EXTRA_AUTO_PLAY_CHANGED = 10002;
    public static final int EXTRA_AUTO_PAUSE_CHANGED = 10003;
    public static final int EXTRA_LOW_LATENCY_AUDIO_CHANGED = 10004;

    /* Shared Preferences */
    public static final String PREFERENCES_BTHELPER = AUTHORITY_BTHELPER + "_preferences";

    /* Shared Preferences Keys */
    public static final String KEY_ONEPOD_MODE = "onepod_mode_pref";
    public static final String KEY_AUTO_PLAY = "auto_play_pref";
    public static final String KEY_AUTO_PAUSE = "auto_pause_pref";

    public static final class Icons {
        public static final int AirPods = R.drawable.AirPods;
        public static final int AirPods_Left = R.drawable.AirPods_Left;
        public static final int AirPods_Right = R.drawable.AirPods_Right;
        public static final int AirPods_Case = R.drawable.AirPods_Case;
        public static final int AirPods_Gen3 = R.drawable.AirPods_Gen3;
        public static final int AirPods_Gen3_Left = R.drawable.AirPods_Gen3_Left;
        public static final int AirPods_Gen3_Right = R.drawable.AirPods_Gen3_Right;
        public static final int AirPods_Gen3_Case = R.drawable.AirPods_Gen3_Case;
        public static final int AirPods_Pro = R.drawable.AirPods_Pro;
        public static final int AirPods_Pro_Left = R.drawable.AirPods_Pro_Left;
        public static final int AirPods_Pro_Right = R.drawable.AirPods_Pro_Right;
        public static final int AirPods_Pro_Case = R.drawable.AirPods_Pro_Case;
        public static final int AirPods_Max_Green = R.drawable.AirPods_Max_Green;
        public static final int AirPods_Max_Pink = R.drawable.AirPods_Max_Pink;
        public static final int AirPods_Max_Silver = R.drawable.AirPods_Max_Silver;
        public static final int AirPods_Max_SkyBlue = R.drawable.AirPods_Max_SkyBlue;
        public static final int AirPods_Max_SpaceGray = R.drawable.AirPods_Max_SpaceGray;

        public static final int[] defaultIcons =
                new int[] {
                    AirPods,
                    AirPods_Left,
                    AirPods_Right,
                    AirPods_Case,
                    AirPods_Gen3,
                    AirPods_Gen3_Left,
                    AirPods_Gen3_Right,
                    AirPods_Gen3_Case,
                    AirPods_Pro,
                    AirPods_Pro_Left,
                    AirPods_Pro_Right,
                    AirPods_Pro_Case,
                    AirPods_Max_Green,
                    AirPods_Max_Pink,
                    AirPods_Max_Silver,
                    AirPods_Max_SkyBlue,
                    AirPods_Max_SpaceGray
                };
    }
}
