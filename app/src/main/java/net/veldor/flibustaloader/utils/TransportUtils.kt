package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import android.content.Intent

object TransportUtils {
    @kotlin.jvm.JvmStatic
    fun intentCanBeHandled(intent: Intent): Boolean {
        val packageManager = App.instance.packageManager
        return intent.resolveActivity(packageManager) != null
    }
}