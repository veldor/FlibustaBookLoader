package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;

public class UriConverter {
    /**
     * Ing.N.Nyerges 2019 V2.0
     *
     * Storage Access Framework(SAF) Uri's creator from File (java.IO),
     * for removable external storages
     *
     * @param context Application Context
     * @param file File path + file name
     * @return Uri[]:
     *   uri[0] = SAF TREE Uri
     *   uri[1] = SAF DOCUMENT Uri
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Uri[] getSafUris (Context context, File file) {

        Uri[] uri = new Uri[2];
        String scheme = "content";
        String authority = "com.android.externalstorage.documents";

        // Separate each element of the File path
        // File format: "/storage/XXXX-XXXX/sub-folder1/sub-folder2..../filename"
        // (XXXX-XXXX is external removable number
        String[] ele = file.getPath().split(File.separator);
        //  ele[0] = not used (empty)
        //  ele[1] = not used (storage name)
        //  ele[2] = storage number
        //  ele[3 to (n-1)] = folders
        //  ele[n] = file name

        // Construct folders strings using SAF format
        StringBuilder folders = new StringBuilder();
        if (ele.length > 4) {
            folders.append(ele[3]);
            for (int i = 4; i < ele.length - 1; ++i) folders.append("%2F").append(ele[i]);
        }

        String common = ele[2] + "%3A" + folders.toString();

        // Construct TREE Uri
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(scheme);
        builder.authority(authority);
        builder.encodedPath("/tree/" + common);
        uri[0] = builder.build();

        // Construct DOCUMENT Uri
        builder = new Uri.Builder();
        builder.scheme(scheme);
        builder.authority(authority);
        if (ele.length > 4) common = common + "%2F";
        builder.encodedPath("/document/" + common + file.getName());
        uri[1] = builder.build();

        return uri;
    }
}
