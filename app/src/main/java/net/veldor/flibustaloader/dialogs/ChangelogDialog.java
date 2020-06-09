package net.veldor.flibustaloader.dialogs;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.Grammar;

public class ChangelogDialog {

    public static class Builder {
        private final Context mContext;

        public Builder(Context context) {
            mContext = context;
        }

        public AlertDialog build() {
            String changeText = FilesHandler.getChangeText();
            // покажу диалог с выбором способа доната
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder
                    .setPositiveButton(mContext.getString(android.R.string.ok), null)
                    .setTitle("Список изменений, версия " + Grammar.getAppVersion())
                    .setMessage(changeText);
            return builder.create();
        }
    }

}
