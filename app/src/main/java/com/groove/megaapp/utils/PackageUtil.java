package com.groove.megaapp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

final public class PackageUtil {

    public static boolean isInstalled(Context context, String pkg) {
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignore) {
        }

        return false;
    }

    public static boolean isIntentNotResolvable(Context context, Intent intent) {
        List<ResolveInfo> infos = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return infos.isEmpty();
    }
}
