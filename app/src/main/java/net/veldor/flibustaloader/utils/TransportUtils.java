package net.veldor.flibustaloader.utils;

import android.content.Intent;
import android.content.pm.PackageManager;

import net.veldor.flibustaloader.App;

public class TransportUtils {
    public static boolean intentCanBeHandled(Intent intent){
        PackageManager packageManager = App.getInstance().getPackageManager();
        return intent.resolveActivity(packageManager) != null;
    }
}
