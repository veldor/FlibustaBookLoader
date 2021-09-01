package net.veldor.flibustaloader.http;

import android.util.Log;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.MutableLiveData;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException;
import net.veldor.flibustaloader.ecxeptions.ZeroBookSizeException;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

public class GlobalWebClient {

    public static final MutableLiveData<Integer> mConnectionState = new MutableLiveData<>();
    public static final Integer CONNECTED = 1;
    public static final Integer DISCONNECTED = 2;

    public static String request(String requestString) throws IOException {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if (App.getInstance().isExternalVpn()) {
            HttpResponse response = ExternalVpnVewClient.rawRequest(requestString);
            if (response != null) {
                return EntityUtils.toString(response.getEntity());
            }
        } else {
            String response = (new MirrorRequestClient()).request(requestString);
            if (response == null) {
                // сначала попробую запросить инфу с зеркала
                TorWebClient webClient;
                try {
                    webClient = new TorWebClient();
                } catch (ConnectionLostException e) {
                    return null;
                }
                return webClient.request(requestString);
            }
            return response;

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
                        boolean showDownloadProgress = MyPreferences.getInstance().isShowDownloadProgress();
                        long contentLength = entity.getContentLength();
                        long startTime = System.currentTimeMillis();
                        long lastNotificationTime = System.currentTimeMillis();
                        String fileName = newFile.getName();
                        Notificator notifier = App.getInstance().getNotificator();
                        if (contentLength > 0) {
                            // создам уведомление, в котором буду показывать прогресс скачивания
                            if (showDownloadProgress) {
                                notifier.createBookLoadingProgressNotification((int) contentLength, 0, fileName, startTime);
                            }
                        }
                        InputStream content = entity.getContent();
                        if (content != null && entity.getContentLength() > 0) {
                            OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                            if (out != null) {
                                int read;
                                byte[] buffer = new byte[4092];
                                while ((read = content.read(buffer)) > 0) {
                                    out.write(buffer, 0, read);
                                    if (contentLength > 0 && showDownloadProgress && lastNotificationTime + 1000 < System.currentTimeMillis()) {
                                        lastNotificationTime = System.currentTimeMillis();
                                        notifier.createBookLoadingProgressNotification((int) contentLength, (int) newFile.length(), fileName, startTime);
                                    }
                                }
                                out.close();
                                content.close();
                                notifier.cancelBookLoadingProgressNotification();
                                if (newFile.length() > 0) {
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
        throw new BookNotFoundException();
    }

    public static void handleBookLoadRequest(HttpResponse response, File newFile) throws BookNotFoundException {
        try {
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        long contentLength = entity.getContentLength();
                        long startTime = System.currentTimeMillis();
                        String fileName = newFile.getName();
                        Notificator notifier = App.getInstance().getNotificator();
                        if (contentLength > 0) {
                            // создам уведомление, в котором буду показывать прогресс скачивания
                            notifier.createBookLoadingProgressNotification((int) contentLength, 0, fileName, startTime);
                        }
                        InputStream content = entity.getContent();
                        if (content != null && entity.getContentLength() > 0) {
                            OutputStream out = new FileOutputStream(newFile);
                            int read;
                            byte[] buffer = new byte[1024];
                            while ((read = content.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                                notifier.createBookLoadingProgressNotification((int) contentLength, (int) newFile.length(), fileName, startTime);
                            }
                            out.close();
                            content.close();
                            notifier.cancelBookLoadingProgressNotification();
                            if (newFile.length() > 0) {
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("surprise", "GlobalWebClient handleBookLoadRequest 103: have error " + e.getMessage());
            e.printStackTrace();
        }
        // если что-то пошло не так- удалю файл и покажу ошибку скачивания
        boolean deleteResult = newFile.delete();
        Log.d("surprise", "TorWebClient downloadBook: книга не найдена, статус удаления файла: " + deleteResult);
        throw new BookNotFoundException();
    }

    public static boolean handleBookLoadRequestNoContentLength(HttpResponse response, DocumentFile newFile) {
        try {
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream content = entity.getContent();
                        if (content != null) {
                            OutputStream out = App.getInstance().getContentResolver().openOutputStream(newFile.getUri());
                            if (out != null) {
                                int read;
                                byte[] buffer = new byte[1024];
                                while ((read = content.read(buffer)) > 0) {
                                    out.write(buffer, 0, read);
                                }

                                out.close();
                                content.close();
                                if (newFile.length() > 0) {
                                    Log.d("surprise", "TorWebClient downloadBook 190: file founded and saved to " + newFile.getUri());
                                    return true;
                                } else {
                                    throw new ZeroBookSizeException();
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
        return false;
    }

    public static boolean handleBookLoadRequestNoContentLength(HttpResponse response, File newFile) {
        try {
            if (response != null) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream content = entity.getContent();
                        if (content != null) {
                            OutputStream out = new FileOutputStream(newFile);
                            int read;
                            byte[] buffer = new byte[1024];
                            while ((read = content.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                            }
                            out.close();
                            Log.d("surprise", "TorWebClient downloadBook 188: created file length is " + newFile.length());
                            if (newFile.length() > 0) {
                                Log.d("surprise", "TorWebClient downloadBook 190: file founded and saved to " + newFile.getAbsolutePath());
                                return true;
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
        return false;
    }

    public static String requestNoMirror(String requestString) throws IOException {
        // если используется внешний VPN- выполню поиск в нём, иначае- в TOR
        if (App.getInstance().isExternalVpn()) {
            HttpResponse response = ExternalVpnVewClient.rawRequest(requestString);
            if (response != null) {
                return EntityUtils.toString(response.getEntity());
            }
        } else {
            // сначала попробую запросить инфу с зеркала
            TorWebClient webClient;
            try {
                webClient = new TorWebClient();
            } catch (ConnectionLostException e) {
                return null;
            }
            return webClient.requestNoMirror(requestString);
        }
        return null;
    }
}
