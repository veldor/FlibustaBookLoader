package net.veldor.flibustaloader.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import net.veldor.flibustaloader.R
import pl.droidsonroids.gif.GifImageView

class GifDialog {
    class Builder(private val activity: Activity) {
        private var title: String? = null
        private var message: String? = null
        private var positiveBtnText: String? = null
        private var pListener: GifDialogListener? = null
        private var cancel = false
        private var gifImageResource = 0
        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        fun setPositiveBtnText(positiveBtnText: String?): Builder {
            this.positiveBtnText = positiveBtnText
            return this
        }

        /*
        public Builder setPositiveBtnBackground(String pBtnColor) {
            this.pBtnColor = pBtnColor;
            return this;
        }*/
        /*
        public Builder setNegativeBtnText(String negativeBtnText) {
            this.negativeBtnText = negativeBtnText;
            return this;
        }*/
        /*
        public Builder setNegativeBtnBackground(String nBtnColor) {
            this.nBtnColor = nBtnColor;
            return this;
        }*/
        //set Positive listener
        fun onPositiveClicked(pListener: GifDialogListener?): Builder {
            this.pListener = pListener
            return this
        }

        /*
        //set Negative listener
        public Builder OnNegativeClicked(GifDialogListener nListener) {
            this.nListener = nListener;
            return this;
        }*/
        fun isCancellable(cancel: Boolean): Builder {
            this.cancel = cancel
            return this
        }

        fun setGifResource(gifImageResource: Int): Builder {
            this.gifImageResource = gifImageResource
            return this
        }

        fun build(): Dialog {
            val message1: TextView
            val title1: TextView
            val nBtn: Button
            val pBtn: Button
            val gifImageView: GifImageView
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val window = dialog.window
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(cancel)
            dialog.setContentView(R.layout.gifdialog)


            //getting resources
            title1 = dialog.findViewById(R.id.title)
            message1 = dialog.findViewById(R.id.message)
            nBtn = dialog.findViewById(R.id.negativeBtn)
            pBtn = dialog.findViewById(R.id.positiveBtn)
            gifImageView = dialog.findViewById(R.id.gifImageView)
            gifImageView.setImageResource(gifImageResource)
            title1.text = title
            message1.text = message
            if (positiveBtnText != null) {
                pBtn.text = positiveBtnText
                pBtn.setOnClickListener {
                    if (pListener != null) pListener!!.onClick()
                    dialog.dismiss()
                }
            } else {
                pBtn.visibility = View.GONE
            }
            nBtn.visibility = View.GONE
            return dialog
        }
    }
}