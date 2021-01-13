package net.veldor.flibustaloader.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import net.veldor.flibustaloader.R;

import pl.droidsonroids.gif.GifImageView;

public class GifDialog {

    public static class Builder {
        private String title, message, positiveBtnText;
        private final Activity activity;
        private GifDialogListener pListener;
        private boolean cancel;
        int gifImageResource;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }


        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setPositiveBtnText(String positiveBtnText) {
            this.positiveBtnText = positiveBtnText;
            return this;
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
        public Builder OnPositiveClicked(GifDialogListener pListener) {
            this.pListener = pListener;
            return this;
        }
/*
        //set Negative listener
        public Builder OnNegativeClicked(GifDialogListener nListener) {
            this.nListener = nListener;
            return this;
        }*/

        public Builder isCancellable(boolean cancel) {
            this.cancel = cancel;
            return this;
        }

        public Builder setGifResource(int gifImageResource) {
            this.gifImageResource = gifImageResource;
            return this;
        }

        public Dialog build() {
            TextView message1, title1;
            Button nBtn, pBtn;
            GifImageView gifImageView;
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = dialog.getWindow();
            if (window != null)
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCancelable(cancel);
            dialog.setContentView(R.layout.gifdialog);


            //getting resources
            title1 = dialog.findViewById(R.id.title);
            message1 = dialog.findViewById(R.id.message);
            nBtn = dialog.findViewById(R.id.negativeBtn);
            pBtn = dialog.findViewById(R.id.positiveBtn);
            gifImageView = dialog.findViewById(R.id.gifImageView);
            gifImageView.setImageResource(gifImageResource);

            title1.setText(title);
            message1.setText(message);
            if (positiveBtnText != null) {
                pBtn.setText(positiveBtnText);

                pBtn.setOnClickListener(view -> {
                    if (pListener != null) pListener.OnClick();
                    dialog.dismiss();
                });
            } else {
                pBtn.setVisibility(View.GONE);
            }
                nBtn.setVisibility(View.GONE);

            return dialog;

        }
    }
}
