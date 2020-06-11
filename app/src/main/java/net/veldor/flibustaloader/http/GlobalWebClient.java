package net.veldor.flibustaloader.http;

import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

public class GlobalWebClient {
    public static String request(String requestString) throws TorNotLoadedException, IOException {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if (App.getInstance().isExternalVpn()) {
            HttpResponse response = ExternalVpnVewClient.rawRequest(requestString);
            if (response != null) {
                return EntityUtils.toString(response.getEntity());
            }
        } else {
            TorWebClient webClient = new TorWebClient();
            return webClient.request(requestString);
        }
        return null;
    }

    public static void handleBookLoadRequest(HttpResponse response, DocumentFile newFile) throws BookNotFoundException {
        try {
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Log.d("surprise", "TorWebClient downloadBook 177: content length is " + entity.getContentLength());
                        InputStream content = entity.getContent();
                        if (content != null && entity.getContentLength() > 0) {
                            OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                            if (out != null) {
                                int read;
                                byte[] buffer = new byte[1024];
                                while ((read = content.read(buffer)) > 0) {
                                    out.write(buffer, 0, read);
                                }
                                out.close();
                                Log.d("surprise", "TorWebClient downloadBook 188: created file length is " + newFile.length());
                                if (newFile.length() > 0) {
                                    Log.d("surprise", "TorWebClient downloadBook 190: file founded and saved");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.getMessage());
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        newFile.delete();
        Log.d("surprise", "TorWebClient downloadBook: книга не найдена");
        throw new BookNotFoundException();
    }

    public static void handleBookLoadRequest(HttpResponse response, File newFile) throws BookNotFoundException {
        try {
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Log.d("surprise", "TorWebClient downloadBook 177: content length is " + entity.getContentLength());
                        InputStream content = entity.getContent();
                        if (content != null && entity.getContentLength() > 0) {
                            OutputStream out = new FileOutputStream(newFile);
                            int read;
                            byte[] buffer = new byte[1024];
                            while ((read = content.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                            }
                            out.close();
                            Log.d("surprise", "TorWebClient downloadBook 188: created file length is " + newFile.length());
                            if (newFile.length() > 0) {
                                Log.d("surprise", "TorWebClient downloadBook 190: file founded and saved");
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.getMessage());
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        boolean deleteResult = newFile.delete();
        Log.d("surprise", "TorWebClient downloadBook: книга не найдена, статус удаления файла: " + deleteResult);
        throw new BookNotFoundException();
    }

}