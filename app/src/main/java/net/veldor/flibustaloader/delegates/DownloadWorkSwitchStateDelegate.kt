package net.veldor.flibustaloader.delegates

import android.view.View
import net.veldor.flibustaloader.selections.FoundedEntity

interface DownloadWorkSwitchStateDelegate {
    fun stateSwitched(state: Int)
}