package net.veldor.flibustaloader.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;

import net.veldor.flibustaloader.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MyFileReader {
    private static final String SEARCH_AUTOCOMPLETE_FILE = "searchAutocomplete.xml";
    private static final String SEARCH_AUTOCOMPLETE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>";
    private static final String SUBSCRIBE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><subscribe> </subscribe>";
    private static final String BOOKS_SUBSCRIBE_FILE = "booksSubscribe.xml";

    public static String getSearchAutocomplete() {

        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        if (!autocompleteFile.exists()) {
            makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(autocompleteFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getBooksSubscribe() {

        File booksSubscribeFile = new File(App.getInstance().getFilesDir(), BOOKS_SUBSCRIBE_FILE);
        if (!booksSubscribeFile.exists()) {
            makeFile(booksSubscribeFile, SUBSCRIBE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(booksSubscribeFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static void saveSearchAutocomplete(String value) {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, value);
        Log.d("surprise", "MyFileReader saveSearchAutocomplete: save file " + autocompleteFile);
    }

    public static void clearAutocomplete() {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
    }

    private static void makeFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void saveBooksSubscription(String value) {
        File subscriptionFile = new File(App.getInstance().getFilesDir(), BOOKS_SUBSCRIBE_FILE);
        makeFile(subscriptionFile, value);
    }

    public static boolean isInternalMemory(File downloadFolder) {
        return downloadFolder.toString().startsWith("/storage/emulated/0");
    }

    // преобразование контекста в путь

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
