package net.veldor.flibustaloader.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import net.veldor.flibustaloader.utils.FilesHandler.getChangeText
import net.veldor.flibustaloader.utils.Grammar.appVersion

class ChangelogDialog {
    class Builder(private val mContext: Context) {
        fun build(): AlertDialog {
            val changeText = getChangeText()
            // покажу диалог с выбором способа доната
            val builder = AlertDialog.Builder(mContext)
            builder
                .setPositiveButton(mContext.getString(android.R.string.ok), null)
                .setTitle("Список изменений, версия $appVersion")
                .setMessage(changeText)
            return builder.create()
        }
    }
}