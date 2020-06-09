package net.veldor.flibustaloader.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

public class DonationDialog {

    public static class Builder {
        private final Context mContext;

        public Builder(Context context) {
            mContext = context;
        }

        public AlertDialog build() {
            String[] donateOptions = new String[]{"PayPal", "Yandex"};
            // покажу диалог с выбором способа доната
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(mContext);
            builder
                    .setItems(
                            donateOptions,
                            (dialog, which) -> {
                                if (which == 0) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YUGUWUF99QYG4&source=url"));
                                    mContext.startActivity(intent);
                                } else {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("https://money.yandex.ru/to/41001269882689"));
                                    mContext.startActivity(intent);
                                }
                            }
                    )
                    .setTitle("Поддержка разработки");
            return builder.create();
        }
    }

}
