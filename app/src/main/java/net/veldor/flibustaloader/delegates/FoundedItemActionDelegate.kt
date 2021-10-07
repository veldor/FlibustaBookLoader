package net.veldor.flibustaloader.delegates

import android.view.View
import net.veldor.flibustaloader.selections.FoundedEntity

interface FoundedItemActionDelegate {
    fun buttonPressed(item: FoundedEntity)
    fun imageClicked(item: FoundedEntity)
    fun itemPressed(item: FoundedEntity)
    fun buttonLongPressed(item: FoundedEntity)
    fun itemLongPressed(item: FoundedEntity)
    fun menuItemPressed(item: FoundedEntity, button: View)
    fun loadMoreBtnClicked()
    fun authorClicked(item: FoundedEntity)
    fun sequenceClicked(item: FoundedEntity)
}