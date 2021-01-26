package com.groove.megaapp.utils;

import android.content.res.Resources;

final public class SizeUtil {

    public static int toDp(Resources resources, int px) {
        return Math.round(px / resources.getDisplayMetrics().density);
    }

    public static int toPx(Resources resources, int dp) {
        return Math.round(dp * resources.getDisplayMetrics().density);
    }
}
