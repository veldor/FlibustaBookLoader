package net.veldor.flibustaloader.dialogs

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class DonationDialog {
    class Builder(private val mContext: Context) {
        fun build(): AlertDialog {
            val donateOptions = arrayOf("PayPal", "Yandex")
            // покажу диалог с выбором способа доната
            val builder = AlertDialog.Builder(mContext)
            builder
                .setItems(
                    donateOptions
                ) { _: DialogInterface?, which: Int ->
                    if (which == 0) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data =
                            Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YUGUWUF99QYG4&source=url")
                        mContext.startActivity(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://money.yandex.ru/to/41001269882689")
                        mContext.startActivity(intent)
                    }
                }
                .setTitle("Поддержка разработки")
            return builder.create()
        }
    }
}